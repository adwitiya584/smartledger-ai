package com.smartledger.budget;

import com.smartledger.auth.User;
import com.smartledger.auth.UserRepository;
import com.smartledger.transaction.TransactionRepository;
import com.smartledger.transaction.TransactionType;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         UserRepository userRepository,
                         TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Budget saveBudget(String email, BudgetDTO dto) {
        User user = getUser(email);
        Optional<Budget> existing = budgetRepository
                .findByUserIdAndCategoryAndMonthAndYear(
                    user.getId(), dto.getCategory(), dto.getMonth(), dto.getYear());

        Budget budget = existing.orElse(new Budget());
        budget.setUser(user);
        budget.setCategory(dto.getCategory());
        budget.setMonthlyLimit(dto.getMonthlyLimit());
        budget.setMonth(dto.getMonth());
        budget.setYear(dto.getYear());
        return budgetRepository.save(budget);
    }

    public List<Map<String, Object>> getBudgetStatus(String email, Integer month, Integer year) {
        User user = getUser(email);
        List<Budget> budgets = budgetRepository
                .findByUserIdAndMonthAndYear(user.getId(), month, year);

        // Get spending per category for this month
        List<Object[]> categorySpending = transactionRepository
                .sumExpensesByCategory(user.getId());

        Map<String, BigDecimal> spendingMap = new HashMap<>();
        for (Object[] row : categorySpending) {
            spendingMap.put((String) row[0], (BigDecimal) row[1]);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Budget b : budgets) {
            Map<String, Object> item = new HashMap<>();
            BigDecimal spent = spendingMap.getOrDefault(b.getCategory(), BigDecimal.ZERO);
            BigDecimal remaining = b.getMonthlyLimit().subtract(spent);
            double percentage = spent.doubleValue() / b.getMonthlyLimit().doubleValue() * 100;

            item.put("category", b.getCategory());
            item.put("limit", b.getMonthlyLimit());
            item.put("spent", spent);
            item.put("remaining", remaining);
            item.put("percentage", Math.min(percentage, 100));
            item.put("exceeded", spent.compareTo(b.getMonthlyLimit()) > 0);
            result.add(item);
        }
        return result;
    }

    public void deleteBudget(Long id, String email) {
        User user = getUser(email);
        Budget b = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        if (!b.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        budgetRepository.delete(b);
    }
}