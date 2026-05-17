package com.smartledger.investment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    List<Investment> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(i.investedAmount), 0) FROM Investment i WHERE i.user.id = :userId AND i.active = true")
    BigDecimal totalInvested(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(i.currentValue), 0) FROM Investment i WHERE i.user.id = :userId AND i.active = true")
    BigDecimal totalCurrentValue(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(i.monthlySip), 0) FROM Investment i WHERE i.user.id = :userId AND i.active = true AND i.monthlySip IS NOT NULL")
    BigDecimal totalMonthlySip(@Param("userId") Long userId);

    @Query("SELECT i.type, SUM(i.investedAmount) FROM Investment i WHERE i.user.id = :userId AND i.active = true GROUP BY i.type")
    List<Object[]> investmentByType(@Param("userId") Long userId);
}