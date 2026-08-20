package com.newpohone.web;

import com.newpohone.catalog.CartService;
import com.newpohone.catalog.ProductPresenter;
import com.newpohone.database.CatalogRepository;
import com.newpohone.database.FavoriteRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final CatalogRepository catalogRepository;
    private final ProductPresenter productPresenter;
    private final CartService cartService;

    public FavoriteController(FavoriteRepository favoriteRepository,
            CatalogRepository catalogRepository,
            ProductPresenter productPresenter,
            CartService cartService) {
        this.favoriteRepository = favoriteRepository;
        this.catalogRepository = catalogRepository;
        this.productPresenter = productPresenter;
        this.cartService = cartService;
    }

    @GetMapping("/favoritos")
    public String list(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Integer customerId = SessionUsers.customerCedula(session);
        if (customerId == null) {
            if (SessionUsers.current(session) == null) {
                return "redirect:/login?next=/favoritos";
            }
            redirectAttributes.addFlashAttribute("error",
                    "Inicia sesión como cliente para ver tu lista de favoritos.");
            return "redirect:/catalog";
        }

        List<Map<String, Object>> products = favoriteRepository.findByCustomer(customerId);
        productPresenter.enrich(products);
        model.addAttribute("products", products);
        model.addAttribute("cartCount", cartService.view(session).get("count"));
        return "catalog/favorites";
    }

    @PostMapping("/favoritos/agregar")
    public String add(@RequestParam int productId,
            @RequestParam(required = false) String next,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer customerId = requireCustomer(session, redirectAttributes);
        if (customerId == null) {
            return guestOrDenied(session, next);
        }
        if (catalogRepository.findById(productId).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ese producto no está en el catálogo.");
            return redirectBack(next);
        }
        if (favoriteRepository.add(customerId, productId)) {
            redirectAttributes.addFlashAttribute("success", "Producto agregado a tus favoritos.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ese producto ya está en tu lista de favoritos.");
        }
        return redirectBack(next);
    }

    @PostMapping("/favoritos/eliminar")
    public String remove(@RequestParam int productId,
            @RequestParam(required = false) String next,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer customerId = requireCustomer(session, redirectAttributes);
        if (customerId == null) {
            return guestOrDenied(session, next);
        }
        if (favoriteRepository.remove(customerId, productId)) {
            redirectAttributes.addFlashAttribute("success", "Producto eliminado de tus favoritos.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ese producto no estaba en tu lista de favoritos.");
        }
        return redirectBack(next);
    }

    private Integer requireCustomer(HttpSession session, RedirectAttributes redirectAttributes) {
        Integer customerId = SessionUsers.customerCedula(session);
        if (customerId == null && SessionUsers.current(session) != null) {
            redirectAttributes.addFlashAttribute("error",
                    "Solo los clientes pueden guardar productos en favoritos.");
        }
        return customerId;
    }

    private String guestOrDenied(HttpSession session, String next) {
        if (SessionUsers.current(session) == null) {
            return "redirect:/login?next=" + safeNext(next);
        }
        return redirectBack(next);
    }

    private String redirectBack(String next) {
        return "redirect:" + safeNext(next);
    }

    private String safeNext(String next) {
        if (next == null || next.isBlank()) {
            return "/favoritos";
        }
        String value = next.trim();
        if (value.equals("/favoritos") || value.equals("/cart") || value.equals("/catalog")
                || value.matches("/catalog/\\d+")) {
            return value;
        }
        return "/favoritos";
    }
}
