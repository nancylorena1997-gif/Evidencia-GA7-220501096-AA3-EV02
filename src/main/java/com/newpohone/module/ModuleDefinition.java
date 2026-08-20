package com.newpohone.module;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ModuleDefinition {

    private final String key;
    private final String title;
    private final String singular;
    private final String group;
    private final String table;
    private final String idColumn;
    private final boolean autoId;
    private final String icon;
    private final List<FieldDefinition> fields;

    public ModuleDefinition(String key, String title, String singular, String group, String table,
            String idColumn, boolean autoId, String icon, FieldDefinition... fields) {
        this.key = key;
        this.title = title;
        this.singular = singular;
        this.group = group;
        this.table = table;
        this.idColumn = idColumn;
        this.autoId = autoId;
        this.icon = icon;
        this.fields = Collections.unmodifiableList(Arrays.asList(fields));
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getSingular() {
        return singular;
    }

    public String getGroup() {
        return group;
    }

    public String getTable() {
        return table;
    }

    public String getIdColumn() {
        return idColumn;
    }

    public boolean isAutoId() {
        return autoId;
    }

    public String getIcon() {
        return icon;
    }

    public List<FieldDefinition> getFields() {
        return fields;
    }

    public static final class FieldDefinition {

        private final String name;
        private final String label;
        private final String type;
        private final boolean required;
        private final boolean readOnly;

        public FieldDefinition(String name, String label, String type, boolean required, boolean readOnly) {
            this.name = name;
            this.label = label;
            this.type = type;
            this.required = required;
            this.readOnly = readOnly;
        }

        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }

        public String getType() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }

        public boolean isReadOnly() {
            return readOnly;
        }
    }
}
