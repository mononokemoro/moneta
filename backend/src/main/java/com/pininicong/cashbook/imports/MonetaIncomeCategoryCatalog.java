package com.pininicong.cashbook.imports;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 모네타 수입 분류 대·소분류 (My분류설정 기준) */
public final class MonetaIncomeCategoryCatalog {

    public record Group(String major, List<String> minors) {}

    private MonetaIncomeCategoryCatalog() {}

    public static final List<Group> GROUPS =
            List.of(
                    new Group("주수입", List.of("급여", "상여", "사업", "기타")),
                    new Group("부수입", List.of("이자", "기타", "주식", "적립", "오류", "티켓")),
                    new Group("전월이월", List.of("전월이월")),
                    new Group("수입제외", List.of("가계환급", "수입제외")));

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
