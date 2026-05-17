package com.smartledger.subscription;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.getStatus(auth.getName()));
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(
                subscriptionService.createOrder(auth.getName(), body.get("plan")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("message", "Failed to create order: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(subscriptionService.verifyAndActivate(
                auth.getName(),
                body.get("razorpayOrderId"),
                body.get("razorpayPaymentId"),
                body.get("razorpaySignature"),
                body.get("plan")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                .body(Map.of("message", e.getMessage()));
        }
    }
}