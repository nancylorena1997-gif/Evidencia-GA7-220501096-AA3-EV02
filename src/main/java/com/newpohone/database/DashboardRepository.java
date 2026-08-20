package com.newpohone.database;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sales", scalar("SELECT COALESCE(SUM(total), 0) FROM pedido"));
        result.put("orders", scalar("SELECT COUNT(*) FROM pedido"));
        result.put("customers", scalar("SELECT COUNT(*) FROM cliente"));
        result.put("products", scalar("SELECT COUNT(*) FROM producto"));
        result.put("pendingTickets", scalar(
                "SELECT COUNT(*) FROM atencion_cliente WHERE respuesta IS NULL OR TRIM(respuesta) = ''"));
        result.put("lowStock", query(
                "SELECT idproducto, nombre, stock FROM producto ORDER BY stock ASC LIMIT 5"));
        result.put("recentOrders", query("""
                SELECT p.idpedido, p.fecha, p.estado, p.total, c.nombre AS cliente
                FROM pedido p
                LEFT JOIN cliente c ON c.cedula = p.cliente_cedula
                ORDER BY p.fecha DESC, p.idpedido DESC
                LIMIT 6
                """));
        result.put("salesByCategory", query("""
                SELECT c.nombre, COALESCE(SUM(d.subtotal), 0) AS total
                FROM categoria c
                LEFT JOIN producto p ON p.categoria_id_categoria = c.id_categoria
                LEFT JOIN detalle_pedido d ON d.producto_idproducto = p.idproducto
                GROUP BY c.id_categoria, c.nombre
                ORDER BY total DESC
                """));
        return result;
    }

    private Object scalar(String sql) {
        Object value = jdbc.queryForObject(sql, Object.class);
        return value == null ? 0 : value;
    }

    private List<Map<String, Object>> query(String sql) {
        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            var metadata = rs.getMetaData();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                row.put(metadata.getColumnLabel(index), rs.getObject(index));
            }
            return row;
        });
    }
}
