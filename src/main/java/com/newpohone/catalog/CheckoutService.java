package com.newpohone.catalog;

import com.newpohone.database.CatalogRepository;
import com.newpohone.database.CheckoutRepository;
import com.newpohone.orders.OrderManagementService;
import com.newpohone.orders.OrderStatus;
import com.newpohone.web.SessionUsers;
import jakarta.servlet.http.HttpSession;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

    public static final List<Map<String, String>> PAYMENT_METHODS = List.of(
            method("PSE", "PSE", "Transferencia inmediata desde tu banco.", "bi-bank"),
            method("Bancolombia", "Bancolombia", "Paga con tu app o sucursal virtual.", "bi-phone"),
            method("Tarjeta de crédito", "Tarjeta de crédito", "Visa, Mastercard y American Express.",
                    "bi-credit-card-2-front"),
            method("Tarjeta débito", "Tarjeta débito", "Débito Visa o Mastercard.", "bi-credit-card"));

    private static final Set<String> ALLOWED_METHODS = Set.of(
            "PSE", "Bancolombia", "Tarjeta de crédito", "Tarjeta débito");

    private final CartService cartService;
    private final CatalogRepository catalogRepository;
    private final CheckoutRepository checkoutRepository;
    private final OrderManagementService orderManagementService;

    public CheckoutService(CartService cartService,
            CatalogRepository catalogRepository,
            CheckoutRepository checkoutRepository,
            OrderManagementService orderManagementService) {
        this.cartService = cartService;
        this.catalogRepository = catalogRepository;
        this.checkoutRepository = checkoutRepository;
        this.orderManagementService = orderManagementService;
    }

    public CheckoutIntent prepare(HttpSession session, String metodo, String titular, String numero,
            String vencimiento, String cvv, String nombre, String telefono, String direccion) {
        Map<String, Object> cart = cartService.view(session);
        if (Boolean.TRUE.equals(cart.get("empty"))) {
            throw new CheckoutException("Agrega productos al carrito para continuar.");
        }
        if (metodo == null || !ALLOWED_METHODS.contains(metodo)) {
            throw new CheckoutException("Selecciona un método de pago.");
        }
        if (isCard(metodo)) {
            validateCard(titular, numero, vencimiento, cvv);
        }
        validateShipping(nombre, telefono, direccion);
        assertStock(cart);

        CheckoutIntent intent = new CheckoutIntent();
        intent.setMetodo(metodo);
        intent.setNombre(nombre.trim());
        intent.setTelefono(digits(telefono));
        intent.setDireccion(direccion.trim());
        intent.setCart(cart);
        intent.setCreatedAt(System.currentTimeMillis());
        session.setAttribute(CheckoutIntent.SESSION_KEY, intent);
        return intent;
    }

    public CheckoutIntent requireIntent(HttpSession session) {
        Object stored = session.getAttribute(CheckoutIntent.SESSION_KEY);
        if (!(stored instanceof CheckoutIntent intent) || intent.expired()) {
            session.removeAttribute(CheckoutIntent.SESSION_KEY);
            throw new CheckoutException("La sesión de pago expiró. Vuelve a confirmar tu compra.");
        }
        return intent;
    }

    @Transactional
    public Map<String, Object> confirm(HttpSession session) {
        CheckoutIntent intent = requireIntent(session);
        Map<String, Object> cart = intent.getCart();
        assertStock(cart);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");
        double total = ((Number) cart.get("total")).doubleValue();
        int orderId = checkoutRepository.insertOrder(total, SessionUsers.customerCedula(session));
        for (Map<String, Object> item : items) {
            int productId = ((Number) item.get("id")).intValue();
            int quantity = ((Number) item.get("cantidad")).intValue();
            double subtotal = ((Number) item.get("subtotal")).doubleValue();
            checkoutRepository.insertDetail(orderId, productId, quantity, subtotal);
            if (!catalogRepository.decrementStock(productId, quantity)) {
                throw new CheckoutException("No fue posible reservar el inventario. Intenta de nuevo.");
            }
        }
        checkoutRepository.insertPayment(orderId, intent.getMetodo());
        String guia = trackingCode(orderId);
        checkoutRepository.insertShipment(orderId, intent.getDireccion(), intent.getTelefono(),
                intent.getNombre(), guia);

        cartService.clear(session);
        session.removeAttribute(CheckoutIntent.SESSION_KEY);

        Map<String, Object> order = new LinkedHashMap<>(cart);
        order.put("id", orderId);
        order.put("metodo", intent.getMetodo());
        order.put("estado", "Aprobado");
        order.put("estadoPedido", OrderStatus.PENDIENTE.getLabel());
        order.put("estadoEnvio", OrderStatus.PENDIENTE.getLabel());
        order.put("guia", guia);
        order.put("nombre", intent.getNombre());
        order.put("telefono", intent.getTelefono());
        order.put("direccion", intent.getDireccion());
        session.setAttribute("lastTracking", Map.of("guia", guia, "telefono", intent.getTelefono()));
        return order;
    }

    public Map<String, Object> track(String guia, String telefono) {
        if (guia == null || guia.isBlank() || telefono == null || digits(telefono).length() < 7) {
            throw new CheckoutException("Ingresa la guía y el teléfono registrados en la compra.");
        }
        Map<String, Object> tracking = checkoutRepository.findTracking(guia.trim(), digits(telefono));
        if (tracking == null) {
            throw new CheckoutException("No encontramos un pedido con esa guía y teléfono.");
        }
        int orderId = ((Number) tracking.get("pedidoId")).intValue();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> item : checkoutRepository.findOrderItems(orderId)) {
            Map<String, Object> row = new LinkedHashMap<>(item);
            row.put("subtotalLabel", money(((Number) item.get("subtotal")).doubleValue()));
            items.add(row);
        }
        tracking.put("items", items);
        tracking.put("totalLabel", money(((Number) tracking.get("total")).doubleValue()));
        OrderStatus status = OrderStatus.from(String.valueOf(tracking.get("estadoPedido")));
        tracking.put("estadoActual", status.getLabel());
        tracking.put("statusClass", status.getCssClass());
        tracking.put("cancelled", status == OrderStatus.CANCELADO);
        tracking.put("steps", orderManagementService.timeline(status));
        tracking.put("entregaEstimada", estimatedDelivery(String.valueOf(tracking.get("fechaEnvio"))));
        return tracking;
    }

    private void assertStock(Map<String, Object> cart) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");
        for (Map<String, Object> item : items) {
            int productId = ((Number) item.get("id")).intValue();
            int quantity = ((Number) item.get("cantidad")).intValue();
            Map<String, Object> product = catalogRepository.findById(productId)
                    .orElseThrow(() -> new CheckoutException("Un producto del carrito ya no está disponible."));
            int stock = ((Number) product.get("stock")).intValue();
            if (stock < quantity) {
                throw new CheckoutException("No hay stock suficiente para " + product.get("nombre") + ".");
            }
        }
    }

    private void validateShipping(String nombre, String telefono, String direccion) {
        if (nombre == null || nombre.trim().length() < 3) {
            throw new CheckoutException("Ingresa el nombre de quien recibe el pedido.");
        }
        String phone = digits(telefono);
        if (phone.length() < 7 || phone.length() > 15) {
            throw new CheckoutException("Ingresa un teléfono válido para el seguimiento.");
        }
        if (direccion == null || direccion.trim().length() < 10) {
            throw new CheckoutException("Ingresa una dirección de entrega completa.");
        }
    }

    private boolean isCard(String method) {
        return method.startsWith("Tarjeta");
    }

    private void validateCard(String titular, String numero, String vencimiento, String cvv) {
        if (titular == null || titular.trim().length() < 3) {
            throw new CheckoutException("Ingresa el nombre que aparece en la tarjeta.");
        }
        String cardDigits = numero == null ? "" : numero.replaceAll("\\D", "");
        if (cardDigits.length() < 13 || cardDigits.length() > 19) {
            throw new CheckoutException("Ingresa un número de tarjeta válido.");
        }
        if (vencimiento == null || !vencimiento.trim().matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
            throw new CheckoutException("Ingresa la fecha de vencimiento en formato MM/AA.");
        }
        if (cvv == null || !cvv.trim().matches("^\\d{3,4}$")) {
            throw new CheckoutException("Ingresa un CVV válido.");
        }
    }

    private String estimatedDelivery(String shipDate) {
        try {
            LocalDate date = LocalDate.parse(shipDate);
            return date.plusDays(3).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception exception) {
            return "Por confirmar";
        }
    }

    private String trackingCode(int orderId) {
        return "NP-%s-%06d".formatted(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), orderId);
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String money(double value) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
        return "$" + format.format(Math.round(value));
    }

    private static Map<String, String> method(String id, String name, String description, String icon) {
        return Map.of("id", id, "nombre", name, "descripcion", description, "icono", icon);
    }
}
