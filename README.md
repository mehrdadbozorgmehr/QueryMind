# QueryMind - AI-Powered Text to SQL Converter

## Overview
QueryMind2 is a Spring Boot application that converts natural language text into SQL queries using AI. It makes database querying accessible to everyone, even without SQL knowledge!

## Features
- 🤖 AI-powered natural language to SQL conversion
- 🎨 Beautiful and intuitive web interface
- 📝 Optional database schema input for better accuracy
- 💡 Example queries to get started quickly
- 📋 One-click copy to clipboard
- ⚡ Fallback to pattern matching if AI is unavailable

## Technologies Used
- Spring Boot 3.5.7
- OpenAI GPT-3.5 Turbo
- H2 Database (for testing)
- Lombok
- REST API

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven
- OpenAI API Key (optional, but recommended)

### Installation

1. **Get OpenAI API Key** (Optional but recommended)
   - Go to https://platform.openai.com/api-keys
   - Create a new API key
   - Copy the key

2. **Configure the Application**
   - Open `src/main/resources/application.properties`
   - Replace `your-openai-api-key-here` with your actual OpenAI API key
   - If you don't have an API key, the app will use basic pattern matching

3. **Build the Project**
   ```bash
   mvnw clean install
   ```

4. **Run the Application**
   ```bash
   mvnw spring-boot:run
   ```

5. **Access the Application**
   - Open your browser and go to: http://localhost:8080
   - The beautiful UI will be displayed!

## How to Use

### Via Web Interface (Easiest!)
1. Open http://localhost:8080 in your browser
2. Type your question in natural language (e.g., "Show all users")
3. Optionally, provide your database schema for better accuracy
4. Click "Convert to SQL"
5. Copy the generated SQL query!

### Via API (For developers)

**Endpoint:** `POST /api/query/convert`

**Request Body:**
```json
{
  "text": "Show all users who registered in the last 30 days",
  "databaseSchema": "users (id, name, email, created_at)"
}
```

**Response:**
```json
{
  "sqlQuery": "SELECT * FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);",
  "explanation": "Generated using OpenAI GPT-3.5",
  "success": true,
  "error": null
}
```

**Example using curl:**
```bash
curl -X POST http://localhost:8080/api/query/convert ^
  -H "Content-Type: application/json" ^
  -d "{\"text\":\"Show all users\",\"databaseSchema\":\"\"}"
```

## Example Queries

Try these natural language queries:
- "Show all users"
- "Get the total number of orders"
- "Find all products with price greater than 100"
- "List customers who made purchases in the last week"
- "Show top 10 products by sales"

## Project Structure

```
src/
├── main/
│   ├── java/com/example/mehrdad/querymind2/
│   │   ├── QueryMind2Application.java       # Main application
│   │   ├── controller/
│   │   │   └── QueryController.java         # REST API endpoints
│   │   ├── service/
│   │   │   └── AIQueryService.java          # AI query generation logic
│   │   └── dto/
│   │       ├── QueryRequest.java            # Request DTO
│   │       └── QueryResponse.java           # Response DTO
│   └── resources/
│       ├── application.properties           # Configuration
│       └── static/
│           └── index.html                   # Web UI
```

## Configuration Options

Edit `application.properties` to customize:

```properties
# OpenAI API Key
openai.api.key=your-key-here

# Server Port
server.port=8080

# H2 Database (for testing)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## Troubleshooting

### Issue: "OpenAI API key not configured"
- **Solution:** Add your OpenAI API key to `application.properties`. The app will still work with basic pattern matching.

### Issue: Application won't start
- **Solution:** Make sure Java 17+ is installed and `JAVA_HOME` is set correctly.

### Issue: Port 8080 is already in use
- **Solution:** Change the port in `application.properties` to another port like 8081.

## Features Explained

### AI-Powered Conversion
When you have an OpenAI API key configured, the app uses GPT-3.5 Turbo to understand your natural language and generate accurate SQL queries.

### Fallback Pattern Matching
If the OpenAI API is not available, the app uses intelligent pattern matching to generate basic SQL queries.

### Database Schema Support
Provide your database schema for more accurate queries. The AI will use the actual table and column names from your schema.

## Future Enhancements
- Support for multiple AI models
- Query execution and result display
- Query history
- Support for multiple databases (PostgreSQL, MySQL, etc.)
- Query optimization suggestions

## License
This project is open source and available for educational purposes.

## Support
For issues or questions, please create an issue in the repository.

---
Made with ❤️ using Spring Boot and OpenAI

