package com.newpohone.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findOrders(String statusLabel, String query) {
        StringBuilder sql = new StringBuilder(baseSelect());
        sql.append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (statusLabel != null && !statusLabel.isBlank()) {
            sql.append(" AND p.estado = ?");
            params.add(statusLabel);
        }
        appendSearch(sql, params, query);
        sql.append(" ORDER BY p.fecha DESC, p.idpedido DESC");
        return jdbc.query(sql.toString(), (rs, rowNum) -> mapOrder(rs), params.toArray());
    }

    public Map<String, Object> findById(int orderId) {
        List<Map<String, Object>> rows = jdbc.query(
                baseSelect() + " WHERE p.idpedido = ?",
                (rs, rowNum) -> mapOrder(rs),
                orderId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<Map<String, Object>> findByCustomer(int cedula) {
        return jdbc.query(
                baseSelect() + " WHERE p.cliente_cedula = ? ORDER BY p.fecha DESC, p.idpedido DESC",
                (rs, rowNum) -> mapOrder(rs),
                cedula);
    }

    public List<Map<String, Object>> findItems(int orderId) {
        return jdbc.query("""
                SELECT d.cantidad, d.subtotal, d.producto_idproducto, pr.nombre
                FROM detalle_pedido d
                LEFT JOIN producto pr ON pr.idproducto = d.producto_idproducto
                WHERE d.pedido_idpedido = ?
                ORDER BY d.iddetalle_pedido
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("cantidad", rs.getInt("cantidad"));
            row.put("subtotal", rs.getDouble("subtotal"));
            row.put("productoId", rs.getObject("producto_idproducto"));
            row.put("nombre", rs.getString("nombre"));
            return row;
        }, orderId);
    }

    public Map<String, Integer> countByStatus() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        jdbc.query("SELECT estado, COUNT(*) AS total FROM pedido GROUP BY estado", rs -> {
            counts.put(rs.getString("estado"), rs.getInt("total"));
        });
        return counts;
    }

    public int countOpen() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pedido
                WHERE estado IN ('Pendiente', 'Preparado')
                """, Integer.class);
        return count == null ? 0 : count;
    }

    public void updateStatus(int orderId, String status) {
        jdbc.update("UPDATE pedido SET estado = ? WHERE idpedido = ?", status, orderId);
        jdbc.update("UPDATE envio SET estado_envio = ? WHERE pedido_idpedido = ?", status, orderId);
    }

    private String baseSelect() {
        return """
                SELECT p.idpedido, p.fecha, p.estado, p.total, p.cliente_cedula,
                       c.nombre AS cliente, c.telefono AS cliente_telefono,
                       e.id_envio, e.guia, e.direccion, e.estado_envio, e.destinatario,
                       e.telefono AS envio_telefono, e.fecha_envio,
                       pay.metodo, pay.estado_pago
                FROM pedido p
                LEFT JOIN cliente c ON c.cedula = p.cliente_cedula
                LEFT JOIN envio e ON e.pedido_idpedido = p.idpedido
                LEFT JOIN pagos pay ON pay.pedido_idpedido = p.idpedido
                """;
    }

    private void appendSearch(StringBuilder sql, List<Object> params, String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        String like = "%" + query.trim().toLowerCase() + "%";
        sql.append("""
                 AND (
                    CAST(p.idpedido AS TEXT) LIKE ?
                    OR LOWER(COALESCE(c.nombre, '')) LIKE ?
                    OR LOWER(COALESCE(e.guia, '')) LIKE ?
                    OR LOWER(COALESCE(e.destinatario, '')) LIKE ?
                    OR REPLACE(REPLACE(REPLACE(COALESCE(e.telefono, ''), ' ', ''), '-', ''), '+', '') LIKE ?
                    OR REPLACE(REPLACE(REPLACE(COALESCE(c.telefono, ''), ' ', ''), '-', ''), '+', '') LIKE ?
                 )
                """);
        params.add(like);
        params.add(like);
        params.add(like);
        params.add(like);
        params.add(query.trim().replaceAll("\\D", ""));
        params.add(query.trim().replaceAll("\\D", ""));
    }

    private Map<String, Object> mapOrder(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getInt("idpedido"));
        row.put("fecha", rs.getString("fecha"));
        row.put("estado", rs.getString("estado"));
        row.put("total", rs.getDouble("total"));
        row.put("clienteCedula", rs.getObject("cliente_cedula"));
        row.put("cliente", rs.getString("cliente"));
        row.put("clienteTelefono", rs.getString("cliente_telefono"));
        row.put("envioId", rs.getObject("id_envio"));
        row.put("guia", rs.getString("guia"));
        row.put("direccion", rs.getString("direccion"));
        row.put("estadoEnvio", rs.getString("estado_envio"));
        row.put("destinatario", rs.getString("destinatario"));
        row.put("envioTelefono", rs.getString("envio_telefono"));
        row.put("fechaEnvio", rs.getString("fecha_envio"));
        row.put("metodo", rs.getString("metodo"));
        row.put("estadoPago", rs.getString("estado_pago"));
        return row;
    }
}
