package com.pininicong.cashbook.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record ReportRowDto(String key, String label, List<BigDecimal> values, BigDecimal periodTotal) {}
