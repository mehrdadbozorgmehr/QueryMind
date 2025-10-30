package com.example.mehrdad.querymind.service;

import com.example.mehrdad.querymind.dto.QueryResponse;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIQueryService {

    @Value("${llm.provider:openai}")
    private String llmProvider;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    private final DatabaseSchemaService databaseSchemaService;
    private final ChatModel vertexAiGeminiChatModel;

    public AIQueryService(DatabaseSchemaService databaseSchemaService,
                         @org.springframework.beans.factory.annotation.Autowired(required = false) ChatModel vertexAiGeminiChatModel) {
        this.databaseSchemaService = databaseSchemaService;
        this.vertexAiGeminiChatModel = vertexAiGeminiChatModel;
    }

    public QueryResponse convertTextToQuery(String text, String databaseSchema) {
        try {
            String effectiveSchema = databaseSchema;
            if (effectiveSchema == null || effectiveSchema.trim().isEmpty()) {
                // Auto-detect current DB schema
                effectiveSchema = databaseSchemaService.getSchemaAsString();
            }

            // Check which provider to use based on feature flag
            if ("gemini".equalsIgnoreCase(llmProvider)) {
                return convertTextToQueryWithGemini(text, effectiveSchema);
            } else {
                return convertTextToQueryWithOpenAI(text, effectiveSchema);
            }

        } catch (Exception e) {
            String fallback = generateBasicQuery(text, databaseSchema);
            return new QueryResponse(
                    fallback,
                    "Fallback to heuristic generation due to error: " + e.getMessage(),
                    true,
                    null
            );
        }
    }

    private QueryResponse convertTextToQueryWithOpenAI(String text, String effectiveSchema) {
        try {
            if (openaiApiKey == null || openaiApiKey.isEmpty()) {
                String sql = generateBasicQuery(text, effectiveSchema);
                return new QueryResponse(
                    sql,
                    "Generated using heuristic pattern matching (OpenAI API key not configured)",
                    true,
                    null
                );
            }

            OpenAiService service = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));

            String systemPrompt = "You are an expert SQL assistant. Convert natural language queries to optimized, syntactically correct SQL for the provided relational schema. " +
                    "Schema format: TABLE (column TYPE [annotations]) where annotations can indicate PK primary keys and FK foreign key relationships. " +
                    "When multiple tables are referenced, infer JOINs using foreign key relationships. Prefer explicit JOIN syntax. " +
                    "Return ONLY the SQL query (single statement) ending with a semicolon. Do not include backticks, markdown, or explanations.";

            String userPrompt = buildUserPrompt(text, effectiveSchema);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", systemPrompt));
            messages.add(new ChatMessage("user", userPrompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(messages)
                    .temperature(0.2)
                    .maxTokens(400)
                    .build();

            String sqlQuery = service.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent().trim();

            sqlQuery = cleanSqlQuery(sqlQuery);

            return new QueryResponse(
                    sqlQuery,
                    "Generated using OpenAI GPT-3.5 with auto-detected schema",
                    true,
                    null
            );

        } catch (Exception e) {
            throw new RuntimeException("OpenAI API error: " + e.getMessage(), e);
        }
    }

    private QueryResponse convertTextToQueryWithGemini(String text, String effectiveSchema) {
        try {
            if (vertexAiGeminiChatModel == null) {
                String sql = generateBasicQuery(text, effectiveSchema);
                return new QueryResponse(
                    sql,
                    "Generated using heuristic pattern matching (Gemini is not configured)",
                    true,
                    null
                );
            }

            String systemPrompt = "You are an expert SQL assistant. Convert natural language queries to optimized, syntactically correct SQL for the provided relational schema. " +
                    "Schema format: TABLE (column TYPE [annotations]) where annotations can indicate PK primary keys and FK foreign key relationships. " +
                    "When multiple tables are referenced, infer JOINs using foreign key relationships. Prefer explicit JOIN syntax. " +
                    "Return ONLY the SQL query (single statement) ending with a semicolon. Do not include backticks, markdown, or explanations.";

            String userPrompt = buildUserPrompt(text, effectiveSchema);
            String fullPrompt = systemPrompt + "\n\n" + userPrompt;

            // Use Spring AI Vertex AI Gemini
            String sqlQuery = vertexAiGeminiChatModel.call(fullPrompt);
            sqlQuery = cleanSqlQuery(sqlQuery);

            return new QueryResponse(
                    sqlQuery,
                    "Generated using Google Gemini 1.5 Flash with auto-detected schema",
                    true,
                    null
            );

        } catch (Exception e) {
            throw new RuntimeException("Gemini API error: " + e.getMessage(), e);
        }
    }


    private String buildUserPrompt(String naturalLanguage, String schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("Database Schema:\n").append(schema).append("\n\n");
        sb.append("User Request: ").append(naturalLanguage).append("\n\n");
        sb.append("Constraints: Only one SQL statement; choose only relevant columns; include necessary JOINs and filters; use table aliases; if aggregation requested include GROUP BY; if counting return COUNT with meaningful alias.");
        return sb.toString();
    }

    private String cleanSqlQuery(String sql) {
        sql = sql.replaceAll("```sql\\n?", "").replaceAll("```\\n?", "");
        sql = sql.trim();
        if (!sql.endsWith(";")) {
            sql += ";";
        }
        return sql;
    }

    // Enhanced basic generator with simple join inference using schema
    private String generateBasicQuery(String text, String schemaString) {
        if (text == null || text.trim().isEmpty()) return "SELECT 1;";
        String lowered = text.toLowerCase(Locale.ROOT);

        // Parse table names from schema
        Set<String> tables = parseTables(schemaString);
        List<String> mentionedTables = new ArrayList<>();
        for (String t : tables) {
            if (lowered.contains(t.toLowerCase(Locale.ROOT))) {
                mentionedTables.add(t);
            }
        }

        // If user asks for count
        boolean wantsCount = lowered.contains("count") || lowered.startsWith("how many") || lowered.contains("number of");

        // Build simple queries
        if (mentionedTables.size() == 1) {
            String table = mentionedTables.get(0);
            if (wantsCount) {
                return "SELECT COUNT(*) AS cnt FROM " + table + ";";
            }
            return "SELECT * FROM " + table + ";";
        }

        if (mentionedTables.size() >= 2) {
            // Attempt to build joins using FK annotations from schema string
            String joinSql = buildJoinQuery(mentionedTables, schemaString, wantsCount);
            if (joinSql != null) return joinSql;
        }

        // Generic fallbacks
        if (wantsCount) return "SELECT COUNT(*) FROM " + (mentionedTables.isEmpty() ? "table_name" : mentionedTables.get(0)) + ";";
        return "SELECT * FROM " + (mentionedTables.isEmpty() ? "table_name" : mentionedTables.get(0)) + ";";
    }

    private Set<String> parseTables(String schemaString) {
        Set<String> tables = new LinkedHashSet<>();
        if (schemaString == null) return tables;
        Pattern p = Pattern.compile("^(\\w+) \\(", Pattern.MULTILINE);
        Matcher m = p.matcher(schemaString);
        while (m.find()) {
            tables.add(m.group(1));
        }
        return tables;
    }

    private String buildJoinQuery(List<String> tables, String schemaString, boolean wantsCount) {
        // Extract FK lines: column TYPE [PK, FK->OtherTable.otherColumn]
        Map<String, Map<String, String>> fkMap = new HashMap<>(); // table -> column -> targetTable.targetColumn
        Pattern tablePattern = Pattern.compile("^(\\w+) \\((.*?)\\)", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher tableMatcher = tablePattern.matcher(schemaString);
        while (tableMatcher.find()) {
            String tableName = tableMatcher.group(1);
            String body = tableMatcher.group(2);
            Map<String, String> colToFk = new HashMap<>();
            for (String line : body.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // line format: column TYPE [annotations], maybe with trailing comma removed
                line = line.replaceAll(",$", "");
                int bracketIdx = line.indexOf('[');
                if (bracketIdx > -1) {
                    String before = line.substring(0, bracketIdx).trim();
                    String annotations = line.substring(bracketIdx + 1, line.lastIndexOf(']'));
                    String colName = before.split(" ")[0];
                    for (String ann : annotations.split("\\s*,\\s*")) {
                        if (ann.startsWith("FK->")) {
                            String target = ann.substring(4); // may contain multiple separated by |
                            String[] pieces = target.split("\\|");
                            // take first for simplicity
                            if (pieces.length > 0) {
                                colToFk.put(colName, pieces[0]);
                            }
                        }
                    }
                }
            }
            if (!colToFk.isEmpty()) fkMap.put(tableName, colToFk);
        }

        // Try to find a chain connecting first two tables
        String base = tables.get(0);
        String second = tables.get(1);
        String joinCondition = null;

        // Search FK from base to second
        Map<String, String> baseFks = fkMap.getOrDefault(base, Collections.emptyMap());
        for (Map.Entry<String, String> e : baseFks.entrySet()) {
            if (e.getValue().startsWith(second + ".")) {
                joinCondition = base + "." + e.getKey() + " = " + e.getValue();
                break;
            }
        }
        // Or FK from second to base
        if (joinCondition == null) {
            Map<String, String> secondFks = fkMap.getOrDefault(second, Collections.emptyMap());
            for (Map.Entry<String, String> e : secondFks.entrySet()) {
                if (e.getValue().startsWith(base + ".")) {
                    joinCondition = second + "." + e.getKey() + " = " + e.getValue();
                    break;
                }
            }
        }
        if (joinCondition == null) return null;

        String selectPart = wantsCount ? "COUNT(*) AS cnt" : base + ".* , " + second + ".*";
        return "SELECT " + selectPart + " FROM " + base + " JOIN " + second + " ON " + joinCondition + ";";
    }
}
