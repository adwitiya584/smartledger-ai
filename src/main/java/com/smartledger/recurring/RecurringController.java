package com.smartledger.recurring;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recurring")
public class RecurringController {

    private final RecurringService recurringService;

    public RecurringController(RecurringService recurringService) {
        this.recurringService = recurringService;
    }

    @PostMapping
    public ResponseEntity<RecurringTransaction> create(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(recurringService.create(auth.getName(), body));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(Authentication auth) {
        return ResponseEntity.ok(recurringService.getAll(auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        recurringService.delete(auth.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}