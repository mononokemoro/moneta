package com.pininicong.cashbook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.domain.CbCategory;
import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.CategoryTier;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.CategoryCreateRequest;
import com.pininicong.cashbook.dto.CategoryReorderItem;
import com.pininicong.cashbook.dto.CategoryReorderRequest;
import com.pininicong.cashbook.dto.CategoryUpdateRequest;
import java.util.ArrayList;
import java.util.List;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.CategoryService;
import com.pininicong.cashbook.service.MonetaImportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CategoryServiceTest {

    @Autowired CategoryService categoryService;
    @Autowired CbTransactionRepository txRepo;
    @Autowired com.pininicong.cashbook.repo.CbCategoryRepository categoryRepo;

    @Test
    void migrateFlatToHierarchyPromotesGitaMajor() {
        var existing =
                categoryRepo.findByBookAndCategoryTypeOrderBySortOrderAscNameAsc(
                        LedgerBook.HOUSEHOLD, CategoryType.EXPENSE);
        existing.stream()
                .filter(c -> c.getParentId() != null)
                .forEach(categoryRepo::delete);
        existing.stream()
                .filter(c -> c.getParentId() == null)
                .forEach(categoryRepo::delete);
        categoryRepo.flush();

        var gita = new CbCategory();
        gita.setBook(LedgerBook.HOUSEHOLD);
        gita.setCategoryType(CategoryType.EXPENSE);
        gita.setName("기타");
        categoryRepo.save(gita);

        var food = new CbCategory();
        food.setBook(LedgerBook.HOUSEHOLD);
        food.setCategoryType(CategoryType.EXPENSE);
        food.setName("식비");
        categoryRepo.save(food);

        int migrated = categoryService.migrateFlatToHierarchy(LedgerBook.HOUSEHOLD);
        assertThat(migrated).isEqualTo(1);

        var tree = categoryService.listCategories(LedgerBook.HOUSEHOLD);
        var gitaMajor =
                tree.expense().stream().filter(g -> "기타".equals(g.name())).findFirst().orElseThrow();
        assertThat(gitaMajor.children()).extracting("name").containsExactly("식비");
    }

    @Test
    void migrateIncomeHierarchyAssignsMonetaGroups() {
        categoryService.migrateIncomeHierarchy(LedgerBook.PERSONAL);

        var tree = categoryService.listCategories(LedgerBook.PERSONAL);
        assertThat(tree.income()).extracting("name")
                .contains("주수입", "부수입", "전월이월", "수입제외");

        var main =
                tree.income().stream().filter(g -> "주수입".equals(g.name())).findFirst().orElseThrow();
        assertThat(main.children()).extracting("name").contains("급여", "상여", "기타");

        var side =
                tree.income().stream().filter(g -> "부수입".equals(g.name())).findFirst().orElseThrow();
        assertThat(side.children()).extracting("name").contains("이자", "기타", "주식", "적립", "오류", "티켓");

        var excluded =
                tree.income().stream().filter(g -> "수입제외".equals(g.name())).findFirst().orElseThrow();
        assertThat(excluded.children()).extracting("name").contains("가계환급", "수입제외");
    }

    @Test
    void migrateIncomeHierarchyReparentsOrphanGitaMajorUnderSideIncome() {
        categoryService.migrateIncomeHierarchy(LedgerBook.PERSONAL);

        var orphanMajor = new CbCategory();
        orphanMajor.setBook(LedgerBook.PERSONAL);
        orphanMajor.setCategoryType(CategoryType.INCOME);
        orphanMajor.setTier(CategoryTier.MAJOR);
        orphanMajor.setName("기타");
        categoryRepo.save(orphanMajor);
        categoryRepo.flush();

        categoryService.migrateIncomeHierarchy(LedgerBook.PERSONAL);

        assertThat(
                        categoryRepo.findByBookAndCategoryTypeAndNameAndTier(
                                LedgerBook.PERSONAL,
                                CategoryType.INCOME,
                                "기타",
                                CategoryTier.MAJOR))
                .isEmpty();

        var tree = categoryService.listCategories(LedgerBook.PERSONAL);
        var main =
                tree.income().stream().filter(g -> "주수입".equals(g.name())).findFirst().orElseThrow();
        assertThat(main.children()).extracting("name").contains("기타");

        var side =
                tree.income().stream().filter(g -> "부수입".equals(g.name())).findFirst().orElseThrow();
        assertThat(side.children()).extracting("name").contains("기타");
    }

    @Test
    void migrateIncomeHierarchyPreservesUserSortOrder() {
        categoryService.migrateIncomeHierarchy(LedgerBook.PERSONAL);
        var tree = categoryService.listCategories(LedgerBook.PERSONAL);
        var main =
                tree.income().stream().filter(g -> "주수입".equals(g.name())).findFirst().orElseThrow();

        var reordered = new ArrayList<>(main.children());
        java.util.Collections.reverse(reordered);
        List<CategoryReorderItem> items = new ArrayList<>();
        for (int i = 0; i < reordered.size(); i++) {
            items.add(new CategoryReorderItem(reordered.get(i).id(), i, main.id()));
        }
        categoryService.reorder(LedgerBook.PERSONAL, new CategoryReorderRequest(items));

        categoryService.migrateIncomeHierarchy(LedgerBook.PERSONAL);

        var after = categoryService.listCategories(LedgerBook.PERSONAL);
        var mainAfter =
                after.income().stream().filter(g -> "주수입".equals(g.name())).findFirst().orElseThrow();
        assertThat(mainAfter.children()).extracting("name")
                .containsExactlyElementsOf(reordered.stream().map(c -> c.name()).toList());
    }

    @Test
    void migrateHouseholdIncomeHierarchyAssignsHouseholdGroups() {
        categoryService.migrateIncomeHierarchy(LedgerBook.HOUSEHOLD);

        var tree = categoryService.listCategories(LedgerBook.HOUSEHOLD);
        assertThat(tree.income()).extracting("name")
                .contains(
                        "주수입",
                        "부수입",
                        "전월이월",
                        "수입제외",
                        "대출상환",
                        "실비환급");
        assertThat(tree.income()).extracting("name").doesNotContain("적립", "오류", "티켓");

        var main =
                tree.income().stream().filter(g -> "주수입".equals(g.name())).findFirst().orElseThrow();
        assertThat(main.children()).extracting("name")
                .contains(
                        "급여-남편",
                        "상여-아내",
                        "급여-아내",
                        "상여-남편",
                        "기타",
                        "사업",
                        "상여",
                        "급여");

        var side =
                tree.income().stream().filter(g -> "부수입".equals(g.name())).findFirst().orElseThrow();
        assertThat(side.children()).extracting("name").contains("주식", "기타", "이자");

        var excluded =
                tree.income().stream().filter(g -> "수입제외".equals(g.name())).findFirst().orElseThrow();
        assertThat(excluded.children()).extracting("name")
                .contains("환불", "대결", "충전", "입금-남편", "입금-아내", "정산");

        var loan =
                tree.income().stream().filter(g -> "대출상환".equals(g.name())).findFirst().orElseThrow();
        assertThat(loan.children()).extracting("name").containsExactly("대출상환");

        var medical =
                tree.income().stream().filter(g -> "실비환급".equals(g.name())).findFirst().orElseThrow();
        assertThat(medical.children()).extracting("name").containsExactly("실비환급");
    }

    @Test
    void migrateHouseholdExpenseHierarchyAssignsHouseholdGroups() {
        categoryService.migrateExpenseHierarchy(LedgerBook.HOUSEHOLD);

        var tree = categoryService.listCategories(LedgerBook.HOUSEHOLD);
        assertThat(tree.expense()).extracting("name")
                .contains(
                        "식비",
                        "경조사",
                        "주거/통신",
                        "용돈/기타",
                        "이자비용",
                        "카드대금",
                        "생활용품",
                        "가족",
                        "지인",
                        "건강/문화",
                        "마트",
                        "식비(제)",
                        "충전",
                        "제외",
                        "교통/차량",
                        "세금외");
        assertThat(tree.expense()).extracting("name")
                .doesNotContain("대인", "취미", "쇼핑", "애인", "가계/일반", "의복");

        var food =
                tree.expense().stream().filter(g -> "식비".equals(g.name())).findFirst().orElseThrow();
        assertThat(food.children()).extracting("name")
                .contains("기타", "외식", "부식", "주식", "커피");

        var card =
                tree.expense().stream().filter(g -> "카드대금".equals(g.name())).findFirst().orElseThrow();
        assertThat(card.children()).extracting("name")
                .contains("카드대금", "아내환급", "남편환급");

        var mart =
                tree.expense().stream().filter(g -> "마트".equals(g.name())).findFirst().orElseThrow();
        assertThat(mart.children()).extracting("name")
                .contains("홈플러스", "E-마트", "쿠팡", "마켓컬리", "쓱배송", "B-마트");

        var tax =
                tree.expense().stream().filter(g -> "세금외".equals(g.name())).findFirst().orElseThrow();
        assertThat(tax.children()).extracting("name")
                .contains("자동차세", "재산세", "국민연금", "주민세", "소득세", "기타", "건강보험");
    }

    @Test
    void migrateExpenseHierarchyAssignsMonetaGroups() {
        categoryService.migrateExpenseHierarchy(LedgerBook.PERSONAL);

        var tree = categoryService.listCategories(LedgerBook.PERSONAL);
        assertThat(tree.expense()).extracting("name")
                .contains("식비", "용돈/기타", "주거/통신", "대인", "쇼핑", "경조사");

        var food =
                tree.expense().stream().filter(g -> "식비".equals(g.name())).findFirst().orElseThrow();
        assertThat(food.children()).extracting("name").contains("외식", "부식", "주식", "기타");

        var household =
                tree.expense().stream().filter(g -> "가계/일반".equals(g.name())).findFirst().orElseThrow();
        assertThat(household.children()).extracting("name")
                .contains("가계:상환", "가계:입금", "가계:지출");

        var hobby =
                tree.expense().stream().filter(g -> "취미".equals(g.name())).findFirst().orElseThrow();
        assertThat(hobby.children()).extracting("name")
                .contains("낚시:친목", "낚시:출조", "낚시:용품");
        assertThat(tree.expense()).extracting("name").doesNotContain("낚시");
    }

    @Test
    void migrateExpenseHierarchyRemovesEmptyNaksiMajor() {
        categoryService.migrateExpenseHierarchy(LedgerBook.PERSONAL);

        var emptyNaksi = new CbCategory();
        emptyNaksi.setBook(LedgerBook.PERSONAL);
        emptyNaksi.setCategoryType(CategoryType.EXPENSE);
        emptyNaksi.setTier(CategoryTier.MAJOR);
        emptyNaksi.setName("낚시");
        categoryRepo.save(emptyNaksi);
        categoryRepo.flush();

        categoryService.migrateExpenseHierarchy(LedgerBook.PERSONAL);

        assertThat(
                        categoryRepo.findByBookAndCategoryTypeAndNameAndTier(
                                LedgerBook.PERSONAL,
                                CategoryType.EXPENSE,
                                "낚시",
                                CategoryTier.MAJOR))
                .isEmpty();
    }

    @Test
    void renameExpenseCategoryUpdatesTransactions() {
        var major =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.EXPENSE, CategoryTier.MAJOR, "테스트", null));
        var minor =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.EXPENSE,
                                CategoryTier.MINOR,
                                "테스트분류",
                                major.id()));

        var tx = new com.pininicong.cashbook.domain.CbTransaction();
        tx.setBook(LedgerBook.PERSONAL);
        tx.setTxDate(LocalDate.of(2026, 6, 20));
        tx.setTxType(TxType.EXPENSE);
        tx.setTitle("점심");
        tx.setAmount(new BigDecimal("9000"));
        tx.setCategoryId(minor.id());
        tx.setSortOrder(0);
        txRepo.save(tx);

        categoryService.update(
                minor.id(), LedgerBook.PERSONAL, new CategoryUpdateRequest("테스트분류수정", null));

        var saved = txRepo.findById(tx.getId()).orElseThrow();
        assertThat(saved.getCategoryId()).isEqualTo(minor.id());
        assertThat(
                        categoryRepo
                                .findById(minor.id())
                                .orElseThrow()
                                .getName())
                .isEqualTo("테스트분류수정");
    }

    @Autowired MonetaImportService importService;

    @Test
    void listTransactionsFindsRowsAfterHouseholdTitleBackfill() {
        categoryService.migrateIncomeHierarchy(LedgerBook.HOUSEHOLD);
        var tree = categoryService.listCategories(LedgerBook.HOUSEHOLD);
        var main =
                tree.income().stream().filter(g -> "주수입".equals(g.name())).findFirst().orElseThrow();
        var husband =
                main.children().stream()
                        .filter(c -> "급여-남편".equals(c.name()))
                        .findFirst()
                        .orElseThrow();

        var tx = new com.pininicong.cashbook.domain.CbTransaction();
        tx.setBook(LedgerBook.HOUSEHOLD);
        tx.setTxDate(LocalDate.of(2026, 5, 30));
        tx.setTxType(TxType.INCOME);
        tx.setTitle("남편입금(월)");
        tx.setAmount(new BigDecimal("2330000"));
        tx.setSortOrder(0);
        txRepo.save(tx);

        int linked = importService.backfillHouseholdIncomeCategoryIdsFromTitles();
        assertThat(linked).isEqualTo(1);

        var result =
                categoryService.listTransactionsForCategory(husband.id(), LedgerBook.HOUSEHOLD);
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.items()).extracting("title").containsExactly("남편입금(월)");
    }

    @Test
    void listTransactionsFindsWifeBonusAfterTitleBackfill() {
        categoryService.migrateIncomeHierarchy(LedgerBook.HOUSEHOLD);
        var tree = categoryService.listCategories(LedgerBook.HOUSEHOLD);
        var main =
                tree.income().stream().filter(g -> "주수입".equals(g.name())).findFirst().orElseThrow();
        var wifeBonus =
                main.children().stream()
                        .filter(c -> "상여-아내".equals(c.name()))
                        .findFirst()
                        .orElseThrow();
        var wifeSalary =
                main.children().stream()
                        .filter(c -> "급여-아내".equals(c.name()))
                        .findFirst()
                        .orElseThrow();

        var tx = new com.pininicong.cashbook.domain.CbTransaction();
        tx.setBook(LedgerBook.HOUSEHOLD);
        tx.setTxDate(LocalDate.of(2025, 9, 15));
        tx.setTxType(TxType.INCOME);
        tx.setTitle("아내상여");
        tx.setAmount(new BigDecimal("800000"));
        tx.setCategoryId(wifeSalary.id());
        tx.setSortOrder(0);
        txRepo.save(tx);

        int linked = importService.backfillHouseholdIncomeCategoryIdsFromTitles();
        assertThat(linked).isEqualTo(1);

        var result =
                categoryService.listTransactionsForCategory(wifeBonus.id(), LedgerBook.HOUSEHOLD);
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.items()).extracting("title").containsExactly("아내상여");
    }

    @Test
    void listTransactionsFindsRowsLinkedToDuplicateMinorId() {
        categoryService.migrateIncomeHierarchy(LedgerBook.HOUSEHOLD);
        var tree = categoryService.listCategories(LedgerBook.HOUSEHOLD);
        var main =
                tree.income().stream().filter(g -> "주수입".equals(g.name())).findFirst().orElseThrow();
        var canonical =
                main.children().stream()
                        .filter(c -> "급여-남편".equals(c.name()))
                        .findFirst()
                        .orElseThrow();

        var orphan = new CbCategory();
        orphan.setBook(LedgerBook.HOUSEHOLD);
        orphan.setCategoryType(CategoryType.INCOME);
        orphan.setTier(CategoryTier.MINOR);
        orphan.setParentId(null);
        orphan.setName("급여-남편");
        orphan.setUserCreated(true);
        categoryRepo.save(orphan);

        var tx = new com.pininicong.cashbook.domain.CbTransaction();
        tx.setBook(LedgerBook.HOUSEHOLD);
        tx.setTxDate(LocalDate.of(2026, 6, 1));
        tx.setTxType(TxType.INCOME);
        tx.setTitle("월급");
        tx.setAmount(new BigDecimal("5000000"));
        tx.setCategoryId(orphan.getId());
        tx.setSortOrder(0);
        txRepo.save(tx);

        var result =
                categoryService.listTransactionsForCategory(
                        canonical.id(), LedgerBook.HOUSEHOLD);
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.items()).extracting("title").containsExactly("월급");
    }

    @Test
    void deleteBlockedWhenCategoryInUse() {
        var major =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.EXPENSE, CategoryTier.MAJOR, "삭제테스트", null));
        var minor =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.EXPENSE,
                                CategoryTier.MINOR,
                                "사용중분류",
                                major.id()));

        var tx = new com.pininicong.cashbook.domain.CbTransaction();
        tx.setBook(LedgerBook.PERSONAL);
        tx.setTxDate(LocalDate.of(2026, 6, 20));
        tx.setTxType(TxType.EXPENSE);
        tx.setTitle("점심");
        tx.setAmount(new BigDecimal("9000"));
        tx.setCategoryId(minor.id());
        tx.setSortOrder(0);
        txRepo.save(tx);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> categoryService.delete(minor.id(), LedgerBook.PERSONAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내역에 사용 중");
    }

    @Test
    void deleteEmptyMajorAllowsCatalogMajor() {
        var major =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.EXPENSE, CategoryTier.MAJOR, "빈대분류", null));
        categoryService.delete(major.id(), LedgerBook.PERSONAL);
        assertThat(categoryRepo.findByIdAndBook(major.id(), LedgerBook.PERSONAL)).isEmpty();
    }

    @Test
    void listTransactionsForMinorCategory() {
        var major =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.EXPENSE, CategoryTier.MAJOR, "조회테스트", null));
        var minor =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.EXPENSE,
                                CategoryTier.MINOR,
                                "조회소분류",
                                major.id()));

        var tx = new com.pininicong.cashbook.domain.CbTransaction();
        tx.setBook(LedgerBook.PERSONAL);
        tx.setTxDate(LocalDate.of(2026, 6, 21));
        tx.setTxType(TxType.EXPENSE);
        tx.setTitle("테스트지출");
        tx.setAmount(new BigDecimal("15000"));
        tx.setCategoryId(minor.id());
        tx.setSortOrder(0);
        txRepo.save(tx);

        var result = categoryService.listTransactionsForCategory(minor.id(), LedgerBook.PERSONAL);
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.items()).extracting("title").containsExactly("테스트지출");
        assertThat(result.totalAmount()).isEqualByComparingTo("15000");
    }

    @Test
    void listTransactionTableReturnsCbTransactionColumns() {
        var major =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.EXPENSE, CategoryTier.MAJOR, "테이블조회대", null));
        var minor =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.EXPENSE,
                                CategoryTier.MINOR,
                                "테이블조회",
                                major.id()));

        var tx = new com.pininicong.cashbook.domain.CbTransaction();
        tx.setBook(LedgerBook.PERSONAL);
        tx.setTxDate(LocalDate.of(2026, 6, 23));
        tx.setTxType(TxType.EXPENSE);
        tx.setTitle("테이블지출");
        tx.setAmount(new BigDecimal("9900"));
        tx.setCategoryId(minor.id());
        tx.setRemarks("비고");
        tx.setSortOrder(2);
        txRepo.save(tx);

        var table =
                categoryService.listTransactionTableForCategory(minor.id(), LedgerBook.PERSONAL);
        assertThat(table.tableName()).isEqualTo("cb_transaction");
        assertThat(table.count()).isEqualTo(1);
        assertThat(table.querySql()).contains("category_id IN");
        assertThat(table.querySql()).contains("book = 'PERSONAL'");
        assertThat(table.rows()).hasSize(1);
        var row = table.rows().get(0);
        assertThat(row.id()).isEqualTo(tx.getId());
        assertThat(row.book()).isEqualTo("PERSONAL");
        assertThat(row.txDate()).isEqualTo("2026-06-23");
        assertThat(row.txType()).isEqualTo("EXPENSE");
        assertThat(row.title()).isEqualTo("테이블지출");
        assertThat(row.amount()).isEqualByComparingTo("9900");
        assertThat(row.categoryId()).isEqualTo(minor.id());
        assertThat(row.remarks()).isEqualTo("비고");
        assertThat(row.sortOrder()).isEqualTo(2);
    }

    @Test
    void listTransactionsForMajorIncludesChildren() {
        var major =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.INCOME, CategoryTier.MAJOR, "수입조회", null));
        var minor =
                categoryService.create(
                        LedgerBook.PERSONAL,
                        new CategoryCreateRequest(
                                CategoryType.INCOME,
                                CategoryTier.MINOR,
                                "수입소분류",
                                major.id()));

        var tx = new com.pininicong.cashbook.domain.CbTransaction();
        tx.setBook(LedgerBook.PERSONAL);
        tx.setTxDate(LocalDate.of(2026, 6, 22));
        tx.setTxType(TxType.INCOME);
        tx.setTitle("급여");
        tx.setAmount(new BigDecimal("3000000"));
        tx.setCategoryId(minor.id());
        tx.setSortOrder(0);
        txRepo.save(tx);

        var result = categoryService.listTransactionsForCategory(major.id(), LedgerBook.PERSONAL);
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.items()).extracting("title").containsExactly("급여");
    }
}
