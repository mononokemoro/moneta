package com.pininicong.cashbook.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.domain.CbCategoryKeyword;
import com.pininicong.cashbook.domain.TxType;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryKeywordMatcherTest {

    @Test
    void picksLongestMatchingKeyword() {
        CbCategoryKeyword shortRule = rule("마트", "쇼핑:잡화", 0);
        CbCategoryKeyword longRule = rule("이마트", "쇼핑:전자", 0);

        assertThat(CategoryKeywordMatcher.matchCategory("이마트 결제", List.of(shortRule, longRule)))
                .isEqualTo("쇼핑:전자");
    }

    @Test
    void returnsNullWhenNoMatch() {
        assertThat(CategoryKeywordMatcher.matchCategory("커피", List.of(rule("마트", "쇼핑", 0))))
                .isNull();
    }

    private static CbCategoryKeyword rule(String keyword, String category, int order) {
        CbCategoryKeyword row = new CbCategoryKeyword();
        row.setTxType(TxType.EXPENSE);
        row.setKeyword(keyword);
        row.setCategoryName(category);
        row.setSortOrder(order);
        return row;
    }
}
