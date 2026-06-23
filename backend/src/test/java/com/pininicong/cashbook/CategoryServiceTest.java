package com.pininicong.cashbook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.domain.CbCategory;
import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.CategoryTier;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.CategoryCreateRequest;
import com.pininicong.cashbook.dto.CategoryUpdateRequest;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.CategoryService;
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
        assertThat(main.children()).extracting("name").contains("급여", "상여", "사업", "기타");

        var excluded =
                tree.income().stream().filter(g -> "수입제외".equals(g.name())).findFirst().orElseThrow();
        assertThat(excluded.children()).extracting("name").contains("가계환급", "수입제외");
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
        tx.setCategory("테스트분류");
        tx.setSortOrder(0);
        txRepo.save(tx);

        categoryService.update(
                minor.id(), LedgerBook.PERSONAL, new CategoryUpdateRequest("테스트분류수정", null));

        var saved = txRepo.findById(tx.getId()).orElseThrow();
        assertThat(saved.getCategory()).isEqualTo("테스트분류수정");
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
        tx.setCategory("사용중분류");
        tx.setSortOrder(0);
        txRepo.save(tx);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> categoryService.delete(minor.id(), LedgerBook.PERSONAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내역에 사용 중");
    }
}
