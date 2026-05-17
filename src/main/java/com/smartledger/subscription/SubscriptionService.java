package com.smartledger.subscription;

import com.razorpay.*;
import com.smartledger.auth.User;
import com.smartledger.auth.UserRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class SubscriptionService {

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    @Value("${app.trial.hours:24}")
    private int trialHours;

    private final UserRepository userRepository;

    public SubscriptionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Check subscription status
    public Map<String, Object> getStatus(String email) {
        User user = getUser(email);
        Map<String, Object> status = new HashMap<>();

        String plan = user.getSubscriptionPlan();
        status.put("plan", plan);
        status.put("email", user.getEmail());
        status.put("name", user.getName());

        if ("FREE_TRIAL".equals(plan)) {
            if (user.getTrialStartDate() == null) {
                // Set trial start now
                user.setTrialStartDate(LocalDateTime.now());
                userRepository.save(user);
            }
            long hoursUsed = ChronoUnit.HOURS.between(
                user.getTrialStartDate(), LocalDateTime.now());
            long hoursLeft = Math.max(trialHours - hoursUsed, 0);
            boolean trialActive = hoursLeft > 0;

            status.put("trialActive", trialActive);
            status.put("hoursLeft", hoursLeft);
            status.put("trialStartDate", user.getTrialStartDate());
            status.put("hasAccess", trialActive);

            if (!trialActive) {
                status.put("message", "Your free trial has expired. Please upgrade to continue.");
            } else {
                status.put("message", "Free trial active — " + hoursLeft + " hours remaining");
            }
        } else if ("PRO".equals(plan) || "FAMILY".equals(plan)) {
            boolean active = user.getSubscriptionEndDate() == null ||
                             user.getSubscriptionEndDate().isAfter(LocalDateTime.now());
            status.put("hasAccess", active);
            status.put("subscriptionEndDate", user.getSubscriptionEndDate());
            status.put("trialActive", false);
            status.put("message", active ? plan + " Plan Active" : "Subscription expired");
        }

        return status;
    }

    // Create Razorpay order
    public Map<String, Object> createOrder(String email, String planType) throws Exception {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            int amount = 9900; // ₹99 only — PRO plan

            // Receipt max 40 chars
            String receipt = "rcpt_" + System.currentTimeMillis() % 1000000000L;

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);

            Order order = client.orders.create(orderRequest);

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.get("id").toString());
            result.put("amount", amount);
            result.put("currency", "INR");
            result.put("keyId", keyId);
            result.put("planType", "PRO");
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Razorpay error: " + e.getMessage());
        }
    }

    // Verify payment and activate subscription
    public Map<String, Object> verifyAndActivate(
            String email,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature,
            String planType) throws Exception {

        // Verify signature
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", razorpayOrderId);
        options.put("razorpay_payment_id", razorpayPaymentId);
        options.put("razorpay_signature", razorpaySignature);

        boolean valid = Utils.verifyPaymentSignature(options, keySecret);

        if (!valid) {
            throw new RuntimeException("Payment verification failed");
        }

        // Activate subscription
        User user = getUser(email);
        user.setSubscriptionPlan(planType);
        user.setSubscriptionStartDate(LocalDateTime.now());
        user.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));
        user.setRazorpaySubscriptionId(razorpayPaymentId);
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("plan", planType);
        result.put("message", planType + " plan activated successfully!");
        result.put("validUntil", user.getSubscriptionEndDate());
        return result;
    }
}