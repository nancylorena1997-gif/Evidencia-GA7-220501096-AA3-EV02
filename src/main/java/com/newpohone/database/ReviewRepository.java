package com.newpohone.database;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRepository {

    private final JdbcTemplate jdbc;

    public ReviewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findByProduct(int productId) {
        return jdbc.query("""
                SELECT r.id_resena, r.comentario, r.calificacion, c.nombre AS cliente
                FROM resena r
                LEFT JOIN cliente c ON c.cedula = r.cliente_cedula
                WHERE r.producto_idproducto = ?
                ORDER BY r.id_resena DESC
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            int rating = rs.getInt("calificacion");
            row.put("id", rs.getInt("id_resena"));
            row.put("comentario", rs.getString("comentario"));
            row.put("calificacion", rating);
            row.put("cliente", rs.getString("cliente"));
            row.put("starPercent", rating * 20);
            return row;
        }, productId);
    }

    public Map<String, Object> findByCustomerAndProduct(int customerId, int productId) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT id_resena, comentario, calificacion
                FROM resena
                WHERE cliente_cedula = ? AND producto_idproducto = ?
                LIMIT 1
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getInt("id_resena"));
            row.put("comentario", rs.getString("comentario"));
            row.put("calificacion", rs.getInt("calificacion"));
            return row;
        }, customerId, productId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public boolean hasPurchased(int customerId, int productId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM pedido p
                JOIN detalle_pedido d ON d.pedido_idpedido = p.idpedido
                WHERE p.cliente_cedula = ? AND d.producto_idproducto = ?
                """, Integer.class, customerId, productId);
        return count != null && count > 0;
    }

    public List<Map<String, Object>> findPending(int customerId) {
        return jdbc.query("""
                SELECT DISTINCT p.idproducto, p.nombre
                FROM pedido pe
                JOIN detalle_pedido d ON d.pedido_idpedido = pe.idpedido
                JOIN producto p ON p.idproducto = d.producto_idproducto
                WHERE pe.cliente_cedula = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM resena r
                      WHERE r.cliente_cedula = pe.cliente_cedula
                        AND r.producto_idproducto = p.idproducto
                  )
                ORDER BY p.nombre
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getInt("idproducto"));
            row.put("nombre", rs.getString("nombre"));
            return row;
        }, customerId);
    }

    public void upsert(int customerId, int productId, int rating, String comment) {
        Map<String, Object> existing = findByCustomerAndProduct(customerId, productId);
        if (existing == null) {
            jdbc.update("""
                    INSERT INTO resena (comentario, calificacion, cliente_cedula, producto_idproducto)
                    VALUES (?, ?, ?, ?)
                    """, comment, rating, customerId, productId);
            return;
        }
        jdbc.update("""
                UPDATE resena
                SET comentario = ?, calificacion = ?
                WHERE id_resena = ?
                """, comment, rating, existing.get("id"));
    }
}
