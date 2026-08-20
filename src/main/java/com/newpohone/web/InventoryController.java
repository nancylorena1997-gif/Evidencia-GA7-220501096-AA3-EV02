package com.newpohone.web;

import com.newpohone.database.InventoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping("/inventario")
    public String inventory(@RequestParam(required = false, defaultValue = "todos") String estado,
            Model model) {
        String status = switch (estado) {
            case "agotado", "bajo", "alerta" -> estado;
            default -> "todos";
        };
        model.addAttribute("pageTitle", "Gestión de inventario");
        model.addAttribute("breadcrumb", "Inventario");
        model.addAttribute("activeNav", "inventario");
        model.addAttribute("estado", status);
        model.addAttribute("products", inventoryRepository.findProducts(status));
        model.addAttribute("catalogCount", inventoryRepository.countAll());
        model.addAttribute("alertCount", inventoryRepository.countAlerts());
        model.addAttribute("outCount", inventoryRepository.countOutOfStock());
        model.addAttribute("threshold", inventoryRepository.getThreshold());
        return "inventory/index";
    }
}
