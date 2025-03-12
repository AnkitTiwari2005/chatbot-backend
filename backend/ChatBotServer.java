package backend;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

public class ChatBotServer {
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Serve static files (Frontend)
        server.createContext("/", new StaticFileHandler());

        // Chat API
        server.createContext("/chat", new ChatHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("✅ Server started on http://localhost:8080/");
    }

    // Handles serving frontend files
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String filePath = "frontend" + (exchange.getRequestURI().getPath().equals("/") ? "/index.html" : exchange.getRequestURI().getPath());

            File file = new File(filePath);
            if (!file.exists()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody(); FileInputStream fs = new FileInputStream(file)) {
                fs.transferTo(os);
            }
        }
    }

    static class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    StringBuilder requestBody = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        requestBody.append(line);
                    }

                    JSONObject requestJson = new JSONObject(requestBody.toString());
                    String userMessage = requestJson.getString("message");

                    String botResponse = getBotResponse(userMessage);

                    JSONObject responseJson = new JSONObject();
                    responseJson.put("response", botResponse);

                    String responseText = responseJson.toString();
                    exchange.sendResponseHeaders(200, responseText.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseText.getBytes());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private String getBotResponse(String message) throws IOException {
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return "❌ Error: API key is missing!";
            }

            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject requestBody = new JSONObject()
                    .put("model", "gpt-3.5-turbo")
                    .put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", message)));

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                }

                if (conn.getResponseCode() != 200) {
                    return "❌ Error: API returned code " + conn.getResponseCode();
                }

                String responseString = readStream(conn.getInputStream());
                JSONObject responseJson = new JSONObject(responseString);
                return responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            } catch (Exception e) {
                e.printStackTrace();
                return "❌ Error: Failed to fetch response from API.";
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        private String readStream(InputStream stream) throws IOException {
            if (stream == null) return "No response from server.";
            BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }
}
