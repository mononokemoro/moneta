package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbFinancialProduct;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductStatus;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductType;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.CategoryDto.CategoryGroupDto;
import com.pininicong.cashbook.dto.CategoryDto.CategoryListResponse;
import com.pininicong.cashbook.dto.FinancialProductDto;
import com.pininicong.cashbook.dto.FinancialProductDto.CardSyncFromTransactionsResponse;
import com.pininicong.cashbook.dto.FinancialProductDto.FinancialProductListResponse;
import com.pininicong.cashbook.dto.FinancialProductDto.FinancialProductSaveRequest;
import com.pininicong.cashbook.repo.CbFinancialProductRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FinancialProductService {

    private final CbFinancialProductRepository productRepo;
    private final CategoryService categoryService;
    private final CbTransactionRepository txRepo;

    public FinancialProductService(
            CbFinancialProductRepository productRepo,
            CategoryService categoryService,
            CbTransactionRepository txRepo) {
        this.productRepo = productRepo;
        this.categoryService = categoryService;
        this.txRepo = txRepo;
    }

    public FinancialProductListResponse list(LedgerBook book) {
        LedgerBook ledger = bookOrDefault(book);
        if (productRepo.countByBook(ledger) == 0) {
            seedFromCategories(ledger);
        }
        return groupProducts(productRepo.findByBookOrderByProductTypeAscSortOrderAscIdAsc(ledger));
    }

    @Transactional
    public FinancialProductListResponse save(LedgerBook book, FinancialProductSaveRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        productRepo.deleteByBook(ledger);
        productRepo.flush();

        int order = 0;
        order = saveType(ledger, ProductType.SAVINGS, req.products().savings(), order);
        order = saveType(ledger, ProductType.INSURANCE, req.products().insurance(), order);
        order = saveType(ledger, ProductType.LOAN, req.products().loans(), order);
        saveType(ledger, ProductType.CARD, req.products().cards(), order);

        return list(ledger);
    }

    @Transactional
    public CardSyncFromTransactionsResponse syncCardsFromTransactions(LedgerBook book) {
        LedgerBook ledger = bookOrDefault(book);
        if (productRepo.countByBook(ledger) == 0) {
            seedFromCategories(ledger);
        }

        List<Long> cardProductIds = txRepo.findDistinctCardProductIds(ledger, TxType.EXPENSE);
        List<CbFinancialProduct> existingCards =
                productRepo.findByBookAndProductTypeOrderBySortOrderAscIdAsc(
                        ledger, ProductType.CARD);
        Set<Long> known = new HashSet<>();
        for (CbFinancialProduct card : existingCards) {
            known.add(card.getId());
        }

        int order =
                existingCards.stream()
                        .mapToInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0)
                        .max()
                        .orElse(-1)
                        + 1;
        int added = 0;
        for (Long cardProductId : cardProductIds) {
            if (cardProductId == null || known.contains(cardProductId)) {
                continue;
            }
            String name = productRepo.findById(cardProductId).map(CbFinancialProduct::getName).orElse("");
            if (name.isEmpty()) {
                continue;
            }
            CbFinancialProduct row = new CbFinancialProduct();
            row.setBook(ledger);
            row.setProductType(ProductType.CARD);
            row.setStatus(ProductStatus.ACTIVE);
            row.setSortOrder(order++);
            row.setClassification("신용카드");
            row.setName(name);
            row.setPaymentDay("13");
            row.setPeriodStartMonth("전월");
            row.setPeriodStartDay("01");
            row.setPeriodEndMonth("전월");
            row.setPeriodEndDay("31");
            productRepo.save(row);
            known.add(cardProductId);
            added++;
        }

        return new CardSyncFromTransactionsResponse(added, list(ledger));
    }

    private int saveType(
            LedgerBook ledger, ProductType type, List<FinancialProductDto> items, int startOrder) {
        int order = startOrder;
        for (FinancialProductDto dto : items) {
            CbFinancialProduct row;
            if (dto.id() != null) {
                row =
                        productRepo
                                .findByIdAndBook(dto.id(), ledger)
                                .orElseGet(CbFinancialProduct::new);
            } else {
                row = new CbFinancialProduct();
            }
            row.setBook(ledger);
            row.setProductType(type);
            row.setStatus(dto.status() != null ? dto.status() : ProductStatus.ACTIVE);
            row.setSortOrder(order++);
            applyDto(row, dto);
            productRepo.save(row);
        }
        return order;
    }

    @Transactional
    public void seedFromCategories(LedgerBook book) {
        CategoryListResponse cats = categoryService.listCategories(book);
        int order = 0;
        for (CategoryGroupDto group : cats.savings()) {
            for (var child : group.children()) {
                CbFinancialProduct row = new CbFinancialProduct();
                row.setBook(book);
                row.setProductType(ProductType.SAVINGS);
                row.setStatus(ProductStatus.ACTIVE);
                row.setSortOrder(order++);
                row.setClassification(guessSavingsClass(child.name(), group.name()));
                row.setName(child.name());
                productRepo.save(row);
            }
        }
        for (CategoryGroupDto group : cats.insurance()) {
            for (var child : group.children()) {
                CbFinancialProduct row = new CbFinancialProduct();
                row.setBook(book);
                row.setProductType(ProductType.INSURANCE);
                row.setStatus(ProductStatus.ACTIVE);
                row.setSortOrder(order++);
                row.setClassification("기타".equals(group.name()) ? "보장성보험" : group.name());
                row.setName(child.name());
                row.setPaymentMethod("현금");
                row.setTransferDay("15");
                productRepo.save(row);
            }
        }
    }

    private FinancialProductListResponse groupProducts(List<CbFinancialProduct> rows) {
        List<FinancialProductDto> savings = new ArrayList<>();
        List<FinancialProductDto> insurance = new ArrayList<>();
        List<FinancialProductDto> loans = new ArrayList<>();
        List<FinancialProductDto> cards = new ArrayList<>();
        for (CbFinancialProduct row : rows) {
            FinancialProductDto dto = toDto(row);
            switch (row.getProductType()) {
                case SAVINGS -> savings.add(dto);
                case INSURANCE -> insurance.add(dto);
                case LOAN -> loans.add(dto);
                case CARD -> cards.add(dto);
            }
        }
        return new FinancialProductListResponse(savings, insurance, loans, cards);
    }

    private static void applyDto(CbFinancialProduct row, FinancialProductDto dto) {
        row.setClassification(dto.classification());
        row.setName(dto.name());
        row.setPaymentMethod(dto.paymentMethod());
        row.setJoinDate(dto.joinDate());
        row.setMaturityDate(dto.maturityDate());
        row.setStartDate(dto.startDate());
        row.setAutoTransferDay(dto.autoTransferDay());
        row.setTransferDay(dto.transferDay());
        row.setRepaymentDay(dto.repaymentDay());
        row.setPaymentDay(dto.paymentDay());
        row.setPeriodStartMonth(dto.periodStartMonth());
        row.setPeriodStartDay(dto.periodStartDay());
        row.setPeriodEndMonth(dto.periodEndMonth());
        row.setPeriodEndDay(dto.periodEndDay());
        row.setPrincipal(dto.principal());
        row.setCardLimit(dto.cardLimit());
        row.setOpeningBalance(dto.openingBalance() != null ? dto.openingBalance() : java.math.BigDecimal.ZERO);
        row.setOpeningDate(parseOpeningDate(dto.openingDate()));
    }

    private static java.time.LocalDate parseOpeningDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 8) {
            return java.time.LocalDate.parse(
                    digits,
                    java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        }
        if (digits.length() == 6) {
            int yy = Integer.parseInt(digits.substring(0, 2));
            int year = yy >= 0 && yy <= 99 ? 2000 + yy : yy;
            int month = Integer.parseInt(digits.substring(2, 4));
            int day = Integer.parseInt(digits.substring(4, 6));
            return java.time.LocalDate.of(year, month, day);
        }
        try {
            return java.time.LocalDate.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String formatOpeningDate(java.time.LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    }

    private static String guessSavingsClass(String name, String majorName) {
        if (majorName != null && !majorName.isBlank() && !"기타".equals(majorName)) {
            return majorName;
        }
        if (name.contains("주식") || name.contains("키움")) {
            return "주식/투자";
        }
        if (name.contains("청약")) {
            return "청약저축";
        }
        if (name.contains("적금")) {
            return "적금";
        }
        return "예금";
    }

    private static FinancialProductDto toDto(CbFinancialProduct row) {
        return new FinancialProductDto(
                row.getId(),
                row.getProductType(),
                row.getStatus(),
                row.getSortOrder() != null ? row.getSortOrder() : 0,
                row.getClassification(),
                row.getName(),
                row.getPaymentMethod(),
                row.getJoinDate(),
                row.getMaturityDate(),
                row.getStartDate(),
                row.getAutoTransferDay(),
                row.getTransferDay(),
                row.getRepaymentDay(),
                row.getPaymentDay(),
                row.getPeriodStartMonth(),
                row.getPeriodStartDay(),
                row.getPeriodEndMonth(),
                row.getPeriodEndDay(),
                row.getPrincipal(),
                row.getCardLimit(),
                row.getOpeningBalance(),
                formatOpeningDate(row.getOpeningDate()));
    }

    private static LedgerBook bookOrDefault(LedgerBook book) {
        return book != null ? book : LedgerBook.PERSONAL;
    }
}
