package com.newpohone.web;

import com.newpohone.catalog.CartService;
import com.newpohone.catalog.CheckoutException;
import com.newpohone.catalog.CheckoutIntent;
import com.newpohone.catalog.CheckoutService;
import com.newpohone.orders.OrderManagementService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutController {

    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final OrderManagementService orderManagementService;

    public CheckoutController(CartService cartService, CheckoutService checkoutService,
            OrderManagementService orderManagementService) {
        this.cartService = cartService;
        this.checkoutService = checkoutService;
        this.orderManagementService = orderManagementService;
    }

    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model) {
        Map<String, Object> cart = cartService.view(session);
        if (Boolean.TRUE.equals(cart.get("empty"))) {
            return "redirect:/cart";
        }
        model.addAttribute("cart", cart);
        model.addAttribute("cartCount", cart.get("count"));
        model.addAttribute("methods", CheckoutService.PAYMENT_METHODS);
        return "catalog/checkout";
    }

    @PostMapping("/checkout")
    public String startPayment(@RequestParam(required = false) String metodo,
            @RequestParam(required = false) String titular,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String vencimiento,
            @RequestParam(required = false) String cvv,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String direccion,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            checkoutService.prepare(session, metodo, titular, numero, vencimiento, cvv, nombre, telefono, direccion);
            return "redirect:/checkout/procesando";
        } catch (CheckoutException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            redirectAttributes.addFlashAttribute("metodo", metodo);
            redirectAttributes.addFlashAttribute("nombre", nombre);
            redirectAttributes.addFlashAttribute("telefono", telefono);
            redirectAttributes.addFlashAttribute("direccion", direccion);
            return "redirect:/checkout";
        }
    }

    @GetMapping("/checkout/procesando")
    public String processing(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        try {
            CheckoutIntent intent = checkoutService.requireIntent(session);
            model.addAttribute("intent", intent);
            model.addAttribute("cart", intent.getCart());
            return "catalog/checkout-processing";
        } catch (CheckoutException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/checkout";
        }
    }

    @PostMapping("/checkout/confirmar")
    public String confirm(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> order = checkoutService.confirm(session);
            redirectAttributes.addFlashAttribute("order", order);
            return "redirect:/checkout/exito";
        } catch (CheckoutException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/checkout/exito")
    public String success(Model model) {
        if (!model.containsAttribute("order")) {
            return "redirect:/catalog";
        }
        return "catalog/checkout-success";
    }

    @GetMapping("/seguimiento")
    public String trackingForm(@RequestParam(required = false) String guia,
            @RequestParam(required = false) String telefono,
            HttpSession session,
            Model model) {
        boolean lookup = guia != null && !guia.isBlank() && telefono != null && !telefono.isBlank();
        Object last = session.getAttribute("lastTracking");
        if (!lookup && last instanceof Map<?, ?> map) {
            guia = String.valueOf(map.get("guia"));
            telefono = String.valueOf(map.get("telefono"));
        }
        model.addAttribute("guia", guia);
        model.addAttribute("telefono", telefono);
        addCustomerOrders(session, model);
        if (lookup) {
            try {
                model.addAttribute("tracking", checkoutService.track(guia, telefono));
            } catch (CheckoutException exception) {
                model.addAttribute("error", exception.getMessage());
            }
        }
        return "catalog/tracking";
    }

    @PostMapping("/seguimiento")
    public String tracking(@RequestParam(required = false) String guia,
            @RequestParam(required = false) String telefono,
            HttpSession session,
            Model model) {
        model.addAttribute("guia", guia);
        model.addAttribute("telefono", telefono);
        addCustomerOrders(session, model);
        try {
            model.addAttribute("tracking", checkoutService.track(guia, telefono));
        } catch (CheckoutException exception) {
            model.addAttribute("error", exception.getMessage());
        }
        return "catalog/tracking";
    }

    private void addCustomerOrders(HttpSession session, Model model) {
        model.addAttribute("myOrders", orderManagementService.customerOrders(SessionUsers.customerCedula(session)));
    }
}
