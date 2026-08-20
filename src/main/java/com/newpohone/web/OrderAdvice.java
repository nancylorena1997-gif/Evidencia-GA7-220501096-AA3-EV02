package com.newpohone.web;

import com.newpohone.database.OrderRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {
        DashboardController.class,
        ModuleController.class,
        InventoryController.class,
        OrderManagementController.class
})
public class OrderAdvice {

    private final OrderRepository orderRepository;

    public OrderAdvice(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @ModelAttribute("pendingOrderCount")
    public int pendingOrderCount() {
        return orderRepository.countOpen();
    }
}
