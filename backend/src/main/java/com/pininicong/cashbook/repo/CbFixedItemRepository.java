package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbFixedItem;
import com.pininicong.cashbook.domain.LedgerBook;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbFixedItemRepository extends JpaRepository<CbFixedItem, Long> {

    List<CbFixedItem> findByBookOrderBySortOrderAscIdAsc(LedgerBook book);

    void deleteByBook(LedgerBook book);

    long countByBook(LedgerBook book);
}
