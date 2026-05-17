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

            // Clean inputs — remove newlines and escape quotes
            String cleanContext = context
                .replace("\\", "\\\\")
                .replace("\"", "'")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();

            String cleanMessage = userMessage
                .replace("\\", "\\\\")
                .replace("\"", "'")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();

            String requestBody = "{" +
                "\"model\": \"llama-3.3-70b-versatile\"," +
                "\"messages\": [" +
                    "{\"role\": \"system\", \"content\": \"" + cleanContext + "\"}," +
                    "{\"role\": \"user\", \"content\": \"" + cleanMessage + "\"}" +
                "]," +
                "\"max_tokens\": 1000" +
            "}";

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
    public Map<String, Object> detectAnomalies(String email) {
        User user = getUser(email);

        // Get all transactions
        List<com.smartledger.transaction.Transaction> transactions =
            transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId());

        if (transactions.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("anomalies", new ArrayList<>());
            empty.put("summary", "No transactions found to analyze.");
            return empty;
        }

        // Build transaction summary for AI
        StringBuilder txSummary = new StringBuilder();
        Map<String, Double> categoryTotals = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (com.smartledger.transaction.Transaction t : transactions) {
            if (t.getType() == com.smartledger.transaction.TransactionType.EXPENSE) {
                String cat = t.getCategory() != null ? t.getCategory() : "Other";
                categoryTotals.merge(cat, t.getAmount().doubleValue(), Double::sum);
                categoryCounts.merge(cat, 1, Integer::sum);
            }
        }

        txSummary.append("Transaction analysis:\n");
        categoryTotals.forEach((cat, total) -> {
            txSummary.append(String.format("Category: %s, Total: %.2f, Count: %d. ",
                cat, total, categoryCounts.get(cat)));
        });

        BigDecimal totalIncome = transactionRepository
            .sumByUserIdAndType(user.getId(),
                com.smartledger.transaction.TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository
            .sumByUserIdAndType(user.getId(),
                com.smartledger.transaction.TransactionType.EXPENSE);

        txSummary.append(String.format(" Total Income: %.2f, Total Expense: %.2f",
        	    totalIncome.doubleValue(), totalExpense.doubleValue()));

        String prompt = "Analyze this spending data and identify unusual patterns, overspending, or financial risks. " +
        	    "Return ONLY a JSON array, no markdown, no explanation: " +
        	    "[{\"type\":\"WARNING\",\"title\":\"short title\",\"description\":\"explanation\"," +
        	    "\"category\":\"category name\",\"suggestion\":\"actionable suggestion\"}]. " +
        	    "Find 3-5 anomalies. Data: " + txSummary.toString().replace("\n", " ");

        String response = callGroq("You are a financial anomaly detection AI that returns pure JSON only.", prompt);

        // Parse response
        try {
            // Clean response
            String cleaned = response.trim();
            if (cleaned.contains("```")) {
                cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
            }
            // Find JSON array
            int start = cleaned.indexOf('[');
            int end = cleaned.lastIndexOf(']') + 1;
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end);
            }

            com.google.gson.JsonArray arr = com.google.gson.JsonParser
                .parseString(cleaned).getAsJsonArray();
            List<Map<String, String>> anomalies = new ArrayList<>();
            for (com.google.gson.JsonElement el : arr) {
                com.google.gson.JsonObject obj = el.getAsJsonObject();
                Map<String, String> anomaly = new HashMap<>();
                anomaly.put("type", obj.has("type") ? obj.get("type").getAsString() : "INFO");
                anomaly.put("title", obj.has("title") ? obj.get("title").getAsString() : "");
                anomaly.put("description", obj.has("description") ? obj.get("description").getAsString() : "");
                anomaly.put("category", obj.has("category") ? obj.get("category").getAsString() : "");
                anomaly.put("suggestion", obj.has("suggestion") ? obj.get("suggestion").getAsString() : "");
                anomalies.add(anomaly);
            }

            // Generate overall summary
            String summaryPrompt = String.format(
                "Based on: Income ₹%.2f, Expenses ₹%.2f, give a 2-sentence financial health summary.",
                totalIncome.doubleValue(), totalExpense.doubleValue()
            );
            String summary = callGroq("You are a financial advisor.", summaryPrompt);

            Map<String, Object> result = new HashMap<>();
            result.put("anomalies", anomalies);
            result.put("summary", summary);
            result.put("totalIncome", totalIncome);
            result.put("totalExpense", totalExpense);
            result.put("expenseRatio",
                totalIncome.doubleValue() > 0
                    ? (totalExpense.doubleValue() / totalIncome.doubleValue()) * 100
                    : 0);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("anomalies", new ArrayList<>());
            err.put("summary", "Could not analyze transactions. Please try again.");
            return err;
        }
    }

    public Map<String, Object> getSpendingPrediction(String email) {
        User user = getUser(email);

        BigDecimal totalIncome = transactionRepository
            .sumByUserIdAndType(user.getId(),
                com.smartledger.transaction.TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository
            .sumByUserIdAndType(user.getId(),
                com.smartledger.transaction.TransactionType.EXPENSE);

        List<Object[]> categoryData = transactionRepository
            .sumExpensesByCategory(user.getId());

        StringBuilder data = new StringBuilder();
        for (Object[] row : categoryData) {
            data.append(row[0]).append(": ₹").append(row[1]).append("\n");
        }

        String prompt = String.format(
            "Based on spending data: Income ₹%s, Expenses ₹%s, Categories: %s. " +
            "Return ONLY a JSON object (no markdown): " +
            "{\"nextMonthExpense\":number,\"savingsRate\":number,\"topRiskCategory\":\"string\"," +
            "\"prediction\":\"2 sentence prediction\",\"tips\":[\"tip1\",\"tip2\",\"tip3\"]}",
            totalIncome, totalExpense, data
        );

        String response = callGroq("You are a financial prediction AI. Return pure JSON only.", prompt);

        try {
            String cleaned = response.trim()
                .replaceAll("```json", "").replaceAll("```", "").trim();
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}') + 1;
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end);
            }

            com.google.gson.JsonObject obj = com.google.gson.JsonParser
                .parseString(cleaned).getAsJsonObject();

            Map<String, Object> result = new HashMap<>();
            result.put("nextMonthExpense",
                obj.has("nextMonthExpense") ? obj.get("nextMonthExpense").getAsDouble() : 0);
            result.put("savingsRate",
                obj.has("savingsRate") ? obj.get("savingsRate").getAsDouble() : 0);
            result.put("topRiskCategory",
                obj.has("topRiskCategory") ? obj.get("topRiskCategory").getAsString() : "");
            result.put("prediction",
                obj.has("prediction") ? obj.get("prediction").getAsString() : "");

            List<String> tips = new ArrayList<>();
            if (obj.has("tips")) {
                for (com.google.gson.JsonElement tip : obj.getAsJsonArray("tips")) {
                    tips.add(tip.getAsString());
                }
            }
            result.put("tips", tips);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("prediction", "Could not generate prediction. Please try again.");
            err.put("tips", new ArrayList<>());
            return err;
        }
    }
}