package com.smartledger.loan;

import com.smartledger.auth.User;
import com.smartledger.auth.UserRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;

    public LoanService(LoanRepository loanRepository,
                       UserRepository userRepository) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Loan create(String email, LoanDTO dto) {
        User user = getUser(email);
        Loan loan = new Loan();
        loan.setUser(user);
        loan.setName(dto.getName());
        loan.setType(dto.getType());
        loan.setPrincipalAmount(dto.getPrincipalAmount());
        loan.setOutstandingAmount(dto.getOutstandingAmount() != null
                ? dto.getOutstandingAmount() : dto.getPrincipalAmount());
        loan.setEmiAmount(dto.getEmiAmount());
        loan.setInterestRate(dto.getInterestRate());
        loan.setTenureMonths(dto.getTenureMonths());
        loan.setEmiDueDay(dto.getEmiDueDay());
        loan.setStartDate(dto.getStartDate());
        loan.setEndDate(dto.getEndDate());
        loan.setLender(dto.getLender());
        return loanRepository.save(loan);
    }

    public Map<String, Object> getAllLoans(String email) {
        User user = getUser(email);
        List<Loan> loans = loanRepository
                .findByUserIdAndActiveTrueOrderByCreatedAtDesc(user.getId());

        BigDecimal totalOutstanding = loanRepository.totalOutstanding(user.getId());
        BigDecimal totalMonthlyEmi = loanRepository.totalMonthlyEmi(user.getId());

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> loanDetails = new ArrayList<>();

        for (Loan loan : loans) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", loan.getId());
            item.put("name", loan.getName());
            item.put("type", loan.getType());
            item.put("principalAmount", loan.getPrincipalAmount());
            item.put("outstandingAmount", loan.getOutstandingAmount());
            item.put("emiAmount", loan.getEmiAmount());
            item.put("interestRate", loan.getInterestRate());
            item.put("tenureMonths", loan.getTenureMonths());
            item.put("emiDueDay", loan.getEmiDueDay());
            item.put("startDate", loan.getStartDate());
            item.put("endDate", loan.getEndDate());
            item.put("lender", loan.getLender());

            // EMI due info
            if (loan.getEmiDueDay() != null) {
                LocalDate nextEmi = LocalDate.of(
                    today.getYear(), today.getMonth(),
                    Math.min(loan.getEmiDueDay(), today.lengthOfMonth())
                );
                if (nextEmi.isBefore(today)) {
                    nextEmi = nextEmi.plusMonths(1);
                }
                long daysUntilEmi = ChronoUnit.DAYS.between(today, nextEmi);
                item.put("nextEmiDate", nextEmi);
                item.put("daysUntilEmi", daysUntilEmi);
                item.put("emiDueSoon", daysUntilEmi <= 5);
            }

            // Paid percentage
            double paidPercent = 100 - (loan.getOutstandingAmount().doubleValue()
                    / loan.getPrincipalAmount().doubleValue() * 100);
            item.put("paidPercentage", Math.max(paidPercent, 0));

            // Months remaining
            if (loan.getEmiAmount().doubleValue() > 0) {
                int monthsLeft = (int) Math.ceil(
                    loan.getOutstandingAmount().doubleValue()
                    / loan.getEmiAmount().doubleValue()
                );
                item.put("monthsRemaining", monthsLeft);
            }

            loanDetails.add(item);
        }

        // Sort by EMI due date
        loanDetails.sort((a, b) -> {
            Long da = (Long) a.get("daysUntilEmi");
            Long db = (Long) b.get("daysUntilEmi");
            if (da == null) return 1;
            if (db == null) return -1;
            return da.compareTo(db);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("loans", loanDetails);
        result.put("totalOutstanding", totalOutstanding);
        result.put("totalMonthlyEmi", totalMonthlyEmi);
        return result;
    }

    public Loan payEmi(String email, Long id) {
        User user = getUser(email);
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        if (!loan.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");
        BigDecimal newOutstanding = loan.getOutstandingAmount()
                .subtract(loan.getEmiAmount());
        if (newOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setOutstandingAmount(BigDecimal.ZERO);
            loan.setActive(false);
        } else {
            loan.setOutstandingAmount(newOutstanding);
        }
        return loanRepository.save(loan);
    }

    public void delete(String email, Long id) {
        User user = getUser(email);
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        if (!loan.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");
        loan.setActive(false);
        loanRepository.save(loan);
    }
}