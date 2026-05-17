package com.smartledger.goals;

import com.smartledger.auth.User;
import com.smartledger.auth.UserRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;

    public SavingsGoalService(SavingsGoalRepository goalRepository,
                               UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public SavingsGoal createGoal(String email, SavingsGoalDTO dto) {
        User user = getUser(email);
        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName(dto.getName());
        goal.setTargetAmount(dto.getTargetAmount());
        goal.setSavedAmount(dto.getSavedAmount() != null ? dto.getSavedAmount() : BigDecimal.ZERO);
        goal.setTargetDate(dto.getTargetDate());
        goal.setIcon(dto.getIcon() != null ? dto.getIcon() : "🎯");
        goal.setColor(dto.getColor() != null ? dto.getColor() : "#6366f1");
        return goalRepository.save(goal);
    }

    public List<Map<String, Object>> getGoals(String email) {
        User user = getUser(email);
        List<SavingsGoal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (SavingsGoal g : goals) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", g.getId());
            item.put("name", g.getName());
            item.put("targetAmount", g.getTargetAmount());
            item.put("savedAmount", g.getSavedAmount());
            item.put("targetDate", g.getTargetDate());
            item.put("icon", g.getIcon());
            item.put("color", g.getColor());

            double percentage = g.getSavedAmount().doubleValue()
                    / g.getTargetAmount().doubleValue() * 100;
            item.put("percentage", Math.min(percentage, 100));
            item.put("remaining", g.getTargetAmount().subtract(g.getSavedAmount()));
            item.put("completed", g.getSavedAmount().compareTo(g.getTargetAmount()) >= 0);

            // Days remaining
            if (g.getTargetDate() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), g.getTargetDate());
                item.put("daysRemaining", days);

                // Monthly savings needed
                if (days > 0) {
                    BigDecimal remaining = g.getTargetAmount().subtract(g.getSavedAmount());
                    double monthsLeft = days / 30.0;
                    double monthlyNeeded = remaining.doubleValue() / monthsLeft;
                    item.put("monthlyNeeded", Math.max(monthlyNeeded, 0));
                }
            }
            result.add(item);
        }
        return result;
    }

    public SavingsGoal addSavings(String email, Long goalId, BigDecimal amount) {
        User user = getUser(email);
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!goal.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");
        goal.setSavedAmount(goal.getSavedAmount().add(amount));
        return goalRepository.save(goal);
    }

    public void deleteGoal(String email, Long goalId) {
        User user = getUser(email);
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!goal.getUser().getId().equals(user.getId()))
            throw new RuntimeException("Unauthorized");
        goalRepository.delete(goal);
    }
}