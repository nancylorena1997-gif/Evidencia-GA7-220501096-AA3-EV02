package com.newpohone.web;

import com.newpohone.database.InventoryRepository;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {
        DashboardController.class,
        ModuleController.class,
        InventoryController.class,
        OrderManagementController.class
})
public class InventoryAdvice {

    private final InventoryRepository inventoryRepository;

    public InventoryAdvice(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @ModelAttribute("inventoryAlertCount")
    public int inventoryAlertCount() {
        return inventoryRepository.countAlerts();
    }

    @ModelAttribute("inventoryAlerts")
    public List<Map<String, Object>> inventoryAlerts() {
        return inventoryRepository.findAlerts(5);
    }

    @ModelAttribute("inventoryThreshold")
    public int inventoryThreshold() {
        return inventoryRepository.getThreshold();
    }
}
