package com.example.mehrdad.querymind.dto;

import lombok.Data;

@Data
public class QueryRequest {
    private String text;
    private String databaseSchema;
}

