package com.example.mehrdad.querymind.controller;

import com.example.mehrdad.querymind.dto.QueryRequest;
import com.example.mehrdad.querymind.dto.QueryResponse;
import com.example.mehrdad.querymind.service.AIQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QueryController {

    private final AIQueryService aiQueryService;

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

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("QueryMind AI is running!");
    }
}

