package com.newpohone.database;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryRepository {

    private final JdbcTemplate jdbc;
    private final int lowStockThreshold;

    public InventoryRepository(JdbcTemplate jdbc,
            @Value("${newphone.inventory.low-stock-threshold:30}") int lowStockThreshold) {
        this.jdbc = jdbc;
        this.lowStockThreshold = lowStockThreshold;
    }

    public int getThreshold() {
        return lowStockThreshold;
    }

    public List<Map<String, Object>> findProducts(String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.idproducto, p.nombre, p.precio, p.stock, p.descripcion,
                       c.nombre AS categoria
                FROM producto p
                LEFT JOIN categoria c ON c.id_categoria = p.categoria_id_categoria
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();
        if ("agotado".equals(status)) {
            sql.append(" AND p.stock <= 0");
        } else if ("bajo".equals(status)) {
            sql.append(" AND p.stock > 0 AND p.stock <= ?");
            params.add(lowStockThreshold);
        } else if ("alerta".equals(status)) {
            sql.append(" AND p.stock <= ?");
            params.add(lowStockThreshold);
        }
        sql.append(" ORDER BY p.stock ASC, p.nombre ASC");

        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            int stock = rs.getInt("stock");
            row.put("idproducto", rs.getInt("idproducto"));
            row.put("nombre", rs.getString("nombre"));
            row.put("precio", rs.getDouble("precio"));
            row.put("stock", stock);
            row.put("descripcion", rs.getString("descripcion"));
            row.put("categoria", rs.getString("categoria"));
            applyStatus(row, stock);
            return row;
        }, params.toArray());
    }

    public List<Map<String, Object>> findAlerts(int limit) {
        return jdbc.query("""
                SELECT p.idproducto, p.nombre, p.stock
                FROM producto p
                WHERE p.stock <= ?
                ORDER BY p.stock ASC, p.nombre ASC
                LIMIT ?
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            int stock = rs.getInt("stock");
            row.put("idproducto", rs.getInt("idproducto"));
            row.put("nombre", rs.getString("nombre"));
            row.put("stock", stock);
            applyStatus(row, stock);
            return row;
        }, lowStockThreshold, limit);
    }

    public int countAlerts() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM producto WHERE stock <= ?", Integer.class, lowStockThreshold);
        return count == null ? 0 : count;
    }

    public int countAll() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM producto", Integer.class);
        return count == null ? 0 : count;
    }

    public int countOutOfStock() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM producto WHERE stock <= 0", Integer.class);
        return count == null ? 0 : count;
    }

    private void applyStatus(Map<String, Object> row, int stock) {
        if (stock <= 0) {
            row.put("level", "out");
            row.put("stockLabel", "Agotado");
        } else if (stock <= lowStockThreshold) {
            row.put("level", "low");
            row.put("stockLabel", "Stock bajo");
        } else {
            row.put("level", "ok");
            row.put("stockLabel", "Disponible");
        }
    }
}
