package com.smartledger.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);

    Optional<Budget> findByUserIdAndCategoryAndMonthAndYear(
        Long userId, String category, Integer month, Integer year);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.month = :month AND b.year = :year")
    List<Budget> findBudgetsWithSpending(@Param("userId") Long userId,
                                          @Param("month") Integer month,
                                          @Param("year") Integer year);
}