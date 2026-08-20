package com.newpohone.web;

import com.newpohone.database.FavoriteRepository;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {
        CatalogController.class,
        CartPageController.class,
        CheckoutController.class,
        FavoriteController.class
})
public class StoreSessionAdvice {

    private final FavoriteRepository favoriteRepository;

    public StoreSessionAdvice(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @ModelAttribute("storeUser")
    public Map<String, Object> storeUser(HttpSession session) {
        Map<String, Object> user = SessionUsers.current(session);
        if (user == null) {
            return null;
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("nombre", String.valueOf(user.getOrDefault("nombre", "Cliente")));
        view.put("rol", String.valueOf(user.getOrDefault("rol", "")));
        view.put("cliente", "CLIENTE".equals(user.get("rol")));
        view.put("admin", "ADMINISTRADOR".equals(user.get("rol")));
        return view;
    }

    @ModelAttribute("favoriteCount")
    public int favoriteCount(HttpSession session) {
        Integer customerId = SessionUsers.customerCedula(session);
        return customerId == null ? 0 : favoriteRepository.count(customerId);
    }
}
