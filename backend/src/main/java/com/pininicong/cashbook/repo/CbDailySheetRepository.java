package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbDailySheet;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbDailySheetRepository extends JpaRepository<CbDailySheet, LocalDate> {}
