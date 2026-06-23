package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbBookSettings;
import com.pininicong.cashbook.domain.LedgerBook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbBookSettingsRepository extends JpaRepository<CbBookSettings, LedgerBook> {}
