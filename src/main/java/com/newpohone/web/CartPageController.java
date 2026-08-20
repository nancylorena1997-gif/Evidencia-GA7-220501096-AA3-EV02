package com.newpohone.web;

import com.newpohone.catalog.CartService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartPageController {

    private final CartService cartService;

    public CartPageController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String cart(HttpSession session, Model model) {
        Map<String, Object> cart = cartService.view(session);
        model.addAttribute("cart", cart);
        model.addAttribute("cartCount", cart.get("count"));
        model.addAttribute("cartEmpty", Boolean.TRUE.equals(cart.get("empty")));
        return "catalog/cart";
    }
}
