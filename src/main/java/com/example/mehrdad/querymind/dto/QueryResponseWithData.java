package com.example.mehrdad.querymind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryResponseWithData {
    private String sqlQuery;
    private String explanation;
    private boolean success;
    private String error;
    private List<Map<String, Object>> data;
    private int rowCount;
    private boolean executed;
}

