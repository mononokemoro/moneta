package com.pininicong.cashbook.imports;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 가계 장부 수입 분류 (My분류설정 기준) */
public final class HouseholdIncomeCategoryCatalog {

    public record Group(String major, List<String> minors) {}

    private HouseholdIncomeCategoryCatalog() {}

    public static final List<Group> GROUPS =
            List.of(
                    new Group(
                            "주수입",
                            List.of(
                                    "급여-남편",
                                    "상여-아내",
                                    "기타",
                                    "급여-아내",
                                    "상여-남편",
                                    "사업",
                                    "상여",
                                    "급여")),
                    new Group("부수입", List.of("주식", "기타", "이자")),
                    new Group("전월이월", List.of("전월이월")),
                    new Group(
                            "수입제외",
                            List.of("환불", "대결", "충전", "입금-남편", "입금-아내", "정산")),
                    new Group("대출상환", List.of("대출상환")),
                    new Group("실비환급", List.of("실비환급")));

    /** 소분류명 → 대분류명 (동명 소분류는 먼저 정의된 대분류 우선) */
    public static Map<String, String> minorToMajor() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Group group : GROUPS) {
            for (String minor : group.minors()) {
                map.putIfAbsent(minor, group.major());
            }
        }
        return map;
    }

    public static List<String> allMinorNames() {
        return GROUPS.stream().flatMap(g -> g.minors().stream()).distinct().toList();
    }
}
