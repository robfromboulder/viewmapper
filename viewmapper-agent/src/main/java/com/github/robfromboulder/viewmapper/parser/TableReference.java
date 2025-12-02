// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import io.trino.sql.tree.QualifiedName;

import java.util.Objects;

/**
 * Represents a reference to a table or view in a SQL statement.
 * <p>
 * Contains the fully qualified name (catalog.schema.table) and additional metadata.
 */
public class TableReference {

    private final QualifiedName qualifiedName;
    private final String catalog;
    private final String schema;
    private final String table;

    /**
     * Creates a TableReference from a Trino QualifiedName.
     *
     * @param qualifiedName The qualified name from the Trino AST
     */
    public TableReference(QualifiedName qualifiedName) {
        this.qualifiedName = qualifiedName;
        var parts = qualifiedName.getParts();
        if (parts.size() == 1) {
            this.catalog = null;
            this.schema = null;
            this.table = parts.get(0);
        } else if (parts.size() == 2) {
            this.catalog = null;
            this.schema = parts.get(0);
            this.table = parts.get(1);
        } else if (parts.size() == 3) {
            this.catalog = parts.get(0);
            this.schema = parts.get(1);
            this.table = parts.get(2);
        } else {
            throw new IllegalArgumentException("Invalid qualified name: " + qualifiedName);
        }
    }

    /**
     * Creates a TableReference with explicit components.
     *
     * @param catalog The catalog name (can be null)
     * @param schema  The schema name (can be null)
     * @param table   The table name (required)
     */
    public TableReference(String catalog, String schema, String table) {
        Objects.requireNonNull(table, "Table name cannot be null");
        this.catalog = catalog;
        this.schema = schema;
        this.table = table;
        if (catalog != null && schema != null) {
            this.qualifiedName = QualifiedName.of(catalog, schema, table);
        } else if (schema != null) {
            this.qualifiedName = QualifiedName.of(schema, table);
        } else {
            this.qualifiedName = QualifiedName.of(table);
        }
    }

    public String getCatalog() {
        return catalog;
    }

    public String getSchema() {
        return schema;
    }

    public String getTable() {
        return table;
    }

    public String getFullyQualifiedName() {
        return qualifiedName.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableReference that = (TableReference) o;
        return Objects.equals(qualifiedName, that.qualifiedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiedName);
    }

    @Override
    public String toString() {
        return qualifiedName.toString();
    }

}