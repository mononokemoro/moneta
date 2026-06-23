package com.pininicong.cashbook.api;

import com.pininicong.cashbook.dto.DatabaseAdminDto.BackupExport;
import com.pininicong.cashbook.dto.DatabaseAdminDto.DatabaseOverview;
import com.pininicong.cashbook.dto.DatabaseAdminDto.RestoreResult;
import com.pininicong.cashbook.dto.DatabaseAdminDto.SqlQueryRequest;
import com.pininicong.cashbook.dto.DatabaseAdminDto.SqlQueryResponse;
import com.pininicong.cashbook.dto.DatabaseAdminDto.TableRowsResponse;
import com.pininicong.cashbook.service.DatabaseAdminService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/database", produces = MediaType.APPLICATION_JSON_VALUE)
public class DatabaseAdminController {

    private final DatabaseAdminService adminService;

    public DatabaseAdminController(DatabaseAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public DatabaseOverview overview() {
        return adminService.getOverview();
    }

    @GetMapping("/tables/{table}/rows")
    public TableRowsResponse tableRows(
            @PathVariable String table,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return adminService.getTableRows(table, offset, limit);
    }

    @GetMapping("/backup")
    public ResponseEntity<BackupExport> downloadBackup() {
        BackupExport backup = adminService.exportBackup();
        String filename = "pininicong-cashbook-backup-" + backup.exportedAt().replace(":", "") + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(backup);
    }

    @PostMapping(value = "/restore", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestoreResult restore(@RequestBody BackupExport backup) {
        return adminService.restoreBackup(backup);
    }

    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SqlQueryResponse query(@RequestBody SqlQueryRequest req) {
        return adminService.executeQuery(req.sql(), req.limit());
    }
}
