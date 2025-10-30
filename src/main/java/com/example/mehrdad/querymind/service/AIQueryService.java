package com.example.mehrdad.querymind.service;

import com.example.mehrdad.querymind.dto.QueryResponse;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class AIQueryService {

    @Value("${openai.api.key:}")
    private String apiKey;

    public QueryResponse convertTextToQuery(String text, String databaseSchema) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                return new QueryResponse(
                    generateBasicQuery(text),
                    "Generated using basic pattern matching (OpenAI API key not configured)",
                    true,
                    null
                );
            }

            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(30));

            String systemPrompt = "You are an expert SQL assistant. Convert natural language queries to SQL. " +
                    "Provide ONLY the SQL query without any explanation or formatting. " +
                    "If a database schema is provided, use it. Otherwise, use common table and column names.";

            String userPrompt = text;
            if (databaseSchema != null && !databaseSchema.isEmpty()) {
                userPrompt = "Database Schema:\n" + databaseSchema + "\n\nUser Query: " + text;
            }

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", systemPrompt));
            messages.add(new ChatMessage("user", userPrompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(messages)
                    .temperature(0.3)
                    .maxTokens(500)
                    .build();

            String sqlQuery = service.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent().trim();

            // Clean up the SQL query
            sqlQuery = cleanSqlQuery(sqlQuery);

            return new QueryResponse(
                    sqlQuery,
                    "Generated using OpenAI GPT-3.5",
                    true,
                    null
            );

        } catch (Exception e) {
            return new QueryResponse(
                    generateBasicQuery(text),
                    "Fallback to basic generation due to error: " + e.getMessage(),
                    true,
                    null
            );
        }
    }

    private String cleanSqlQuery(String sql) {
        // Remove markdown code blocks if present
        sql = sql.replaceAll("```sql\\n?", "").replaceAll("```\\n?", "");
        // Remove leading/trailing whitespace
        sql = sql.trim();
        // Ensure it ends with semicolon
        if (!sql.endsWith(";")) {
            sql += ";";
        }
        return sql;
    }

    private String generateBasicQuery(String text) {
        text = text.toLowerCase();

        if (text.contains("all") || text.contains("list") || text.contains("show")) {
            if (text.contains("user")) {
                return "SELECT * FROM users;";
            } else if (text.contains("product")) {
                return "SELECT * FROM products;";
            } else if (text.contains("order")) {
                return "SELECT * FROM orders;";
            } else if (text.contains("customer")) {
                return "SELECT * FROM customers;";
            }
        }

        if (text.contains("count")) {
            if (text.contains("user")) {
                return "SELECT COUNT(*) FROM users;";
            } else if (text.contains("product")) {
                return "SELECT COUNT(*) FROM products;";
            } else if (text.contains("order")) {
                return "SELECT COUNT(*) FROM orders;";
            }
        }

        if (text.contains("where") || text.contains("with")) {
            return "SELECT * FROM table_name WHERE condition = 'value';";
        }

        return "SELECT * FROM table_name;";
    }
}

