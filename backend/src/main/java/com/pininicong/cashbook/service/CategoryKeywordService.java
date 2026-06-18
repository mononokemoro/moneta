package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbCategoryKeyword;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.CategoryKeywordDto;
import com.pininicong.cashbook.dto.CategoryKeywordUpsertRequest;
import com.pininicong.cashbook.repo.CbCategoryKeywordRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryKeywordService {

    private final CbCategoryKeywordRepository repo;

    public CategoryKeywordService(CbCategoryKeywordRepository repo) {
        this.repo = repo;
    }

    public List<CategoryKeywordDto> list(LedgerBook book, TxType txType) {
        LedgerBook ledger = bookOrDefault(book);
        return repo.findByBookAndTxTypeOrderBySortOrderAscKeywordAsc(ledger, txType).stream()
                .map(CategoryKeywordService::toDto)
                .toList();
    }

    public List<CategoryKeywordDto> listAll(LedgerBook book) {
        LedgerBook ledger = bookOrDefault(book);
        return List.of(TxType.EXPENSE, TxType.INCOME).stream()
                .flatMap(
                        type ->
                                repo.findByBookAndTxTypeOrderBySortOrderAscKeywordAsc(ledger, type)
                                        .stream())
                .map(CategoryKeywordService::toDto)
                .toList();
    }

    @Transactional
    public CategoryKeywordDto create(LedgerBook book, CategoryKeywordUpsertRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        validateTxType(req.txType());
        String keyword = normalize(req.keyword());
        String category = normalize(req.categoryName());

        CbCategoryKeyword row = new CbCategoryKeyword();
        row.setBook(ledger);
        row.setTxType(req.txType());
        row.setKeyword(keyword);
        row.setCategoryName(category);
        row.setSortOrder(nextSortOrder(ledger, req.txType()));
        repo.save(row);
        return toDto(row);
    }

    @Transactional
    public CategoryKeywordDto update(Long id, CategoryKeywordUpsertRequest req) {
        CbCategoryKeyword row =
                repo.findById(id).orElseThrow(() -> new IllegalArgumentException("예약어 없음: " + id));
        validateTxType(req.txType());
        row.setTxType(req.txType());
        row.setKeyword(normalize(req.keyword()));
        row.setCategoryName(normalize(req.categoryName()));
        repo.save(row);
        return toDto(row);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("예약어 없음: " + id);
        }
        repo.deleteById(id);
    }

    private int nextSortOrder(LedgerBook book, TxType txType) {
        return repo.findByBookAndTxTypeOrderBySortOrderAscKeywordAsc(book, txType).size();
    }

    private static void validateTxType(TxType txType) {
        if (txType != TxType.EXPENSE && txType != TxType.INCOME) {
            throw new IllegalArgumentException("지출·수입만 예약어를 등록할 수 있습니다.");
        }
    }

    private static String normalize(String value) {
        return value != null ? value.trim() : "";
    }

    private static LedgerBook bookOrDefault(LedgerBook book) {
        return book != null ? book : LedgerBook.PERSONAL;
    }

    private static CategoryKeywordDto toDto(CbCategoryKeyword row) {
        return new CategoryKeywordDto(
                row.getId(), row.getTxType(), row.getKeyword(), row.getCategoryName());
    }
}
