package com.pininicong.cashbook.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CategoryReorderRequest(@NotEmpty List<@Valid CategoryReorderItem> items) {}
