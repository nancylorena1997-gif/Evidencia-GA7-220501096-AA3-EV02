package com.newpohone.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FavoriteRepository {

    private final JdbcTemplate jdbc;

    public FavoriteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findByCustomer(int customerId) {
        return jdbc.query("""
                SELECT p.idproducto, p.nombre, p.precio, p.stock, p.descripcion,
                       c.id_categoria, c.nombre AS categoria,
                       f.fecha_agregado,
                       COUNT(r.id_resena) AS review_count,
                       AVG(r.calificacion) AS review_avg
                FROM producto_favorito f
                JOIN producto p ON p.idproducto = f.producto_idproducto
                LEFT JOIN categoria c ON c.id_categoria = p.categoria_id_categoria
                LEFT JOIN resena r ON r.producto_idproducto = p.idproducto
                WHERE f.cliente_cedula = ?
                GROUP BY p.idproducto, p.nombre, p.precio, p.stock, p.descripcion,
                         c.id_categoria, c.nombre, f.fecha_agregado
                ORDER BY f.fecha_agregado DESC, p.nombre ASC
                """, (rs, rowNum) -> mapProduct(rs), customerId);
    }

    public Set<Integer> findProductIds(int customerId) {
        return new HashSet<>(jdbc.query("""
                SELECT producto_idproducto
                FROM producto_favorito
                WHERE cliente_cedula = ?
                """, (rs, rowNum) -> rs.getInt("producto_idproducto"), customerId));
    }

    public boolean isFavorite(int customerId, int productId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM producto_favorito
                WHERE cliente_cedula = ? AND producto_idproducto = ?
                """, Integer.class, customerId, productId);
        return count != null && count > 0;
    }

    public int count(int customerId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM producto_favorito WHERE cliente_cedula = ?",
                Integer.class, customerId);
        return count == null ? 0 : count;
    }

    public boolean add(int customerId, int productId) {
        if (isFavorite(customerId, productId)) {
            return false;
        }
        jdbc.update("""
                INSERT INTO producto_favorito (fecha_agregado, producto_idproducto, cliente_cedula)
                VALUES (date('now'), ?, ?)
                """, productId, customerId);
        return true;
    }

    public boolean remove(int customerId, int productId) {
        return jdbc.update("""
                DELETE FROM producto_favorito
                WHERE cliente_cedula = ? AND producto_idproducto = ?
                """, customerId, productId) > 0;
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
        row.put("fechaAgregado", rs.getString("fecha_agregado"));
        int reviewCount = rs.getInt("review_count");
        double reviewAvg = rs.getDouble("review_avg");
        if (rs.wasNull()) {
            reviewAvg = 0;
        }
        row.put("reviewCount", reviewCount);
        row.put("reviewAvg", reviewAvg);
        row.put("favorite", true);
        return row;
    }
}
