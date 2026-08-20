package com.newpohone.web;

import com.newpohone.catalog.CartService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Map<String, Object> cart(HttpSession session) {
        return cartService.view(session);
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestParam int productId, HttpSession session) {
        return cartService.add(session, productId);
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestParam int productId,
            @RequestParam int cantidad,
            HttpSession session) {
        return cartService.update(session, productId, cantidad);
    }

    @PostMapping("/remove")
    public Map<String, Object> remove(@RequestParam int productId, HttpSession session) {
        return cartService.remove(session, productId);
    }

    @PostMapping("/clear")
    public Map<String, Object> clear(HttpSession session) {
        return cartService.clear(session);
    }
}
