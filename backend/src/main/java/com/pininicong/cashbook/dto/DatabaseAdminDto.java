package com.pininicong.cashbook.dto;

import java.util.List;
import java.util.Map;

public final class DatabaseAdminDto {

    private DatabaseAdminDto() {}

    public record ColumnInfo(
            String name,
            String description,
            String type,
            Integer maxLength,
            boolean nullable,
            String defaultValue,
            boolean primaryKey) {}

    public record TableRelation(
            String fromTable,
            String fromColumn,
            String toTable,
            String toColumn,
            String kind,
            String description) {}

    public record TableSummary(
            String name,
            String description,
            String entity,
            long rowCount,
            List<ColumnInfo> columns) {}

    public record DatabaseOverview(
            String databaseProduct,
            String jdbcUrl,
            String schema,
            long totalRows,
            List<TableSummary> tables,
            List<TableRelation> relations) {}

    public record TableRowsResponse(
            String table,
            long total,
            int offset,
            int limit,
            List<String> columns,
            List<Map<String, Object>> rows) {}

    public record BackupExport(
            int version,
            String exportedAt,
            String databaseProduct,
            Map<String, BackupTable> tables) {}

    public record BackupTable(List<String> columns, List<Map<String, Object>> rows) {}

    public record RestoreResult(
            String restoredAt,
            Map<String, Integer> rowCounts,
            long totalRows) {}
}
