package com.example.mehrdad.querymind.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DatabaseSchemaService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Get all tables in the database with their columns enriched with PK/FK information
     */
    public Map<String, List<ColumnInfo>> getAllTables() {
        Map<String, List<ColumnInfo>> schema = new LinkedHashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Get all tables
            try (ResultSet tables = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    List<ColumnInfo> columns = new ArrayList<>();

                    // Collect primary keys for this table
                    Set<String> primaryKeys = new HashSet<>();
                    try (ResultSet pkRs = metaData.getPrimaryKeys(conn.getCatalog(), null, tableName)) {
                        while (pkRs.next()) {
                            primaryKeys.add(pkRs.getString("COLUMN_NAME"));
                        }
                    }

                    // Collect foreign keys (imported keys) for this table
                    Map<String, List<String>> fkTargetsByColumn = new HashMap<>();
                    try (ResultSet fkRs = metaData.getImportedKeys(conn.getCatalog(), null, tableName)) {
                        while (fkRs.next()) {
                            String fkColumn = fkRs.getString("FKCOLUMN_NAME");
                            String pkTable = fkRs.getString("PKTABLE_NAME");
                            String pkColumn = fkRs.getString("PKCOLUMN_NAME");
                            fkTargetsByColumn.computeIfAbsent(fkColumn, c -> new ArrayList<>())
                                    .add(pkTable + "." + pkColumn);
                        }
                    }

                    // Get columns for this table
                    try (ResultSet columnsRs = metaData.getColumns(conn.getCatalog(), null, tableName, "%")) {
                        while (columnsRs.next()) {
                            ColumnInfo columnInfo = new ColumnInfo();
                            columnInfo.setName(columnsRs.getString("COLUMN_NAME"));
                            columnInfo.setType(columnsRs.getString("TYPE_NAME"));
                            columnInfo.setSize(columnsRs.getInt("COLUMN_SIZE"));
                            columnInfo.setNullable(columnsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                            columnInfo.setPrimaryKey(primaryKeys.contains(columnInfo.getName()));
                            if (fkTargetsByColumn.containsKey(columnInfo.getName())) {
                                columnInfo.setForeignKeyTargets(fkTargetsByColumn.get(columnInfo.getName()));
                            }
                            columns.add(columnInfo);
                        }
                    }

                    if (!columns.isEmpty()) {
                        schema.put(tableName, columns);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve database schema: " + e.getMessage(), e);
        }

        return schema;
    }

    /**
     * Get schema as a formatted string for AI including PK and FK annotations
     */
    public String getSchemaAsString() {
        Map<String, List<ColumnInfo>> schema = getAllTables();
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<ColumnInfo>> entry : schema.entrySet()) {
            sb.append(entry.getKey()).append(" (\n");
            for (int i = 0; i < entry.getValue().size(); i++) {
                ColumnInfo col = entry.getValue().get(i);
                sb.append("  ").append(col.getName()).append(" ").append(col.getType());
                List<String> annotations = new ArrayList<>();
                if (col.isPrimaryKey()) annotations.add("PK");
                if (col.getForeignKeyTargets() != null && !col.getForeignKeyTargets().isEmpty()) {
                    annotations.add("FK->" + String.join("|", col.getForeignKeyTargets()));
                }
                if (!annotations.isEmpty()) {
                    sb.append(" [").append(String.join(", ", annotations)).append("]");
                }
                if (i < entry.getValue().size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(")\n");
        }

        return sb.toString().trim();
    }

    /**
     * Execute a SELECT query and return results (read-only safety)
     */
    public QueryExecutionResult executeQuery(String sqlQuery) {
        QueryExecutionResult result = new QueryExecutionResult();

        try {
            String trimmedQuery = sqlQuery.trim().toUpperCase(Locale.ROOT);
            if (!trimmedQuery.startsWith("SELECT")) {
                result.setSuccess(false);
                result.setError("Only SELECT queries are allowed for execution");
                return result;
            }

            List<Map<String, Object>> rows = jdbcTemplate.query(sqlQuery, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                return row;
            });

            result.setSuccess(true);
            result.setRows(rows);
            result.setRowCount(rows.size());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("Query execution failed: " + e.getMessage());
        }

        return result;
    }

    public static class ColumnInfo {
        private String name;
        private String type;
        private int size;
        private boolean nullable;
        private boolean primaryKey;
        private List<String> foreignKeyTargets; // Each entry: targetTable.targetColumn

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
        public boolean isPrimaryKey() { return primaryKey; }
        public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }
        public List<String> getForeignKeyTargets() { return foreignKeyTargets; }
        public void setForeignKeyTargets(List<String> foreignKeyTargets) { this.foreignKeyTargets = foreignKeyTargets; }
    }

    public static class QueryExecutionResult {
        private boolean success;
        private List<Map<String, Object>> rows;
        private int rowCount;
        private String error;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public List<Map<String, Object>> getRows() { return rows; }
        public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
        public int getRowCount() { return rowCount; }
        public void setRowCount(int rowCount) { this.rowCount = rowCount; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}

