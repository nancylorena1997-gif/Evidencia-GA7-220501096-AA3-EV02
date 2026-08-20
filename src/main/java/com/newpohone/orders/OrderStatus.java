package com.newpohone.orders;

import java.util.List;
import java.util.Locale;

public enum OrderStatus {
    PENDIENTE("pendiente", "Pendiente", "pending", 0, false),
    PREPARADO("preparado", "Preparado", "ready", 1, false),
    ENVIADO("enviado", "Enviado", "shipped", 2, false),
    ENTREGADO("entregado", "Entregado", "delivered", 3, true),
    CANCELADO("cancelado", "Cancelado", "cancelled", -1, true);

    public static final List<OrderStatus> ALL = List.of(values());
    public static final List<OrderStatus> FLOW = List.of(PENDIENTE, PREPARADO, ENVIADO, ENTREGADO);

    private final String key;
    private final String label;
    private final String cssClass;
    private final int step;
    private final boolean terminal;

    OrderStatus(String key, String label, String cssClass, int step, boolean terminal) {
        this.key = key;
        this.label = label;
        this.cssClass = cssClass;
        this.step = step;
        this.terminal = terminal;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }

    public int getStep() {
        return step;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public static OrderStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return PENDIENTE;
        }
        String trimmed = raw.trim();
        for (OrderStatus status : values()) {
            if (status.key.equalsIgnoreCase(trimmed) || status.label.equalsIgnoreCase(trimmed)) {
                return status;
            }
        }
        String value = trimmed.toLowerCase(Locale.ROOT);
        if (value.contains("cancel")) {
            return CANCELADO;
        }
        if (value.contains("entreg")) {
            return ENTREGADO;
        }
        if (value.contains("camino") || value.contains("tránsito") || value.contains("transito")
                || value.contains("envi")) {
            return ENVIADO;
        }
        if (value.contains("prepar")) {
            return PREPARADO;
        }
        return PENDIENTE;
    }

    public static OrderStatus fromKey(String key) {
        if (key == null || key.isBlank() || "todos".equalsIgnoreCase(key.trim())) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.key.equalsIgnoreCase(key.trim())) {
                return status;
            }
        }
        return null;
    }
}
