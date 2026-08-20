package com.newpohone.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {

    private final JdbcTemplate jdbc;

    public CatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findCategories() {
        return jdbc.query("""
                SELECT c.id_categoria, c.nombre, COUNT(p.idproducto) AS total
                FROM categoria c
                LEFT JOIN producto p ON p.categoria_id_categoria = c.id_categoria
                GROUP BY c.id_categoria, c.nombre
                ORDER BY c.nombre
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getInt("id_categoria"));
            row.put("nombre", rs.getString("nombre"));
            row.put("total", rs.getInt("total"));
            return row;
        });
    }

    public Map<String, Object> priceBounds() {
        return jdbc.query("""
                SELECT COALESCE(MIN(precio), 0) AS min_precio,
                       COALESCE(MAX(precio), 0) AS max_precio,
                       COUNT(*) AS total
                FROM producto
                """, rs -> {
            rs.next();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("min", rs.getDouble("min_precio"));
            row.put("max", rs.getDouble("max_precio"));
            row.put("total", rs.getInt("total"));
            return row;
        });
    }

    public List<Map<String, Object>> search(String query, Integer categoryId, Double minPrice,
            Double maxPrice, boolean onlyAvailable, String sort) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.idproducto, p.nombre, p.precio, p.stock, p.descripcion,
                       c.id_categoria, c.nombre AS categoria,
                       COUNT(r.id_resena) AS review_count,
                       AVG(r.calificacion) AS review_avg
                FROM producto p
                LEFT JOIN categoria c ON c.id_categoria = p.categoria_id_categoria
                LEFT JOIN resena r ON r.producto_idproducto = p.idproducto
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(p.nombre) LIKE ? OR LOWER(p.descripcion) LIKE ? OR LOWER(c.nombre) LIKE ?)");
            String like = "%" + query.trim().toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (categoryId != null) {
            sql.append(" AND p.categoria_id_categoria = ?");
            params.add(categoryId);
        }
        if (minPrice != null) {
            sql.append(" AND p.precio >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND p.precio <= ?");
            params.add(maxPrice);
        }
        if (onlyAvailable) {
            sql.append(" AND p.stock > 0");
        }

        sql.append("""
                 GROUP BY p.idproducto, p.nombre, p.precio, p.stock, p.descripcion,
                          c.id_categoria, c.nombre
                 ORDER BY
                """).append(orderBy(sort));

        return jdbc.query(sql.toString(), (rs, rowNum) -> mapProduct(rs), params.toArray());
    }

    public Optional<Map<String, Object>> findById(int productId) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT p.idproducto, p.nombre, p.precio, p.stock, p.descripcion,
                       c.id_categoria, c.nombre AS categoria,
                       COUNT(r.id_resena) AS review_count,
                       AVG(r.calificacion) AS review_avg
                FROM producto p
                LEFT JOIN categoria c ON c.id_categoria = p.categoria_id_categoria
                LEFT JOIN resena r ON r.producto_idproducto = p.idproducto
                WHERE p.idproducto = ?
                GROUP BY p.idproducto, p.nombre, p.precio, p.stock, p.descripcion,
                         c.id_categoria, c.nombre
                """, (rs, rowNum) -> mapProduct(rs), productId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    public boolean decrementStock(int productId, int quantity) {
        int updated = jdbc.update("""
                UPDATE producto
                SET stock = stock - ?
                WHERE idproducto = ? AND stock >= ?
                """, quantity, productId, quantity);
        return updated == 1;
    }

    public void incrementStock(int productId, int quantity) {
        jdbc.update("""
                UPDATE producto
                SET stock = stock + ?
                WHERE idproducto = ?
                """, quantity, productId);
    }

    private Map<String, Object> mapProduct(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getInt("idproducto"));
        row.put("nombre", rs.getString("nombre"));
        row.put("precio", rs.getDouble("precio"));
        row.put("stock", rs.getInt("stock"));
        row.put("descripcion", rs.getString("descripcion"));
        row.put("categoriaId", rs.getObject("id_categoria"));
        row.put("categoria", rs.getString("categoria"));
        int reviewCount = rs.getInt("review_count");
        double reviewAvg = rs.getDouble("review_avg");
        if (rs.wasNull()) {
            reviewAvg = 0;
        }
        row.put("reviewCount", reviewCount);
        row.put("reviewAvg", reviewAvg);
        return row;
    }

    private String orderBy(String sort) {
        if (sort == null) {
            return "p.nombre ASC";
        }
        return switch (sort) {
            case "price_asc" -> "p.precio ASC, p.nombre ASC";
            case "price_desc" -> "p.precio DESC, p.nombre ASC";
            case "stock_desc" -> "p.stock DESC, p.nombre ASC";
            case "newest" -> "p.idproducto DESC";
            case "rating" -> "COALESCE(AVG(r.calificacion), 0) DESC, COUNT(r.id_resena) DESC, p.nombre ASC";
            default -> "p.nombre ASC";
        };
    }
}
