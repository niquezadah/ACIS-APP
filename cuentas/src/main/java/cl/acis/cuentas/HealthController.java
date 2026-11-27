package cl.acis.cuentas;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {
    private final JdbcTemplate database;

    HealthController(JdbcTemplate database) {
        this.database = database;
    }

    @GetMapping("/health")
    ResponseEntity<Void> health() {
        try {
            database.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok().build();
        } catch (DataAccessException e) {
            return ResponseEntity.status(503).build();
        }
    }
}
