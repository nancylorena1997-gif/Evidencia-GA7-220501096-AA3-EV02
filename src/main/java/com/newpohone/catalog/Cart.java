package com.newpohone.catalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cart implements Serializable {

    public static final String SESSION_KEY = "catalogCart";

    private final Map<Integer, CartItem> items = new LinkedHashMap<>();

    public List<CartItem> getItems() {
        return new ArrayList<>(items.values());
    }

    public CartItem get(int productId) {
        return items.get(productId);
    }

    public void put(CartItem item) {
        items.put(item.getId(), item);
    }

    public void remove(int productId) {
        items.remove(productId);
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getLineCount() {
        return items.size();
    }

    public int getItemCount() {
        return items.values().stream().mapToInt(CartItem::getCantidad).sum();
    }

    public double getTotal() {
        return items.values().stream().mapToDouble(CartItem::getSubtotal).sum();
    }
}
