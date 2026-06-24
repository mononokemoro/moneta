package com.pininicong.cashbook.support;

import java.util.LinkedHashMap;
import java.util.Map;

/** 개발자 데이터 화면용 테이블·컬럼 설명 카탈로그 */
public final class DatabaseMetadataCatalog {

    private static final Map<String, String> TABLE_DESCRIPTIONS = tableDescriptions();
    private static final Map<String, Map<String, String>> COLUMN_DESCRIPTIONS = columnDescriptions();

    private DatabaseMetadataCatalog() {}

    public static String tableDescription(String table) {
        return TABLE_DESCRIPTIONS.getOrDefault(table, "—");
    }

    public static String columnDescription(String table, String column) {
        Map<String, String> cols = COLUMN_DESCRIPTIONS.get(table);
        if (cols == null) {
            return "—";
        }
        return cols.getOrDefault(column, "—");
    }

    private static Map<String, String> tableDescriptions() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("cb_book_settings", "장부별 UI 설정 (항목 입력 시 표시할 부가 필드 모드)");
        m.put("cb_cash_balance", "장부별 현금 잔액");
        m.put("cb_category", "지출·수입·저축·보험 분류 항목 (대/중/소 계층)");
        m.put("cb_financial_product", "금융상품 마스터 (저축·보험·대출·카드)");
        m.put("cb_category_keyword", "거래 제목 예약어 → 분류 자동 매칭 규칙");
        m.put("cb_fixed_item", "고정 지출·수입 항목 템플릿 (반복 입력용)");
        m.put("cb_monthly_budget", "월별 총 예산");
        m.put("cb_daily_sheet", "일별 일정 메모·오늘의 메모");
        m.put("cb_transaction", "거래 내역 (지출·수입·저축·보험)");
        return Map.copyOf(m);
    }

    private static Map<String, Map<String, String>> columnDescriptions() {
        Map<String, Map<String, String>> all = new LinkedHashMap<>();

        all.put("cb_book_settings", cols(
                "book", "장부 구분 (PK, PERSONAL / HOUSEHOLD)",
                "expense_field_mode", "지출 항목 부가 필드 모드 (REMARKS 등)",
                "savings_insurance_field_mode", "저축·보험 항목 부가 필드 모드",
                "loan_field_mode", "대출 항목 부가 필드 모드",
                "income_field_mode", "수입 항목 부가 필드 모드"));

        all.put("cb_cash_balance", cols(
                "book", "장부 구분 (PK)",
                "amount", "현재 현금 잔액"));

        all.put("cb_category", cols(
                "id", "분류 항목 ID (PK, 자동 증가)",
                "book", "장부 구분",
                "category_type", "분류 종류 (EXPENSE / INCOME / SAVINGS / INSURANCE)",
                "tier", "계층 단계 (MAJOR / MINOR 등)",
                "parent_id", "상위 분류 ID → cb_category.id",
                "name", "분류 항목명",
                "sort_order", "목록 표시 순서",
                "enabled", "사용 여부",
                "user_created", "사용자가 직접 생성한 항목 여부",
                "fixed_expense", "고정 지출 분류 여부"));

        all.put("cb_financial_product", cols(
                "id", "금융상품 ID (PK, 자동 증가)",
                "book", "장부 구분",
                "product_type", "상품 유형 (SAVINGS / INSURANCE / LOAN / CARD)",
                "status", "상태 (ACTIVE / MATURED / TERMINATED 등)",
                "sort_order", "목록 표시 순서",
                "classification", "상품 세부 분류",
                "name", "상품명 (거래 card_product_id 와 FK 연계)",
                "payment_method", "납입·결제 방식",
                "join_date", "가입일 (YYYYMMDD)",
                "maturity_date", "만기일 (YYYYMMDD)",
                "start_date", "시작일 (YYYYMMDD)",
                "auto_transfer_day", "자동이체일",
                "transfer_day", "이체일",
                "repayment_day", "상환일",
                "payment_day", "납입일",
                "period_start_month", "기간 시작 월",
                "period_start_day", "기간 시작 일",
                "period_end_month", "기간 종료 월",
                "period_end_day", "기간 종료 일",
                "principal", "원금·가입금액",
                "card_limit", "카드 한도"));

        all.put("cb_category_keyword", cols(
                "id", "예약어 규칙 ID (PK, 자동 증가)",
                "book", "장부 구분",
                "tx_type", "적용 거래 유형 (EXPENSE / INCOME / SAVINGS)",
                "keyword", "매칭 키워드 (거래 제목에 포함 시)",
                "category_name", "매핑될 분류 항목명 → cb_category.name",
                "sort_order", "매칭 우선순서 (낮을수록 먼저)"));

        all.put("cb_fixed_item", cols(
                "id", "고정 항목 ID (PK, 자동 증가)",
                "book", "장부 구분",
                "title", "고정 항목명",
                "default_amount", "기본 금액",
                "category", "분류명 → cb_category.name (soft)",
                "card_name", "카드명 → cb_financial_product.name (soft)",
                "tx_type", "거래 유형 (EXPENSE / INCOME / SAVINGS)",
                "sort_order", "목록 표시 순서"));

        all.put("cb_monthly_budget", cols(
                "book", "장부 구분 (PK)",
                "year_month", "예산 월 (PK, YYYY-MM)",
                "total_budget", "월 총 예산 금액"));

        all.put("cb_daily_sheet", cols(
                "book", "장부 구분 (PK)",
                "sheet_date", "일자 (PK)",
                "schedule_note", "상단 일정 메모",
                "day_memo", "하단 오늘의 메모"));

        all.put("cb_transaction", cols(
                "id", "거래 ID (PK, 자동 증가)",
                "book", "장부 구분",
                "tx_date", "거래 일자",
                "tx_type", "거래 유형 (EXPENSE / INCOME / SAVINGS / INSURANCE)",
                "title", "항목명",
                "amount", "금액",
                "category_id", "소분류 ID → cb_category.id",
                "household_category_id", "가계 연동 소분류 ID → cb_category.id (개인 지출)",
                "card_product_id", "카드 상품 ID → cb_financial_product.id (CARD, 현금이면 null)",
                "savings_product_id", "저축/보험 상품 ID → cb_financial_product.id",
                "remarks", "비고·메모",
                "accumulated_amount", "레거시 누적금액 (표시는 계산값 우선)",
                "sort_order", "일별 목록 표시 순서",
                "expense_scope", "지출 범위 (NORMAL / COMMON, 가계 공통지출)",
                "linked_tx_id", "연동 거래 ID → cb_transaction.id (장부 간 미러)"));

        return Map.copyOf(all);
    }

    private static Map<String, String> cols(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }
}
