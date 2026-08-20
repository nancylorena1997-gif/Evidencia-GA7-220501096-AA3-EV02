package com.newpohone.database;

import com.newpohone.module.ModuleDefinition;
import com.newpohone.module.ModuleDefinition.FieldDefinition;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class GenericModuleRepository {

    private final JdbcTemplate jdbc;

    public GenericModuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAll(ModuleDefinition module, String query) {
        return findAll(module, query, null, null, null);
    }

    public List<Map<String, Object>> findAll(ModuleDefinition module, String query,
            String fromDate, String toDate, String type) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(module.getTable());
        List<Object> parameters = new ArrayList<>();
        List<String> where = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            StringJoiner filters = new StringJoiner(" OR ", "(", ")");
            for (FieldDefinition field : module.getFields()) {
                filters.add("CAST(" + field.getName() + " AS TEXT) LIKE ?");
                parameters.add("%" + query.trim() + "%");
            }
            where.add(filters.toString());
        }
        if (hasField(module, "fecha_inicial") && hasField(module, "fecha_final")) {
            if (fromDate != null && !fromDate.isBlank()) {
                where.add("fecha_final >= ?");
                parameters.add(fromDate.trim());
            }
            if (toDate != null && !toDate.isBlank()) {
                where.add("fecha_inicial <= ?");
                parameters.add(toDate.trim());
            }
        }
        if (hasField(module, "tipo_reporte") && type != null && !type.isBlank()) {
            where.add("LOWER(tipo_reporte) = LOWER(?)");
            parameters.add(type.trim());
        }
        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }
        sql.append(" ORDER BY ");
        if (hasField(module, "fecha_inicial")) {
            sql.append("fecha_inicial DESC, ");
        }
        sql.append(module.getIdColumn()).append(" DESC");

        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            var metadata = rs.getMetaData();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                Object value = rs.getObject(index);
                if (!(value instanceof byte[])) {
                    row.put(metadata.getColumnLabel(index), value);
                }
            }
            return row;
        }, parameters.toArray());
    }

    public Map<String, Object> findById(ModuleDefinition module, Object id) {
        String sql = "SELECT * FROM " + module.getTable() + " WHERE " + module.getIdColumn() + " = ?";
        List<Map<String, Object>> rows = jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            var metadata = rs.getMetaData();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                Object value = rs.getObject(index);
                if (!(value instanceof byte[])) {
                    row.put(metadata.getColumnLabel(index), value);
                }
            }
            return row;
        }, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public Map<String, Object> create(ModuleDefinition module, Map<String, Object> values) {
        List<FieldDefinition> writable = writableFields(module, values, true);
        StringJoiner columns = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");
        List<Object> parameters = new ArrayList<>();
        for (FieldDefinition field : writable) {
            columns.add(field.getName());
            placeholders.add("?");
            parameters.add(clean(values.get(field.getName())));
        }

        String sql = "INSERT INTO " + module.getTable() + " (" + columns + ") VALUES (" + placeholders + ")";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < parameters.size(); index++) {
                statement.setObject(index + 1, parameters.get(index));
            }
            return statement;
        }, keyHolder);

        Object id = values.get(module.getIdColumn());
        if (module.isAutoId()) {
            Number key = keyHolder.getKey();
            if (key != null) {
                id = key;
            }
        }
        return findById(module, id);
    }

    public Map<String, Object> update(ModuleDefinition module, Object id, Map<String, Object> values) {
        List<FieldDefinition> writable = writableFields(module, values, false);
        StringJoiner assignments = new StringJoiner(", ");
        List<Object> parameters = new ArrayList<>();
        for (FieldDefinition field : writable) {
            assignments.add(field.getName() + " = ?");
            parameters.add(clean(values.get(field.getName())));
        }
        parameters.add(id);

        String sql = "UPDATE " + module.getTable() + " SET " + assignments
                + " WHERE " + module.getIdColumn() + " = ?";
        int updated = jdbc.update(sql, parameters.toArray());
        if (updated == 0) {
            return null;
        }
        return findById(module, id);
    }

    public boolean delete(ModuleDefinition module, Object id) {
        String sql = "DELETE FROM " + module.getTable() + " WHERE " + module.getIdColumn() + " = ?";
        return jdbc.update(sql, id) > 0;
    }

    private List<FieldDefinition> writableFields(
            ModuleDefinition module, Map<String, Object> values, boolean creating) {
        List<FieldDefinition> writable = new ArrayList<>();
        for (FieldDefinition field : module.getFields()) {
            boolean generatedId = creating && module.isAutoId()
                    && field.getName().equals(module.getIdColumn());
            boolean primaryKeyOnUpdate = !creating && field.getName().equals(module.getIdColumn());
            if (!generatedId && !primaryKeyOnUpdate && values.containsKey(field.getName())) {
                writable.add(field);
            }
        }
        return writable;
    }

    private boolean hasField(ModuleDefinition module, String name) {
        return module.getFields().stream().anyMatch(field -> name.equals(field.getName()));
    }

    private Object clean(Object value) {
        if (value instanceof String text && text.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}
