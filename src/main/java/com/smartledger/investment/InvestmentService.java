package com.smartledger.investment;

import com.smartledger.auth.User;
import com.smartledger.auth.UserRepository;
import com.smartledger.transaction.TransactionRepository;
import com.smartledger.transaction.TransactionType;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public InvestmentService(InvestmentRepository investmentRepository,
                              UserRepository userRepository,
                              TransactionRepository transactionRepository) {
        this.investmentRepository = investmentRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Investment create(String email, InvestmentDTO dto) {
        User user = getUser(email);
        Investment inv = new Investment();
        inv.setUser(user);
        inv.setName(dto.getName());
        inv.setType(dto.getType());
        inv.setInvestedAmount(dto.getInvestedAmount());
        inv.setCurrentValue(dto.getCurrentValue() != null
                ? dto.getCurrentValue() : dto.getInvestedAmount());
        inv.setMonthlySip(dto.getMonthlySip());
        inv.setStartDate(dto.getStartDate());
        inv.setMaturityDate(dto.getMaturityDate());
        inv.setNotes(dto.getNotes());
        return investmentRepository.save(inv);
    }

    public Map<String, Object> getPortfolio(String email) {
        User user = getUser(email);
        List<Investment> investments = investmentRepository
                .findByUserIdAndActiveTrueOrderByCreatedAtDesc(user.getId());

        BigDecimal totalInvested = investmentRepository.totalInvested(user.getId());
        BigDecimal totalCurrentValue = investmentRepository.totalCurrentValue(user.getId());
        BigDecimal totalMonthlySip = investmentRepository.totalMonthlySip(user.getId());
        BigDecimal totalIncome = transactionRepository
                .sumByUserIdAndType(user.getId(), TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository
                .sumByUserIdAndType(user.getId(), TransactionType.EXPENSE);

        // Returns
        BigDecimal totalReturns = totalCurrentValue.subtract(totalInvested);
        double returnsPercentage = totalInvested.doubleValue() > 0
                ? (totalReturns.doubleValue() / totalInvested.doubleValue()) * 100 : 0;

        // Net savings = Income - Expenses - Investments
        BigDecimal netSavings = totalIncome.subtract(totalExpense).subtract(totalInvested);

        // By type breakdown
        List<Object[]> byType = investmentRepository.investmentByType(user.getId());
        Map<String, BigDecimal> typeBreakdown = new LinkedHashMap<>();
        for (Object[] row : byType) {
            typeBreakdown.put((String) row[0], (BigDecimal) row[1]);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("investments", investments);
        result.put("totalInvested", totalInvested);
        result.put("totalCurrentValue", totalCurrentValue);
        result.put("totalMonthlySip", totalMonthlySip);
        result.put("totalReturns", totalReturns);
        result.put("returnsPercentage", returnsPercentage);
        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("netSavings", netSavings);
        result.put("typeBreakdown", typeBreakdown);
        return result;
    }

    public Investment updateValue(String email, Long id, BigDecimal newValue) {
        User user = getUser(email);
        Investment inv = investmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Investment not found"));
        if (!inv.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");
        inv.setCurrentValue(newValue);
        return investmentRepository.save(inv);
    }

    public void delete(String email, Long id) {
        User user = getUser(email);
        Investment inv = investmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Investment not found"));
        if (!inv.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");
        inv.setActive(false);
        investmentRepository.save(inv);
    }
}