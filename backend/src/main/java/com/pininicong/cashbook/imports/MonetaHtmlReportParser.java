package com.pininicong.cashbook.imports;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MonetaHtmlReportParser {

    private static final Pattern ROW = Pattern.compile("<tr>\\s*(.*?)\\s*</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CELL = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final DateTimeFormatter DOT_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.ROOT);

    private MonetaHtmlReportParser() {}

    public enum ReportKind {
        EXPENSE,
        INCOME,
        SAVINGS,
        INSURANCE
    }

    public record ParsedRow(
            ReportKind kind,
            LocalDate date,
            String category,
            String title,
            BigDecimal amount,
            String cardName,
            String remarks) {}

    public static List<ParsedRow> parse(Path file, ReportKind kind) {
        try {
            String html = Files.readString(file, Charset.forName("EUC-KR"));
            return parseHtml(html, kind);
        } catch (Exception ex) {
            throw new IllegalArgumentException("파일 파싱 실패: " + file + " — " + ex.getMessage(), ex);
        }
    }

    public static List<ParsedRow> parseHtml(String html, ReportKind kind) {
        List<ParsedRow> rows = new ArrayList<>();
        Matcher rm = ROW.matcher(html);
        while (rm.find()) {
            String rowHtml = rm.group(1);
            if (rowHtml.contains("tb_sum")) continue;
            List<String> cells = new ArrayList<>();
            Matcher cm = CELL.matcher(rowHtml);
            while (cm.find()) {
                cells.add(strip(cm.group(1)));
            }
            if (cells.isEmpty() || "일자".equals(cells.get(0))) continue;
            ParsedRow parsed = toRow(kind, cells);
            if (parsed != null) rows.add(parsed);
        }
        return rows;
    }

    private static ParsedRow toRow(ReportKind kind, List<String> c) {
        return switch (kind) {
            case EXPENSE -> {
                if (c.size() < 7) yield null;
                LocalDate date = parseDate(c.get(0));
                if (date == null) yield null;
                BigDecimal cash = parseAmount(c.get(3));
                BigDecimal card = parseAmount(c.get(4));
                yield new ParsedRow(
                        kind,
                        date,
                        c.get(1),
                        c.get(2),
                        cash.add(card),
                        c.get(5),
                        c.get(6));
            }
            case INCOME -> {
                if (c.size() < 5) yield null;
                LocalDate date = parseDate(c.get(0));
                if (date == null) yield null;
                yield new ParsedRow(
                        kind,
                        date,
                        c.get(1),
                        c.get(2),
                        parseAmount(c.get(3)),
                        "",
                        c.get(4));
            }
            case SAVINGS -> {
                if (c.size() < 4) yield null;
                LocalDate date = parseDate(c.get(0));
                if (date == null) yield null;
                yield new ParsedRow(
                        kind,
                        date,
                        "저축",
                        c.get(1),
                        parseAmount(c.get(2)),
                        "",
                        c.get(3));
            }
            case INSURANCE -> {
                if (c.size() < 4) yield null;
                LocalDate date = parseDate(c.get(0));
                if (date == null) yield null;
                yield new ParsedRow(
                        kind,
                        date,
                        "보험",
                        c.get(1),
                        parseAmount(c.get(2)),
                        "",
                        c.get(3));
            }
        };
    }

    private static String strip(String raw) {
        return TAG.matcher(raw).replaceAll("").trim();
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDate.parse(raw.trim(), DOT_DATE);
    }

    private static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        String n = raw.replace(",", "").trim();
        if (n.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(n);
    }
}
