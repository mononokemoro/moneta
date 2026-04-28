package com.pininicong.cashbook.api;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiHealthController {

    private final DataSource dataSource;

    public ApiHealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("application", "pininicong-cashbook");
        try (Connection c = dataSource.getConnection()) {
            body.put("database", c.isValid(3) ? "CONNECTED" : "UNKNOWN");
        } catch (Exception e) {
            body.put("database", "ERROR");
            body.put("databaseMessage", e.getMessage());
        }
        return body;
    }
}
