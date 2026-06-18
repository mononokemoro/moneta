package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbDailySheet;
import com.pininicong.cashbook.domain.DailySheetKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbDailySheetRepository extends JpaRepository<CbDailySheet, DailySheetKey> {}
