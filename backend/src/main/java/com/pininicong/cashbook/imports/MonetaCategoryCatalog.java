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
                    List.of(
                            "가계:상환", "가계:입금", "가계:지출", "가계:환급", "가족", "기타", "낚시:용품", "낚시:출조",
                            "몽당", "미분류", "복권", "부식", "선물", "쇼핑:잡화", "쇼핑:전자", "의류", "제외:대결", "주식",
                            "주유비", "주차비", "직장", "취미:게임", "취미:등산", "취미:야구", "취미:영화", "친구", "하이패스",
                            "할인"),
                    CategoryType.INCOME,
                    List.of("가계환급", "급여", "기타", "상여", "수입제외", "이자", "적립", "주식"),
                    CategoryType.SAVINGS,
                    List.of(
                            "1.토스", "2.농협", "3.네이버", "예금.토스", "예치.출조", "예치.토스", "주식.키움", "주식.토스",
                            "채무.선상환(*)", "채무.함무니(*)", "청약통장", "통장.케이", "통장.토스box", "페이.신세계", "페이.쿠팡",
                            "현금.보관"),
                    CategoryType.INSURANCE,
                    List.of("실비보험"));
}
