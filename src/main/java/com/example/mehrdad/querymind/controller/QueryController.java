package com.example.mehrdad.querymind.controller;

import com.example.mehrdad.querymind.dto.DatabaseSchemaResponse;
import com.example.mehrdad.querymind.dto.QueryRequest;
import com.example.mehrdad.querymind.dto.QueryResponse;
import com.example.mehrdad.querymind.dto.QueryResponseWithData;
import com.example.mehrdad.querymind.service.AIQueryService;
import com.example.mehrdad.querymind.service.DatabaseSchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QueryController {

    private final AIQueryService aiQueryService;
    private final DatabaseSchemaService databaseSchemaService;

    @PostMapping("/convert")
    public ResponseEntity<QueryResponse> convertTextToQuery(@RequestBody QueryRequest request) {
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                new QueryResponse(null, null, false, "Text cannot be empty")
            );
        }

        QueryResponse response = aiQueryService.convertTextToQuery(
            request.getText(),
            request.getDatabaseSchema()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/convert-and-execute")
    public ResponseEntity<QueryResponseWithData> convertAndExecute(@RequestBody QueryRequest request) {
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                new QueryResponseWithData(null, null, false, "Text cannot be empty", null, 0, false)
            );
        }

        // Use auto-detected schema if none provided
        String schema = request.getDatabaseSchema();
        if (schema == null || schema.trim().isEmpty()) {
            schema = databaseSchemaService.getSchemaAsString();
        }

        // Generate SQL query
        QueryResponse queryResponse = aiQueryService.convertTextToQuery(request.getText(), schema);

        if (!queryResponse.isSuccess()) {
            return ResponseEntity.ok(new QueryResponseWithData(
                queryResponse.getSqlQuery(),
                queryResponse.getExplanation(),
                false,
                queryResponse.getError(),
                null,
                0,
                false
            ));
        }

        // Execute the query
        DatabaseSchemaService.QueryExecutionResult executionResult =
            databaseSchemaService.executeQuery(queryResponse.getSqlQuery());

        QueryResponseWithData response = new QueryResponseWithData();
        response.setSqlQuery(queryResponse.getSqlQuery());
        response.setExplanation(queryResponse.getExplanation());
        response.setSuccess(executionResult.isSuccess());
        response.setError(executionResult.getError());
        response.setData(executionResult.getRows());
        response.setRowCount(executionResult.getRowCount());
        response.setExecuted(true);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/schema")
    public ResponseEntity<DatabaseSchemaResponse> getDatabaseSchema() {
        Map<String, java.util.List<DatabaseSchemaService.ColumnInfo>> tables =
            databaseSchemaService.getAllTables();

        DatabaseSchemaResponse response = new DatabaseSchemaResponse();
        response.setTables(tables);
        response.setTableCount(tables.size());
        response.setSchemaString(databaseSchemaService.getSchemaAsString());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("QueryMind AI is running!");
    }
}
