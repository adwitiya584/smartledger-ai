package com.smartledger.loan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(l.outstandingAmount), 0) FROM Loan l WHERE l.user.id = :userId AND l.active = true")
    BigDecimal totalOutstanding(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(l.emiAmount), 0) FROM Loan l WHERE l.user.id = :userId AND l.active = true")
    BigDecimal totalMonthlyEmi(@Param("userId") Long userId);
}