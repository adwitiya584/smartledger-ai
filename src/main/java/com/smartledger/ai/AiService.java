package com.smartledger.ai;

import com.google.gson.*;
import com.smartledger.transaction.TransactionRepository;
import com.smartledger.transaction.TransactionType;
import com.smartledger.auth.UserRepository;
import com.smartledger.auth.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.util.*;

@Service
public class AiService {

    @Value("${app.groq.api-key}")
    private String groqApiKey;

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public AiService(TransactionRepository transactionRepository,
                     UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public String chat(String email, String userMessage) {
        User user = getUser(email);

        BigDecimal income = transactionRepository
                .sumByUserIdAndType(user.getId(), TransactionType.INCOME);
        BigDecimal expense = transactionRepository
                .sumByUserIdAndType(user.getId(), TransactionType.EXPENSE);
        BigDecimal balance = income.subtract(expense);

        List<Object[]> categoryData = transactionRepository
                .sumExpensesByCategory(user.getId());

        StringBuilder categoryInfo = new StringBuilder();
        for (Object[] row : categoryData) {
            categoryInfo.append(row[0]).append(": ₹").append(row[1]).append(", ");
        }

        String context = String.format(
            "You are SmartLedger AI, a personal finance advisor. " +
            "User financial summary - Total Income: ₹%s, Total Expenses: ₹%s, " +
            "Balance: ₹%s, Spending by category: %s. " +
            "Give concise, practical advice in 2-3 sentences.",
            income, expense, balance, categoryInfo
        );

        return callGroq(context, userMessage);
    }

    private String callGroq(String context, String userMessage) {
        try {
            String url = "https://api.groq.com/openai/v1/chat/completions";

            String requestBody = String.format("""
                {
                    "model": "llama-3.3-70b-versatile",
                    "messages": [
                        {"role": "system", "content": "%s"},
                        {"role": "user", "content": "%s"}
                    ],
                    "max_tokens": 200
                }
                """,
                context.replace("\"", "'").replace("\n", " "),
                userMessage.replace("\"", "'")
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("Groq Status: " + response.statusCode());
            System.out.println("Groq Response: " + response.body());

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (json.has("error")) {
                return "AI Error: " + json.getAsJsonObject("error")
                    .get("message").getAsString();
            }

            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Sorry, couldn't process request: " + e.getMessage();
        }
    }
}