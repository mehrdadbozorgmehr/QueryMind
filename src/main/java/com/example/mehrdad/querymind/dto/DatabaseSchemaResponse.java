package com.example.mehrdad.querymind.dto;

import com.example.mehrdad.querymind.service.DatabaseSchemaService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseSchemaResponse {
    private Map<String, List<DatabaseSchemaService.ColumnInfo>> tables;
    private int tableCount;
    private String schemaString;
}

