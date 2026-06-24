package com.pininicong.cashbook.imports;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 가계 장부 지출 분류 (My분류설정 기준) */
public final class HouseholdExpenseCategoryCatalog {

    public record Group(String major, List<String> minors) {}

    private HouseholdExpenseCategoryCatalog() {}

    public static final List<Group> GROUPS =
            List.of(
                    new Group("식비", List.of("기타", "외식", "부식", "주식", "커피")),
                    new Group("경조사", List.of("축의금", "부의금", "회비", "선물", "기타")),
                    new Group(
                            "주거/통신",
                            List.of(
                                    "월세",
                                    "수도료",
                                    "전기료",
                                    "난방비",
                                    "이동통신",
                                    "인터넷",
                                    "기타",
                                    "관리비",
                                    "전화",
                                    "수리비")),
                    new Group("용돈/기타", List.of("남편용돈", "아내용돈", "자녀용돈", "기타")),
                    new Group("이자비용", List.of("대출이자", "기타")),
                    new Group("카드대금", List.of("카드대금", "아내환급", "남편환급")),
                    new Group(
                            "생활용품",
                            List.of(
                                    "기타",
                                    "잡화소모",
                                    "주방용품",
                                    "가전용품",
                                    "가구",
                                    "세탁용품",
                                    "수납용품",
                                    "침구",
                                    "청소용품")),
                    new Group("가족", List.of("선물", "기타", "대결", "명절", "외식", "생신", "기념")),
                    new Group("기타", List.of("기타")),
                    new Group("지인", List.of("방문", "초청")),
                    new Group(
                            "건강/문화",
                            List.of(
                                    "기타",
                                    "레져",
                                    "여행",
                                    "영화",
                                    "신문",
                                    "약값",
                                    "병원비",
                                    "구독",
                                    "게임")),
                    new Group(
                            "마트",
                            List.of("홈플러스", "E-마트", "쿠팡", "마켓컬리", "쓱배송", "B-마트")),
                    new Group("식비(제)", List.of("간식", "배달", "빵집", "과일")),
                    new Group("충전", List.of("충전")),
                    new Group("제외", List.of("제외")),
                    new Group(
                            "교통/차량",
                            List.of("기타", "자동차수리", "세차비", "주유비", "대중교통")),
                    new Group(
                            "세금외",
                            List.of(
                                    "자동차세",
                                    "재산세",
                                    "국민연금",
                                    "주민세",
                                    "소득세",
                                    "기타",
                                    "건강보험")));

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
