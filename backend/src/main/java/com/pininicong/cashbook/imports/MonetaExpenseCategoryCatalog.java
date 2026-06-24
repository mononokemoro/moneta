package com.pininicong.cashbook.imports;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 모네타 개인 지출 분류 대·소분류 (My분류설정 기준) */
public final class MonetaExpenseCategoryCatalog {

    public record Group(String major, List<String> minors) {}

    private MonetaExpenseCategoryCatalog() {}

    public static final List<Group> GROUPS =
            List.of(
                    new Group("식비", List.of("외식", "부식", "주식", "기타")),
                    new Group("용돈/기타", List.of("남편용돈", "아내용돈", "자녀용돈", "기타")),
                    new Group(
                            "주거/통신",
                            List.of(
                                    "관리비", "월세", "수도료", "전기료", "난방비", "전화", "이동통신", "인터넷",
                                    "기타")),
                    new Group("이자비용", List.of("대출이자", "기타")),
                    new Group("카드대금", List.of("지출제외", "카드대금")),
                    new Group("애인", List.of("선물", "부식", "기타", "영화", "음료", "식사")),
                    new Group("대인", List.of("친구", "가족", "가족카드", "직장", "레저", "몽당")),
                    new Group("의복", List.of("신발", "세탁비", "기타", "수선", "의류", "잡화")),
                    new Group("기타", List.of("기타", "할인", "복권", "미분류")),
                    new Group("제외", List.of("제외:티켓", "제외:대결", "제외:충전", "제외:취소")),
                    new Group(
                            "취미",
                            List.of(
                                    "취미:영화",
                                    "취미:야구",
                                    "취미:게임",
                                    "취미:피방",
                                    "취미:등산",
                                    "낚시:친목",
                                    "낚시:출조",
                                    "낚시:용품")),
                    new Group(
                            "교통/차량",
                            List.of(
                                    "대중교통", "자동차수리", "세차비", "기타", "주차비", "주유비", "자동차소모",
                                    "하이패스", "보험료")),
                    new Group("쇼핑", List.of("쇼핑:전자", "쇼핑:잡화", "쇼핑:차량")),
                    new Group("가계/환급", List.of("가계:환급")),
                    new Group(
                            "가계/일반",
                            List.of("가계:보험", "가계:상환", "가계:지출", "가계:입금")),
                    new Group("경조사", List.of("선물", "기타", "회비", "부의금", "축의금")));

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
