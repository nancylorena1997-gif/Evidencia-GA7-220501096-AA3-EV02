package com.newpohone.orders;

import com.newpohone.database.CatalogRepository;
import com.newpohone.database.OrderRepository;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderManagementService {

    private final OrderRepository orderRepository;
    private final CatalogRepository catalogRepository;

    public OrderManagementService(OrderRepository orderRepository, CatalogRepository catalogRepository) {
        this.orderRepository = orderRepository;
        this.catalogRepository = catalogRepository;
    }

    public List<Map<String, Object>> list(String statusKey, String query) {
        OrderStatus filter = OrderStatus.fromKey(statusKey);
        String label = filter == null ? null : filter.getLabel();
        List<Map<String, Object>> orders = new ArrayList<>();
        for (Map<String, Object> row : orderRepository.findOrders(label, query)) {
            orders.add(enrich(row, false));
        }
        return orders;
    }

    public Map<String, Object> detail(int orderId) {
        Map<String, Object> order = orderRepository.findById(orderId);
        if (order == null) {
            throw new OrderException("No encontramos el pedido solicitado.");
        }
        Map<String, Object> view = enrich(order, true);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> item : orderRepository.findItems(orderId)) {
            Map<String, Object> line = new LinkedHashMap<>(item);
            line.put("subtotalLabel", money(((Number) item.get("subtotal")).doubleValue()));
            items.add(line);
        }
        view.put("items", items);
        view.put("steps", timeline(OrderStatus.from(String.valueOf(order.get("estado")))));
        return view;
    }

    public Map<String, Object> metrics() {
        Map<String, Integer> raw = orderRepository.countByStatus();
        Map<String, Object> metrics = new LinkedHashMap<>();
        int total = 0;
        for (OrderStatus status : OrderStatus.ALL) {
            int count = 0;
            for (Map.Entry<String, Integer> entry : raw.entrySet()) {
                if (OrderStatus.from(entry.getKey()) == status) {
                    count += entry.getValue();
                }
            }
            metrics.put(status.getKey(), count);
            total += count;
        }
        metrics.put("todos", total);
        metrics.put("abiertos", orderRepository.countOpen());
        return metrics;
    }

    public List<Map<String, Object>> customerOrders(Integer cedula) {
        if (cedula == null) {
            return List.of();
        }
        List<Map<String, Object>> orders = new ArrayList<>();
        for (Map<String, Object> row : orderRepository.findByCustomer(cedula)) {
            orders.add(enrich(row, false));
        }
        return orders;
    }

    @Transactional
    public String updateStatus(int orderId, String statusKey, boolean confirmed) {
        if (!confirmed) {
            throw new OrderException("Confirma la modificación del estado para guardarla.");
        }
        Map<String, Object> order = orderRepository.findById(orderId);
        if (order == null) {
            throw new OrderException("No encontramos el pedido solicitado.");
        }
        OrderStatus current = OrderStatus.from(String.valueOf(order.get("estado")));
        OrderStatus next = OrderStatus.fromKey(statusKey);
        if (next == null) {
            throw new OrderException("Selecciona un estado válido.");
        }
        if (current == next) {
            throw new OrderException("El pedido ya está en " + current.getLabel() + ".");
        }
        if (current.isTerminal()) {
            throw new OrderException("El pedido " + current.getLabel().toLowerCase(Locale.ROOT)
                    + " ya no admite cambios de estado.");
        }
        orderRepository.updateStatus(orderId, next.getLabel());
        if (next == OrderStatus.CANCELADO) {
            restoreStock(orderId);
        }
        return "El estado del pedido #" + orderId + " se actualizó a " + next.getLabel() + ".";
    }

    private void restoreStock(int orderId) {
        for (Map<String, Object> item : orderRepository.findItems(orderId)) {
            Object productId = item.get("productoId");
            if (productId instanceof Number number) {
                catalogRepository.incrementStock(number.intValue(), ((Number) item.get("cantidad")).intValue());
            }
        }
    }

    private Map<String, Object> enrich(Map<String, Object> order, boolean detailed) {
        Map<String, Object> view = new LinkedHashMap<>(order);
        OrderStatus status = OrderStatus.from(String.valueOf(order.get("estado")));
        view.put("status", status);
        view.put("statusKey", status.getKey());
        view.put("statusLabel", status.getLabel());
        view.put("statusClass", status.getCssClass());
        view.put("locked", status.isTerminal());
        view.put("totalLabel", money(((Number) order.get("total")).doubleValue()));
        if (view.get("cliente") == null || String.valueOf(view.get("cliente")).isBlank()) {
            view.put("cliente", view.get("destinatario") != null ? view.get("destinatario") : "Cliente invitado");
        }
        if (detailed) {
            view.put("statuses", OrderStatus.ALL);
        }
        return view;
    }

    public List<Map<String, Object>> timeline(OrderStatus current) {
        List<Map<String, Object>> steps = new ArrayList<>();
        String[] details = {
                "El pedido quedó registrado y el pago aprobado.",
                "Los productos se empacaron y están listos.",
                "El pedido salió hacia la dirección de entrega.",
                "El pedido llegó a destino."
        };
        for (int index = 0; index < OrderStatus.FLOW.size(); index++) {
            OrderStatus step = OrderStatus.FLOW.get(index);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", step.getLabel());
            row.put("detail", details[index]);
            boolean cancelled = current == OrderStatus.CANCELADO;
            row.put("done", !cancelled && current.getStep() >= step.getStep());
            row.put("current", !cancelled && current == step);
            steps.add(row);
        }
        return steps;
    }

    private String money(double value) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
        return "$" + format.format(Math.round(value));
    }
}
