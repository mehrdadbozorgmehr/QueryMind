package com.example.mehrdad.querymind.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DatabaseSchemaService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Get all tables in the database with their columns
     */
    public Map<String, List<ColumnInfo>> getAllTables() {
        Map<String, List<ColumnInfo>> schema = new LinkedHashMap<>();

        try {
            DatabaseMetaData metaData = dataSource.getConnection().getMetaData();

            // Get all tables
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                List<ColumnInfo> columns = new ArrayList<>();

                // Get columns for this table
                ResultSet columnsRs = metaData.getColumns(null, null, tableName, "%");
                while (columnsRs.next()) {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setName(columnsRs.getString("COLUMN_NAME"));
                    columnInfo.setType(columnsRs.getString("TYPE_NAME"));
                    columnInfo.setSize(columnsRs.getInt("COLUMN_SIZE"));
                    columnInfo.setNullable(columnsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    columns.add(columnInfo);
                }
                columnsRs.close();

                if (!columns.isEmpty()) {
                    schema.put(tableName, columns);
                }
            }
            tables.close();

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve database schema: " + e.getMessage(), e);
        }

        return schema;
    }

    /**
     * Get schema as a formatted string for AI
     */
    public String getSchemaAsString() {
        Map<String, List<ColumnInfo>> schema = getAllTables();
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<ColumnInfo>> entry : schema.entrySet()) {
            sb.append(entry.getKey()).append(" (");

            List<String> columnDefs = new ArrayList<>();
            for (ColumnInfo col : entry.getValue()) {
                columnDefs.add(col.getName() + " " + col.getType());
            }

            sb.append(String.join(", ", columnDefs));
            sb.append(")\n");
        }

        return sb.toString().trim();
    }

    /**
     * Execute a SELECT query and return results
     */
    public QueryExecutionResult executeQuery(String sqlQuery) {
        QueryExecutionResult result = new QueryExecutionResult();

        try {
            // Only allow SELECT queries for safety
            String trimmedQuery = sqlQuery.trim().toUpperCase();
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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }
    }

    public static class QueryExecutionResult {
        private boolean success;
        private List<Map<String, Object>> rows;
        private int rowCount;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }

        public void setRows(List<Map<String, Object>> rows) {
            this.rows = rows;
        }

        public int getRowCount() {
            return rowCount;
        }

        public void setRowCount(int rowCount) {
            this.rowCount = rowCount;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}

