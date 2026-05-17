package com.smartledger.loan;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<Loan> create(
            Authentication auth,
            @RequestBody LoanDTO dto) {
        return ResponseEntity.ok(loanService.create(auth.getName(), dto));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(Authentication auth) {
        return ResponseEntity.ok(loanService.getAllLoans(auth.getName()));
    }

    @PostMapping("/{id}/pay-emi")
    public ResponseEntity<Loan> payEmi(
            Authentication auth,
            @PathVariable Long id) {
        return ResponseEntity.ok(loanService.payEmi(auth.getName(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            Authentication auth,
            @PathVariable Long id) {
        loanService.delete(auth.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Loan removed"));
    }
}