package com.example.mehrdad.querymind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryResponse {
    private String sqlQuery;
    private String explanation;
    private boolean success;
    private String error;
}

