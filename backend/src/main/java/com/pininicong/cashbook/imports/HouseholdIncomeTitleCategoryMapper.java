package com.pininicong.cashbook.imports;

import java.util.Optional;

/** 가계 수입 거래 제목 → 소분류명 (category_id 미연결 데이터 백필용). */
public final class HouseholdIncomeTitleCategoryMapper {

    private HouseholdIncomeTitleCategoryMapper() {}

    public static Optional<String> categoryForTitle(String title) {
        if (title == null) {
            return Optional.empty();
        }
        String t = title.trim();
        if (t.isEmpty()) {
            return Optional.empty();
        }

        if (t.equals("대출상환")) {
            return Optional.of("대출상환");
        }
        if (t.equals("실비환급") || t.startsWith("실비환급")) {
            return Optional.of("실비환급");
        }
        if (t.startsWith("부천페이")) {
            return Optional.of("충전");
        }
        if (t.equals("초기자금")) {
            return Optional.of("전월이월");
        }

        // 상여 (급여 입금 규칙보다 먼저)
        if (t.equals("아내상여")
                || t.equals("명절상여(아내)")
                || t.equals("상여(아내)")
                || (t.contains("상여") && t.contains("아내"))) {
            return Optional.of("상여-아내");
        }
        if (t.startsWith("남편입금(상여)")
                || t.equals("명절상여")
                || t.equals("추석상여")
                || t.equals("정기상여")
                || t.equals("연말상여")
                || t.equals("연초상여")
                || t.equals("장기근속상여")
                || (t.contains("상여") && !t.contains("아내"))) {
            return Optional.of("상여-남편");
        }

        if (t.startsWith("남편입금")
                || t.startsWith("남편입급")
                || t.startsWith("남편환급")
                || t.startsWith("급여인상")
                || t.equals("연말정산(남편)")
                || t.equals("연말정산")) {
            return Optional.of("급여-남편");
        }
        if (t.startsWith("아내입금")
                || t.startsWith("아내입급")
                || t.equals("연말정산(아내)")) {
            return Optional.of("급여-아내");
        }

        if (t.startsWith("월납입-남편")) {
            return Optional.of("입금-남편");
        }
        if (t.startsWith("월납입-아내")) {
            return Optional.of("입금-아내");
        }

        return Optional.empty();
    }
}
