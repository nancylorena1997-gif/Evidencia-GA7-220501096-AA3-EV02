package com.newpohone.web;

import com.newpohone.orders.OrderException;
import com.newpohone.orders.OrderManagementService;
import com.newpohone.orders.OrderStatus;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderManagementController {

    private final OrderManagementService orderManagementService;

    public OrderManagementController(OrderManagementService orderManagementService) {
        this.orderManagementService = orderManagementService;
    }

    @GetMapping("/pedidos")
    public String list(@RequestParam(required = false, defaultValue = "todos") String estado,
            @RequestParam(required = false) String q,
            Model model) {
        String status = sanitizeFilter(estado);
        model.addAttribute("pageTitle", "Gestión de pedidos");
        model.addAttribute("breadcrumb", "Pedidos");
        model.addAttribute("activeNav", "gestion-pedidos");
        model.addAttribute("estado", status);
        model.addAttribute("q", q);
        model.addAttribute("orders", orderManagementService.list(status, q));
        model.addAttribute("metrics", orderManagementService.metrics());
        model.addAttribute("statuses", OrderStatus.ALL);
        return "orders/index";
    }

    @GetMapping("/pedidos/{id}")
    public String detail(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> order = orderManagementService.detail(id);
            model.addAttribute("pageTitle", "Pedido #" + id);
            model.addAttribute("breadcrumb", "Pedidos");
            model.addAttribute("activeNav", "gestion-pedidos");
            model.addAttribute("order", order);
            return "orders/detail";
        } catch (OrderException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/pedidos";
        }
    }

    @PostMapping("/pedidos/{id}/estado")
    public String updateStatus(@PathVariable int id,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String confirmado,
            @RequestParam(required = false) String returnTo,
            @RequestParam(required = false, defaultValue = "todos") String filterEstado,
            @RequestParam(required = false) String q,
            RedirectAttributes redirectAttributes) {
        try {
            String message = orderManagementService.updateStatus(id, estado, "on".equals(confirmado));
            redirectAttributes.addFlashAttribute("success", message);
        } catch (OrderException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        if ("list".equals(returnTo)) {
            redirectAttributes.addAttribute("estado", sanitizeFilter(filterEstado));
            if (q != null && !q.isBlank()) {
                redirectAttributes.addAttribute("q", q.trim());
            }
            return "redirect:/pedidos";
        }
        return "redirect:/pedidos/" + id;
    }

    private String sanitizeFilter(String estado) {
        if (estado == null || estado.isBlank() || "todos".equalsIgnoreCase(estado)) {
            return "todos";
        }
        OrderStatus status = OrderStatus.fromKey(estado);
        return status == null ? "todos" : status.getKey();
    }
}
