package com.newpohone.database;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class CheckoutRepository {

    private final JdbcTemplate jdbc;

    public CheckoutRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insertOrder(double total, Integer customerId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO pedido (fecha, estado, total, cliente_cedula)
                    VALUES (date('now'), 'Pendiente', ?, ?)
                    """, new String[]{"idpedido"});
            statement.setDouble(1, total);
            if (customerId == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, customerId);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("No se generó el identificador del pedido.");
        }
        return key.intValue();
    }

    public void insertDetail(int orderId, int productId, int quantity, double subtotal) {
        jdbc.update("""
                INSERT INTO detalle_pedido (cantidad, subtotal, pedido_idpedido, producto_idproducto)
                VALUES (?, ?, ?, ?)
                """, quantity, subtotal, orderId, productId);
    }

    public void insertPayment(int orderId, String method) {
        jdbc.update("""
                INSERT INTO pagos (metodo, estado_pago, pedido_idpedido)
                VALUES (?, 'Aprobado', ?)
                """, method, orderId);
    }

    public void insertShipment(int orderId, String address, String phone, String recipient, String trackingCode) {
        jdbc.update("""
                INSERT INTO envio (direccion, fecha_envio, estado_envio, pedido_idpedido, guia, telefono, destinatario)
                VALUES (?, date('now'), 'Pendiente', ?, ?, ?, ?)
                """, address, orderId, trackingCode, phone, recipient);
    }

    public Map<String, Object> findTracking(String trackingCode, String phoneDigits) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT e.guia, e.telefono, e.destinatario, e.direccion, e.fecha_envio, e.estado_envio,
                       p.idpedido, p.fecha, p.estado, p.total,
                       pay.metodo, pay.estado_pago
                FROM envio e
                JOIN pedido p ON p.idpedido = e.pedido_idpedido
                LEFT JOIN pagos pay ON pay.pedido_idpedido = p.idpedido
                WHERE UPPER(TRIM(e.guia)) = UPPER(TRIM(?))
                  AND REPLACE(REPLACE(REPLACE(COALESCE(e.telefono, ''), ' ', ''), '-', ''), '+', '') = ?
                LIMIT 1
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("guia", rs.getString("guia"));
            row.put("telefono", rs.getString("telefono"));
            row.put("destinatario", rs.getString("destinatario"));
            row.put("direccion", rs.getString("direccion"));
            row.put("fechaEnvio", rs.getString("fecha_envio"));
            row.put("estadoEnvio", rs.getString("estado_envio"));
            row.put("pedidoId", rs.getInt("idpedido"));
            row.put("fechaPedido", rs.getString("fecha"));
            row.put("estadoPedido", rs.getString("estado"));
            row.put("total", rs.getDouble("total"));
            row.put("metodo", rs.getString("metodo"));
            row.put("estadoPago", rs.getString("estado_pago"));
            return row;
        }, trackingCode, phoneDigits);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<Map<String, Object>> findOrderItems(int orderId) {
        return jdbc.query("""
                SELECT d.cantidad, d.subtotal, pr.nombre
                FROM detalle_pedido d
                LEFT JOIN producto pr ON pr.idproducto = d.producto_idproducto
                WHERE d.pedido_idpedido = ?
                ORDER BY d.iddetalle_pedido
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("cantidad", rs.getInt("cantidad"));
            row.put("subtotal", rs.getDouble("subtotal"));
            row.put("nombre", rs.getString("nombre"));
            return row;
        }, orderId);
    }
}
