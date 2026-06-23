package com.pininicong.cashbook.service;

import com.pininicong.cashbook.dto.DatabaseAdminDto.BackupExport;
import com.pininicong.cashbook.dto.DatabaseAdminDto.BackupTable;
import com.pininicong.cashbook.dto.DatabaseAdminDto.ColumnInfo;
import com.pininicong.cashbook.dto.DatabaseAdminDto.DatabaseOverview;
import com.pininicong.cashbook.dto.DatabaseAdminDto.RestoreResult;
import com.pininicong.cashbook.dto.DatabaseAdminDto.TableRelation;
import com.pininicong.cashbook.dto.DatabaseAdminDto.TableRowsResponse;
import com.pininicong.cashbook.dto.DatabaseAdminDto.TableSummary;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import com.pininicong.cashbook.support.DatabaseMetadataCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseAdminService {

    private static final int BACKUP_VERSION = 1;

    private static final List<String> TABLE_ORDER =
            List.of(
                    "cb_book_settings",
                    "cb_cash_balance",
                    "cb_category",
                    "cb_financial_product",
                    "cb_category_keyword",
                    "cb_fixed_item",
                    "cb_monthly_budget",
                    "cb_daily_sheet",
                    "cb_transaction");

    private static final Map<String, String> ENTITY_BY_TABLE =
            Map.ofEntries(
                    Map.entry("cb_book_settings", "CbBookSettings"),
                    Map.entry("cb_cash_balance", "CbCashBalance"),
                    Map.entry("cb_category", "CbCategory"),
                    Map.entry("cb_category_keyword", "CbCategoryKeyword"),
                    Map.entry("cb_daily_sheet", "CbDailySheet"),
                    Map.entry("cb_fixed_item", "CbFixedItem"),
                    Map.entry("cb_financial_product", "CbFinancialProduct"),
                    Map.entry("cb_monthly_budget", "CbMonthlyBudget"),
                    Map.entry("cb_transaction", "CbTransaction"));

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public DatabaseAdminService(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    public DatabaseOverview getOverview() {
        String product = jdbc.queryForObject("SELECT DATABASE()", String.class);
        String url = maskJdbcUrl(readJdbcUrl());
        String schema = readSchema();
        List<TableSummary> tables = new ArrayList<>();
        long totalRows = 0;
        for (String table : existingTables()) {
            long count = countRows(table);
            totalRows += count;
            tables.add(
                    new TableSummary(
                            table,
                            DatabaseMetadataCatalog.tableDescription(table),
                            ENTITY_BY_TABLE.getOrDefault(table, "—"),
                            count,
                            loadColumns(table, schema)));
        }
        return new DatabaseOverview(
                product, url, schema, totalRows, tables, logicalRelations());
    }

    public TableRowsResponse getTableRows(String table, int offset, int limit) {
        String normalized = normalizeTable(table);
        if (!existingTables().contains(normalized)) {
            throw new IllegalArgumentException("알 수 없는 테이블: " + table);
        }
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.min(Math.max(1, limit), 500);
        long total = countRows(normalized);
        List<String> columns = columnNames(normalized);
        String colList = String.join(", ", columns);
        String sql =
                "SELECT "
                        + colList
                        + " FROM "
                        + normalized
                        + " ORDER BY 1 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Map<String, Object>> rows =
                jdbc.query(
                        sql,
                        (rs, rowNum) -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (String col : columns) {
                                row.put(col, rs.getObject(col));
                            }
                            return row;
                        },
                        safeOffset,
                        safeLimit);
        return new TableRowsResponse(normalized, total, safeOffset, safeLimit, columns, rows);
    }

    public BackupExport exportBackup() {
        Map<String, BackupTable> tables = new LinkedHashMap<>();
        for (String table : existingTables()) {
            List<String> columns = columnNames(table);
            String colList = String.join(", ", columns);
            List<Map<String, Object>> rows =
                    jdbc.query(
                            "SELECT " + colList + " FROM " + table,
                            (rs, rowNum) -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                for (String col : columns) {
                                    row.put(col, rs.getObject(col));
                                }
                                return row;
                            });
            tables.put(table, new BackupTable(columns, rows));
        }
        return new BackupExport(
                BACKUP_VERSION, Instant.now().toString(), readDatabaseProduct(), tables);
    }

    @Transactional
    public RestoreResult restoreBackup(BackupExport backup) {
        if (backup == null || backup.tables() == null || backup.tables().isEmpty()) {
            throw new IllegalArgumentException("백업 데이터가 비어 있습니다.");
        }
        if (backup.version() != BACKUP_VERSION) {
            throw new IllegalArgumentException("지원하지 않는 백업 버전: " + backup.version());
        }
        Set<String> known = new LinkedHashSet<>(existingTables());
        for (String table : backup.tables().keySet()) {
            if (!known.contains(table)) {
                throw new IllegalArgumentException("백업에 알 수 없는 테이블이 포함됨: " + table);
            }
        }

        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            List<String> deleteOrder = new ArrayList<>(TABLE_ORDER);
            deleteOrder.sort(Comparator.reverseOrder());
            for (String table : deleteOrder) {
                if (known.contains(table)) {
                    jdbc.execute("DELETE FROM " + table);
                }
            }

            Map<String, Integer> rowCounts = new LinkedHashMap<>();
            for (String table : TABLE_ORDER) {
                BackupTable part = backup.tables().get(table);
                if (part == null || part.rows() == null || part.rows().isEmpty()) {
                    rowCounts.put(table, 0);
                    continue;
                }
                List<Map<String, Object>> rows = sortForInsert(table, part.rows());
                List<String> columns = part.columns();
                if (columns == null || columns.isEmpty()) {
                    columns = columnNames(table);
                }
                insertRows(table, columns, rows);
                rowCounts.put(table, rows.size());
                resetIdentity(table);
            }

            long total = rowCounts.values().stream().mapToLong(Integer::longValue).sum();
            return new RestoreResult(Instant.now().toString(), rowCounts, total);
        } finally {
            jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    private List<Map<String, Object>> sortForInsert(String table, List<Map<String, Object>> rows) {
        if (!"cb_category".equals(table)) {
            return rows;
        }
        List<Map<String, Object>> sorted = new ArrayList<>(rows);
        sorted.sort(
                Comparator.comparing(
                        row -> row.get("parent_id") == null ? 0 : 1));
        return sorted;
    }

    private void insertRows(String table, List<String> columns, List<Map<String, Object>> rows) {
        String placeholders = String.join(", ", columns.stream().map(c -> "?").toList());
        String colList = String.join(", ", columns);
        String sql = "INSERT INTO " + table + " (" + colList + ") VALUES (" + placeholders + ")";
        jdbc.batchUpdate(
                sql,
                rows,
                rows.size(),
                (ps, row) -> {
                    for (int i = 0; i < columns.size(); i++) {
                        ps.setObject(i + 1, row.get(columns.get(i)));
                    }
                });
    }

    private void resetIdentity(String table) {
        if (!hasIdentityColumn(table)) {
            return;
        }
        Long maxId = jdbc.queryForObject("SELECT MAX(id) FROM " + table, Long.class);
        long next = maxId == null ? 1L : maxId + 1L;
        jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + next);
    }

    private boolean hasIdentityColumn(String table) {
        return switch (table) {
            case "cb_category",
                    "cb_category_keyword",
                    "cb_fixed_item",
                    "cb_financial_product",
                    "cb_transaction" -> true;
            default -> false;
        };
    }

    private long countRows(String table) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return count == null ? 0L : count;
    }

    private List<String> columnNames(String table) {
        return loadColumns(table, readSchema()).stream().map(ColumnInfo::name).toList();
    }

    private List<ColumnInfo> loadColumns(String table, String schema) {
        List<ColumnInfo> columns = new ArrayList<>();
        Set<String> primaryKeys = primaryKeys(table);
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, schema, table.toUpperCase(Locale.ROOT), null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT);
                    columns.add(
                            new ColumnInfo(
                                    name,
                                    DatabaseMetadataCatalog.columnDescription(table, name),
                                    rs.getString("TYPE_NAME"),
                                    rs.getObject("COLUMN_SIZE") == null
                                            ? null
                                            : rs.getInt("COLUMN_SIZE"),
                                    "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")),
                                    rs.getString("COLUMN_DEF"),
                                    primaryKeys.contains(name)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("컬럼 조회 실패: " + table, e);
        }
        return columns;
    }

    private Set<String> primaryKeys(String table) {
        Set<String> keys = new LinkedHashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getPrimaryKeys(null, readSchema(), table.toUpperCase(Locale.ROOT))) {
                while (rs.next()) {
                    keys.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("PK 조회 실패: " + table, e);
        }
        return keys;
    }

    private List<String> existingTables() {
        List<String> found = new ArrayList<>();
        for (String table : TABLE_ORDER) {
            if (tableExists(table)) {
                found.add(table);
            }
        }
        return found;
    }

    private boolean tableExists(String table) {
        Integer count =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
                        Integer.class,
                        table.toUpperCase(Locale.ROOT));
        return count != null && count > 0;
    }

    private String normalizeTable(String table) {
        return table.trim().toLowerCase(Locale.ROOT);
    }

    private String readSchema() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.getSchema() == null ? "PUBLIC" : conn.getSchema();
        } catch (SQLException e) {
            return "PUBLIC";
        }
    }

    private String readJdbcUrl() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.getMetaData().getURL();
        } catch (SQLException e) {
            return "";
        }
    }

    private String readDatabaseProduct() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            return "UNKNOWN";
        }
    }

    private static String maskJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.replaceAll("password=[^;&]*", "password=****");
    }

    private static List<TableRelation> logicalRelations() {
        return List.of(
                new TableRelation(
                        "cb_category",
                        "parent_id",
                        "cb_category",
                        "id",
                        "FK (logical)",
                        "분류 계층: 대분류 → 중분류 → 소분류"),
                new TableRelation(
                        "cb_transaction",
                        "linked_tx_id",
                        "cb_transaction",
                        "id",
                        "FK (logical)",
                        "가계 공통지출 ↔ 개인장부 거래 연동"),
                new TableRelation(
                        "cb_transaction",
                        "category",
                        "cb_category",
                        "name",
                        "soft",
                        "거래 분류명 → 분류 항목 (book + tx_type 기준 매칭)"),
                new TableRelation(
                        "cb_transaction",
                        "card_name",
                        "cb_financial_product",
                        "name",
                        "soft",
                        "거래 카드명 → 금융상품(CARD)"),
                new TableRelation(
                        "cb_category_keyword",
                        "category_name",
                        "cb_category",
                        "name",
                        "soft",
                        "예약어 → 분류 항목명"),
                new TableRelation(
                        "cb_fixed_item",
                        "category",
                        "cb_category",
                        "name",
                        "soft",
                        "고정 항목 분류 → 분류 항목"),
                new TableRelation(
                        "cb_fixed_item",
                        "card_name",
                        "cb_financial_product",
                        "name",
                        "soft",
                        "고정 항목 카드 → 금융상품"),
                new TableRelation(
                        "*",
                        "book",
                        "—",
                        "LedgerBook",
                        "partition",
                        "모든 장부별 테이블의 파티션 키 (PERSONAL / HOUSEHOLD)"));
    }
}
