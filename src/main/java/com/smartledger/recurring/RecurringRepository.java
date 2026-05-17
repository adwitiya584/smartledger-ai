package com.smartledger.recurring;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecurringRepository extends JpaRepository<RecurringTransaction, Long> {
    List<RecurringTransaction> findByUserIdAndActiveTrue(Long userId);
}