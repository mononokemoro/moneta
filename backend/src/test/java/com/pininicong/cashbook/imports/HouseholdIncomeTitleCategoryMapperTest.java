package com.pininicong.cashbook.imports;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HouseholdIncomeTitleCategoryMapperTest {

    @Test
    void mapsWifeBonusTitlesToSangyeoAine() {
        assertThat(HouseholdIncomeTitleCategoryMapper.categoryForTitle("아내상여"))
                .contains("상여-아내");
        assertThat(HouseholdIncomeTitleCategoryMapper.categoryForTitle("명절상여(아내)"))
                .contains("상여-아내");
        assertThat(HouseholdIncomeTitleCategoryMapper.categoryForTitle("상여(아내)"))
                .contains("상여-아내");
    }

    @Test
    void mapsWifeSalarySeparatelyFromBonus() {
        assertThat(HouseholdIncomeTitleCategoryMapper.categoryForTitle("아내입금(월)"))
                .contains("급여-아내");
    }

    @Test
    void mapsHusbandBonusDepositToSangyeoNampyeon() {
        assertThat(HouseholdIncomeTitleCategoryMapper.categoryForTitle("남편입금(상여)"))
                .contains("상여-남편");
    }
}
