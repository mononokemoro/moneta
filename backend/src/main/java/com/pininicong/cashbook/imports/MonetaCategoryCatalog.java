package com.pininicong.cashbook.imports;

import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import java.util.List;
import java.util.Map;

/** 모네타 미가스마트보내기(2026 상반기)에서 추출한 분류·저축·보험 명칭 */
public final class MonetaCategoryCatalog {

    private MonetaCategoryCatalog() {}

    public static final Map<CategoryType, List<String>> NAMES =
            Map.of(
                    CategoryType.EXPENSE,
                    MonetaExpenseCategoryCatalog.allMinorNames(),
                    CategoryType.INCOME,
                    MonetaIncomeCategoryCatalog.allMinorNames(),
                    CategoryType.SAVINGS,
                    List.of(
                            "1.토스", "2.농협", "3.네이버", "예금.토스", "예치.출조", "예치.토스", "주식.키움", "주식.토스",
                            "채무.선상환(*)", "채무.함무니(*)", "청약통장", "통장.케이", "통장.토스box", "페이.신세계", "페이.쿠팡",
                            "현금.보관"),
                    CategoryType.INSURANCE,
                    List.of("실비보험"));
}
