package com.newpohone.web;

import com.newpohone.catalog.CartService;
import com.newpohone.catalog.ProductPresenter;
import com.newpohone.database.CatalogRepository;
import com.newpohone.database.FavoriteRepository;
import com.newpohone.database.ReviewRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CatalogController {

    private final CatalogRepository catalogRepository;
    private final ProductPresenter productPresenter;
    private final CartService cartService;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;

    public CatalogController(CatalogRepository catalogRepository,
            ProductPresenter productPresenter,
            CartService cartService,
            ReviewRepository reviewRepository,
            FavoriteRepository favoriteRepository) {
        this.catalogRepository = catalogRepository;
        this.productPresenter = productPresenter;
        this.cartService = cartService;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
    }

    @GetMapping({"/", "/catalog"})
    public String catalog(@RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String min,
            @RequestParam(required = false) String max,
            @RequestParam(required = false, defaultValue = "false") boolean disponibles,
            @RequestParam(required = false, defaultValue = "name") String sort,
            HttpSession session,
            Model model) {
        Integer categoryId = parseInteger(categoria);
        Double minPrice = parseDouble(min);
        Double maxPrice = parseDouble(max);

        Map<String, Object> bounds = catalogRepository.priceBounds();
        List<Map<String, Object>> categories = catalogRepository.findCategories();
        List<Map<String, Object>> products = catalogRepository.search(
                q, categoryId, minPrice, maxPrice, disponibles, sort);

        productPresenter.enrich(products);
        markFavorites(products, SessionUsers.customerCedula(session));

        Integer customerId = SessionUsers.customerCedula(session);
        if (customerId != null) {
            model.addAttribute("pendingReviews", reviewRepository.findPending(customerId));
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("bounds", bounds);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("categoria", categoryId);
        model.addAttribute("min", minPrice);
        model.addAttribute("max", maxPrice);
        model.addAttribute("disponibles", disponibles);
        model.addAttribute("sort", sort);
        model.addAttribute("resultCount", products.size());
        model.addAttribute("cartCount", cartService.view(session).get("count"));
        model.addAttribute("hasFilters",
                (q != null && !q.isBlank())
                        || categoryId != null
                        || minPrice != null
                        || maxPrice != null
                        || disponibles
                        || (sort != null && !"name".equals(sort)));
        return "catalog/index";
    }

    @GetMapping("/catalog/{id}")
    public String product(@PathVariable int id, HttpSession session, Model model,
            RedirectAttributes redirectAttributes) {
        return catalogRepository.findById(id).map(product -> {
            productPresenter.enrich(product);
            Integer customerId = SessionUsers.customerCedula(session);
            boolean purchased = customerId != null && reviewRepository.hasPurchased(customerId, id);
            Map<String, Object> ownReview = customerId == null
                    ? null
                    : reviewRepository.findByCustomerAndProduct(customerId, id);

            model.addAttribute("product", product);
            model.addAttribute("reviews", reviewRepository.findByProduct(id));
            model.addAttribute("purchased", purchased);
            model.addAttribute("ownReview", ownReview);
            model.addAttribute("canReview", purchased && ownReview == null);
            model.addAttribute("favorite", customerId != null && favoriteRepository.isFavorite(customerId, id));
            model.addAttribute("cartCount", cartService.view(session).get("count"));
            return "catalog/product";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Ese producto no está en el catálogo.");
            return "redirect:/catalog";
        });
    }

    @PostMapping("/catalog/{id}/resena")
    public String saveReview(@PathVariable int id,
            @RequestParam(required = false) Integer calificacion,
            @RequestParam(required = false) String comentario,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (catalogRepository.findById(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ese producto no está en el catálogo.");
            return "redirect:/catalog";
        }

        Integer customerId = SessionUsers.customerCedula(session);
        if (customerId == null) {
            if (SessionUsers.current(session) == null) {
                return "redirect:/login?next=/catalog/" + id;
            }
            redirectAttributes.addFlashAttribute("error",
                    "Solo los clientes pueden calificar los productos que compraron.");
            return "redirect:/catalog/" + id;
        }
        if (!reviewRepository.hasPurchased(customerId, id)) {
            redirectAttributes.addFlashAttribute("error",
                    "Puedes calificar este producto cuando lo hayas comprado.");
            return "redirect:/catalog/" + id;
        }

        int rating = calificacion == null ? 0 : calificacion;
        String comment = comentario == null ? "" : comentario.trim();
        if (rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("error", "Selecciona una calificación de 1 a 5 estrellas.");
            return "redirect:/catalog/" + id;
        }
        if (comment.length() < 10 || comment.length() > 400) {
            redirectAttributes.addFlashAttribute("error",
                    "El comentario debe tener entre 10 y 400 caracteres.");
            return "redirect:/catalog/" + id;
        }

        reviewRepository.upsert(customerId, id, rating, comment);
        redirectAttributes.addFlashAttribute("success", "Tu reseña ya está publicada.");
        return "redirect:/catalog/" + id + "#resenas";
    }

    private void markFavorites(List<Map<String, Object>> products, Integer customerId) {
        Set<Integer> ids = customerId == null
                ? Set.of()
                : favoriteRepository.findProductIds(customerId);
        for (Map<String, Object> product : products) {
            int id = ((Number) product.get("id")).intValue();
            product.put("favorite", ids.contains(id));
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
