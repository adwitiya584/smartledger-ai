package com.smartledger.transaction;

import com.smartledger.auth.User;
import com.smartledger.auth.UserRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                               UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Transaction addTransaction(String email, TransactionDTO dto) {
        User user = getUser(email);
        Transaction t = new Transaction();
        t.setUser(user);
        t.setTitle(dto.getTitle());
        t.setAmount(dto.getAmount());
        t.setType(TransactionType.valueOf(dto.getType()));
        t.setCategory(dto.getCategory());
        t.setTransactionDate(dto.getTransactionDate());
        t.setNote(dto.getNote());
        return transactionRepository.save(t);
    }

    public List<Transaction> getAll(String email) {
        User user = getUser(email);
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId());
    }

    public void delete(Long id, String email) {
        User user = getUser(email);
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!t.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        transactionRepository.delete(t);
    }

    public Map<String, Object> getSummary(String email) {
        User user = getUser(email);
        BigDecimal income = transactionRepository
                .sumByUserIdAndType(user.getId(), TransactionType.INCOME);
        BigDecimal expense = transactionRepository
                .sumByUserIdAndType(user.getId(), TransactionType.EXPENSE);
        BigDecimal balance = income.subtract(expense);

        List<Object[]> categoryData = transactionRepository
                .sumExpensesByCategory(user.getId());
        Map<String, BigDecimal> categoryMap = new HashMap<>();
        for (Object[] row : categoryData) {
            categoryMap.put((String) row[0], (BigDecimal) row[1]);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", income);
        summary.put("totalExpense", expense);
        summary.put("balance", balance);
        summary.put("expenseByCategory", categoryMap);
        return summary;
    }
}