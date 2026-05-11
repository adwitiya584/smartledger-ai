package com.smartledger.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        String message = body.get("message");
        String reply = aiService.chat(auth.getName(), message);
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}