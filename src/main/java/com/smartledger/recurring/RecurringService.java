package com.smartledger.recurring;

import com.smartledger.auth.User;
import com.smartledger.auth.UserRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class RecurringService {

    private final RecurringRepository recurringRepository;
    private final UserRepository userRepository;

    public RecurringService(RecurringRepository recurringRepository,
                             UserRepository userRepository) {
        this.recurringRepository = recurringRepository;
        this.userRepository = userRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public RecurringTransaction create(String email, Map<String, Object> body) {
        User user = getUser(email);
        RecurringTransaction r = new RecurringTransaction();
        r.setUser(user);
        r.setTitle((String) body.get("title"));
        r.setAmount(new java.math.BigDecimal(body.get("amount").toString()));
        r.setType((String) body.get("type"));
        r.setCategory((String) body.get("category"));
        r.setDueDay(Integer.parseInt(body.get("dueDay").toString()));
        r.setFrequency((String) body.getOrDefault("frequency", "MONTHLY"));
        r.setActive(true);

        // Calculate next due date
        LocalDate now = LocalDate.now();
        int dueDay = r.getDueDay();
        LocalDate nextDue = LocalDate.of(now.getYear(), now.getMonth(), 
            Math.min(dueDay, now.lengthOfMonth()));
        if (nextDue.isBefore(now) || nextDue.isEqual(now)) {
            nextDue = nextDue.plusMonths(1);
        }
        r.setNextDueDate(nextDue);
        return recurringRepository.save(r);
    }

    public List<Map<String, Object>> getAll(String email) {
        User user = getUser(email);
        List<RecurringTransaction> list = recurringRepository
                .findByUserIdAndActiveTrue(user.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (RecurringTransaction r : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("title", r.getTitle());
            item.put("amount", r.getAmount());
            item.put("type", r.getType());
            item.put("category", r.getCategory());
            item.put("dueDay", r.getDueDay());
            item.put("frequency", r.getFrequency());
            item.put("nextDueDate", r.getNextDueDate());

            // Days until due
            if (r.getNextDueDate() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS
                        .between(today, r.getNextDueDate());
                item.put("daysUntilDue", days);
                item.put("isDueSoon", days <= 5 && days >= 0);
                item.put("isOverdue", days < 0);
            }
            result.add(item);
        }

        // Sort by next due date
        result.sort((a, b) -> {
            LocalDate da = (LocalDate) a.get("nextDueDate");
            LocalDate db = (LocalDate) b.get("nextDueDate");
            if (da == null) return 1;
            if (db == null) return -1;
            return da.compareTo(db);
        });
        return result;
    }

    public void delete(String email, Long id) {
        User user = getUser(email);
        RecurringTransaction r = recurringRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        if (!r.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");
        r.setActive(false);
        recurringRepository.save(r);
    }
}