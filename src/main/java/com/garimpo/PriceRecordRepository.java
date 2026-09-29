package com.garimpo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {
    Optional<PriceRecord> findByProductIdAndDay(Long productId, LocalDate day);
    List<PriceRecord> findByProductIdAndDayGreaterThanEqual(Long productId, LocalDate since);
    void deleteByProductId(Long productId);
}
