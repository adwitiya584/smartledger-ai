package com.smartledger.goals;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalService goalService;

    public SavingsGoalController(SavingsGoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<SavingsGoal> create(
            Authentication auth,
            @RequestBody SavingsGoalDTO dto) {
        return ResponseEntity.ok(goalService.createGoal(auth.getName(), dto));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(Authentication auth) {
        return ResponseEntity.ok(goalService.getGoals(auth.getName()));
    }

    @PostMapping("/{id}/add-savings")
    public ResponseEntity<SavingsGoal> addSavings(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body) {
        return ResponseEntity.ok(goalService.addSavings(auth.getName(), id, body.get("amount")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        goalService.deleteGoal(auth.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Goal deleted"));
    }
}