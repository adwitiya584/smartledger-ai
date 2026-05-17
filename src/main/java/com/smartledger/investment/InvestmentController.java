package com.smartledger.investment;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @PostMapping
    public ResponseEntity<Investment> create(
            Authentication auth,
            @RequestBody InvestmentDTO dto) {
        return ResponseEntity.ok(investmentService.create(auth.getName(), dto));
    }

    @GetMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> getPortfolio(Authentication auth) {
        return ResponseEntity.ok(investmentService.getPortfolio(auth.getName()));
    }

    @PutMapping("/{id}/update-value")
    public ResponseEntity<Investment> updateValue(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body) {
        return ResponseEntity.ok(
            investmentService.updateValue(auth.getName(), id, body.get("currentValue"))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        investmentService.delete(auth.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Investment removed"));
    }
}