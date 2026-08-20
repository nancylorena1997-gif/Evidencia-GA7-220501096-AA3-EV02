package com.newpohone.catalog;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProductPresenter {

    private static final String[] ACCENTS = {"cyan", "magenta", "violet", "aqua"};

    public void enrich(List<Map<String, Object>> products) {
        for (int index = 0; index < products.size(); index++) {
            enrich(products.get(index), index);
        }
    }

    public void enrich(Map<String, Object> product) {
        enrich(product, 0);
    }

    public void enrich(Map<String, Object> product, int index) {
        product.put("accent", ACCENTS[index % ACCENTS.length]);
        product.put("image", resolveImage(product));
        int stock = ((Number) product.get("stock")).intValue();
        product.put("available", stock > 0);
        if (stock <= 0) {
            product.put("stockLabel", "Agotado");
        } else if (stock < 30) {
            product.put("stockLabel", "Últimas unidades");
        } else {
            product.put("stockLabel", "Disponible");
        }

        int reviewCount = number(product.get("reviewCount"));
        double reviewAvg = product.get("reviewAvg") instanceof Number value ? value.doubleValue() : 0;
        product.put("reviewCount", reviewCount);
        product.put("reviewAvg", reviewAvg);
        product.put("hasReviews", reviewCount > 0);
        product.put("starPercent", (int) Math.round((reviewAvg / 5.0) * 100));
        if (reviewCount == 0) {
            product.put("reviewSummary", "Sin reseñas");
            product.put("reviewAvgLabel", "—");
        } else {
            String avgLabel = String.format(Locale.US, "%.1f", reviewAvg);
            product.put("reviewAvgLabel", avgLabel);
            product.put("reviewSummary", reviewCount == 1
                    ? avgLabel + " · 1 reseña"
                    : avgLabel + " · " + reviewCount + " reseñas");
        }
    }

    public String resolveImage(Map<String, Object> product) {
        String name = String.valueOf(product.getOrDefault("nombre", "")).toLowerCase();
        String category = String.valueOf(product.getOrDefault("categoria", "")).toLowerCase();
        String haystack = name + " " + category;

        if (haystack.contains("bud") || haystack.contains("audio") || haystack.contains("auricular")) {
            return "/images/products/product-earbuds.png";
        }
        if (haystack.contains("cargador") || haystack.contains("charger") || haystack.contains("adaptador")) {
            return "/images/products/product-charger.png";
        }
        if (haystack.contains("funda") || haystack.contains("case") || haystack.contains("magsafe")
                || haystack.contains("protector")) {
            return "/images/products/product-case.png";
        }
        if (haystack.contains("smartphone") || haystack.contains("phone") || haystack.contains("celular")) {
            return "/images/products/product-smartphone.png";
        }
        if (category.contains("accesorio")) {
            return "/images/products/product-case.png";
        }
        return "/images/products/product-smartphone.png";
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
