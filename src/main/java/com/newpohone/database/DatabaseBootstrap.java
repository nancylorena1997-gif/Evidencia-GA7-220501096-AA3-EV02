package com.newpohone.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DatabaseBootstrap {

    public static final String DATABASE_PROPERTY = "newphone.database";
    private static final Logger log = LoggerFactory.getLogger(DatabaseBootstrap.class);
    private static String jdbcUrl;

    private DatabaseBootstrap() {
    }

    public static synchronized String initialize() {
        if (jdbcUrl != null) {
            return jdbcUrl;
        }

        try {
            Class.forName("org.sqlite.JDBC");
            Path databasePath = resolveDatabasePath();
            Files.createDirectories(databasePath.getParent());
            jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();

            try (Connection connection = DriverManager.getConnection(jdbcUrl);
                    Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode = WAL");
                statement.execute("PRAGMA synchronous = NORMAL");
                statement.execute("PRAGMA foreign_keys = ON");
            }

            try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
                executeScript(connection, "/db/schema.sql");
                ensureColumn(connection, "envio", "guia", "TEXT");
                ensureColumn(connection, "envio", "telefono", "TEXT");
                ensureColumn(connection, "envio", "destinatario", "TEXT");
                ensureUniqueIndex(connection, "idx_resena_cliente_producto",
                        "CREATE UNIQUE INDEX IF NOT EXISTS idx_resena_cliente_producto ON resena(cliente_cedula, producto_idproducto)");
                ensureUniqueIndex(connection, "idx_favorito_cliente_producto",
                        "CREATE UNIQUE INDEX IF NOT EXISTS idx_favorito_cliente_producto ON producto_favorito(cliente_cedula, producto_idproducto)");
                executeScript(connection, "/db/seed.sql");
                normalizeOrderStatuses(connection);
            }

            log.info("SQLite listo en {}", databasePath.toAbsolutePath());
            return jdbcUrl;
        } catch (ClassNotFoundException | IOException | SQLException exception) {
            jdbcUrl = null;
            throw new IllegalStateException("No fue posible inicializar SQLite", exception);
        }
    }

    public static String getJdbcUrl() {
        return jdbcUrl == null ? initialize() : jdbcUrl;
    }

    public static String getLocation() {
        return getJdbcUrl().replace("jdbc:sqlite:", "");
    }

    private static Path resolveDatabasePath() {
        String configuredPath = System.getProperty(DATABASE_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv("NEWPHONE_DATABASE");
        }
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Paths.get(configuredPath.trim());
        }
        return Paths.get(System.getProperty("user.home"), ".newphone", "newphone-spring.db");
    }

    private static void ensureColumn(Connection connection, String table, String column, String definition)
            throws SQLException {
        boolean exists = false;
        try (var resultSet = connection.getMetaData().getColumns(null, null, table, column)) {
            exists = resultSet.next();
        }
        if (!exists) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
            log.info("Columna {}.{} agregada", table, column);
        }
    }

    private static void ensureUniqueIndex(Connection connection, String name, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            log.warn("No se pudo crear el índice {}: {}", name, exception.getMessage());
        }
    }

    private static void normalizeOrderStatuses(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    UPDATE pedido SET estado = CASE
                      WHEN lower(estado) LIKE '%cancel%' THEN 'Cancelado'
                      WHEN lower(estado) LIKE '%entreg%' THEN 'Entregado'
                      WHEN lower(estado) LIKE '%camino%' OR lower(estado) LIKE '%transito%'
                        OR lower(estado) LIKE '%tránsito%' OR lower(estado) LIKE '%envi%' THEN 'Enviado'
                      WHEN lower(estado) LIKE '%prepar%' THEN 'Preparado'
                      ELSE 'Pendiente'
                    END
                    WHERE estado NOT IN ('Pendiente', 'Preparado', 'Enviado', 'Entregado', 'Cancelado')
                    """);
            statement.execute("""
                    UPDATE envio SET estado_envio = CASE
                      WHEN lower(estado_envio) LIKE '%cancel%' THEN 'Cancelado'
                      WHEN lower(estado_envio) LIKE '%entreg%' THEN 'Entregado'
                      WHEN lower(estado_envio) LIKE '%camino%' OR lower(estado_envio) LIKE '%transito%'
                        OR lower(estado_envio) LIKE '%tránsito%' OR lower(estado_envio) LIKE '%envi%' THEN 'Enviado'
                      WHEN lower(estado_envio) LIKE '%prepar%' THEN 'Preparado'
                      ELSE 'Pendiente'
                    END
                    WHERE estado_envio NOT IN ('Pendiente', 'Preparado', 'Enviado', 'Entregado', 'Cancelado')
                    """);
            statement.execute("""
                    UPDATE pedido SET estado = (
                        SELECT e.estado_envio FROM envio e WHERE e.pedido_idpedido = pedido.idpedido LIMIT 1
                    )
                    WHERE EXISTS (
                        SELECT 1 FROM envio e
                        WHERE e.pedido_idpedido = pedido.idpedido
                          AND e.estado_envio IN ('Pendiente', 'Preparado', 'Enviado', 'Entregado', 'Cancelado')
                          AND e.estado_envio != pedido.estado
                    )
                    """);
        }
    }

    private static void executeScript(Connection connection, String resource) throws IOException, SQLException {
        String sql;
        try (InputStream stream = DatabaseBootstrap.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("No se encontró el recurso " + resource);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }
        }

        for (String statementSql : sql.split(";")) {
            String trimmed = statementSql.trim();
            if (!trimmed.isEmpty()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(trimmed);
                }
            }
        }
    }
}
