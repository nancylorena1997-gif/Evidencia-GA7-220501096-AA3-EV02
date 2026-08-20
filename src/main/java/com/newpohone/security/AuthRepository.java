package com.newpohone.security;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbc;

    public AuthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> authenticate(String email, String password) {
        String sql = """
                SELECT c.idcuenta, c.email, c.contrasena,
                       COALESCE(a.nombre, cl.nombre, c.email) AS nombre,
                       COALESCE(a.cedula, cl.cedula) AS cedula,
                       CASE WHEN a.cedula IS NOT NULL THEN 'ADMINISTRADOR' ELSE 'CLIENTE' END AS rol
                FROM cuenta c
                LEFT JOIN administrador a ON a.cuenta_idcuenta = c.idcuenta
                LEFT JOIN cliente cl ON cl.cuenta_idcuenta = c.idcuenta
                WHERE LOWER(c.email) = LOWER(?)
                """;

        var rows = jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("idcuenta", rs.getInt("idcuenta"));
            row.put("email", rs.getString("email"));
            row.put("contrasena", rs.getString("contrasena"));
            row.put("nombre", rs.getString("nombre"));
            row.put("rol", rs.getString("rol"));
            row.put("cedula", rs.getObject("cedula"));
            return row;
        }, email.trim());

        if (rows.isEmpty()) {
            return null;
        }

        Map<String, Object> account = rows.getFirst();
        String storedPassword = String.valueOf(account.get("contrasena"));
        if (!PasswordHasher.verify(password, storedPassword)) {
            return null;
        }

        int accountId = ((Number) account.get("idcuenta")).intValue();
        if (PasswordHasher.needsUpgrade(storedPassword)) {
            jdbc.update("UPDATE cuenta SET contrasena = ? WHERE idcuenta = ?",
                    PasswordHasher.hash(password), accountId);
        }

        return user(accountId, String.valueOf(account.get("email")),
                String.valueOf(account.get("nombre")), String.valueOf(account.get("rol")),
                account.get("cedula"));
    }

    public Map<String, Object> register(int document, String name, String phone,
            String email, String password) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO cuenta (contrasena, email) VALUES (?, ?)",
                    new String[]{"idcuenta"});
            statement.setString(1, PasswordHasher.hash(password));
            statement.setString(2, email.trim().toLowerCase());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("No se generó el identificador de la cuenta.");
        }
        int accountId = key.intValue();

        jdbc.update("""
                INSERT INTO cliente (cedula, nombre, telefono, registro, cuenta_idcuenta)
                VALUES (?, ?, ?, date('now'), ?)
                """, document, name.trim(), phone.trim(), accountId);

        return user(accountId, email.trim().toLowerCase(), name.trim(), "CLIENTE", document);
    }

    private Map<String, Object> user(int accountId, String email, String name, String role, Object cedula) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", accountId);
        user.put("email", email);
        user.put("nombre", name);
        user.put("rol", role);
        user.put("cedula", cedula);
        return user;
    }
}
