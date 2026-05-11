package com.smartledger.transaction;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> add(Authentication auth,
                                            @RequestBody TransactionDTO dto) {
        return ResponseEntity.ok(transactionService.addTransaction(auth.getName(), dto));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAll(Authentication auth) {
        return ResponseEntity.ok(transactionService.getAll(auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        transactionService.delete(id, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(Authentication auth) {
        return ResponseEntity.ok(transactionService.getSummary(auth.getName()));
    }
}