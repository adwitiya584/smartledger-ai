package com.smartledger.budget;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<Budget> save(Authentication auth,
                                        @RequestBody BudgetDTO dto) {
        return ResponseEntity.ok(budgetService.saveBudget(auth.getName(), dto));
    }

    @GetMapping("/status")
    public ResponseEntity<List<Map<String, Object>>> getStatus(
            Authentication auth,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        return ResponseEntity.ok(budgetService.getBudgetStatus(auth.getName(), month, year));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        budgetService.deleteBudget(id, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Budget deleted"));
    }
}