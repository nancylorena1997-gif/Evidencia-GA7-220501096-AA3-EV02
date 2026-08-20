package com.newpohone.catalog;

import com.newpohone.database.CatalogRepository;
import jakarta.servlet.http.HttpSession;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final CatalogRepository catalogRepository;
    private final ProductPresenter productPresenter;

    public CartService(CatalogRepository catalogRepository, ProductPresenter productPresenter) {
        this.catalogRepository = catalogRepository;
        this.productPresenter = productPresenter;
    }

    public Map<String, Object> view(HttpSession session) {
        return snapshot(peek(session), null, null);
    }

    public Map<String, Object> add(HttpSession session, int productId) {
        Optional<Map<String, Object>> product = loadProduct(productId);
        if (product.isEmpty()) {
            return snapshot(peek(session), "El producto no existe.", true);
        }

        Map<String, Object> data = product.get();
        int stock = stockOf(data);
        if (stock <= 0) {
            return snapshot(peek(session), "Este producto está agotado.", true);
        }

        Cart cart = writable(session);
        CartItem existing = cart.get(productId);
        if (existing == null) {
            cart.put(toItem(data, 1));
            return snapshot(cart, "Producto agregado al carrito.", false);
        }

        if (existing.getCantidad() >= stock) {
            refreshItem(existing, data);
            return snapshot(cart, "Solo hay " + stock + " unidades disponibles.", true);
        }

        existing.setCantidad(existing.getCantidad() + 1);
        refreshItem(existing, data);
        return snapshot(cart, "Cantidad actualizada.", false);
    }

    public Map<String, Object> update(HttpSession session, int productId, int cantidad) {
        Cart cart = writable(session);
        CartItem item = cart.get(productId);
        if (item == null) {
            return snapshot(cart, "Ese producto no está en el carrito.", true);
        }

        if (cantidad <= 0) {
            cart.remove(productId);
            return snapshot(cart, "Producto eliminado del carrito.", false);
        }

        Optional<Map<String, Object>> product = loadProduct(productId);
        if (product.isEmpty()) {
            cart.remove(productId);
            return snapshot(cart, "El producto ya no está disponible.", true);
        }

        Map<String, Object> data = product.get();
        int stock = stockOf(data);
        int nextQuantity = Math.min(cantidad, Math.max(stock, 0));
        String message = "Cantidad actualizada.";
        boolean error = false;

        if (stock <= 0) {
            cart.remove(productId);
            return snapshot(cart, "Este producto se agotó y se quitó del carrito.", true);
        }
        if (cantidad > stock) {
            message = "Solo hay " + stock + " unidades disponibles.";
            error = true;
        }

        item.setCantidad(nextQuantity);
        refreshItem(item, data);
        return snapshot(cart, message, error);
    }

    public Map<String, Object> remove(HttpSession session, int productId) {
        Cart cart = writable(session);
        cart.remove(productId);
        return snapshot(cart, "Producto eliminado del carrito.", false);
    }

    public Map<String, Object> clear(HttpSession session) {
        Cart cart = writable(session);
        cart.clear();
        return snapshot(cart, "El carrito quedó vacío.", false);
    }

    private Optional<Map<String, Object>> loadProduct(int productId) {
        Optional<Map<String, Object>> product = catalogRepository.findById(productId);
        product.ifPresent(productPresenter::enrich);
        return product;
    }

    private Cart peek(HttpSession session) {
        if (session == null) {
            return new Cart();
        }
        Object stored = session.getAttribute(Cart.SESSION_KEY);
        return stored instanceof Cart cart ? cart : new Cart();
    }

    private Cart writable(HttpSession session) {
        Cart cart = peek(session);
        session.setAttribute(Cart.SESSION_KEY, cart);
        return cart;
    }

    private CartItem toItem(Map<String, Object> product, int cantidad) {
        CartItem item = new CartItem();
        item.setId(((Number) product.get("id")).intValue());
        refreshItem(item, product);
        item.setCantidad(cantidad);
        return item;
    }

    private void refreshItem(CartItem item, Map<String, Object> product) {
        item.setNombre(String.valueOf(product.get("nombre")));
        Object category = product.get("categoria");
        item.setCategoria(category == null ? "Sin categoría" : String.valueOf(category));
        item.setImage(String.valueOf(product.get("image")));
        item.setPrecio(((Number) product.get("precio")).doubleValue());
        item.setStock(stockOf(product));
    }

    private int stockOf(Map<String, Object> product) {
        return ((Number) product.get("stock")).intValue();
    }

    private Map<String, Object> snapshot(Cart cart, String message, Boolean error) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("nombre", item.getNombre());
            row.put("categoria", item.getCategoria());
            row.put("image", item.getImage());
            row.put("precio", item.getPrecio());
            row.put("precioLabel", money(item.getPrecio()));
            row.put("cantidad", item.getCantidad());
            row.put("stock", item.getStock());
            row.put("subtotal", item.getSubtotal());
            row.put("subtotalLabel", money(item.getSubtotal()));
            items.add(row);
        }

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("items", items);
        view.put("count", cart.getItemCount());
        view.put("lines", cart.getLineCount());
        view.put("total", cart.getTotal());
        view.put("totalLabel", money(cart.getTotal()));
        view.put("empty", cart.isEmpty());
        if (message != null) {
            view.put("message", message);
        }
        if (error != null) {
            view.put("error", error);
        }
        return view;
    }

    private String money(double value) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
        return "$" + format.format(Math.round(value));
    }
}
