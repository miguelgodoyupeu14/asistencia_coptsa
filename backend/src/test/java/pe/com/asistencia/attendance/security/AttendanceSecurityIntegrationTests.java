package pe.com.asistencia.attendance.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.server.ResponseStatusException;
import pe.com.asistencia.attendance.domain.PunchType;
import pe.com.asistencia.employee.domain.Employee;
import pe.com.asistencia.employee.repository.EmployeeRepository;
import pe.com.asistencia.security.domain.RoleCode;
import pe.com.asistencia.security.domain.User;
import pe.com.asistencia.security.repository.RoleRepository;
import pe.com.asistencia.security.repository.UserRepository;

@SpringBootTest
class AttendanceSecurityIntegrationTests {
    @Autowired AttendanceSecurityService security;
    @Autowired JdbcClient jdbc;
    @Autowired EmployeeRepository employees;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;

    @Test
    void validatesQrStatesAndPersistsCompleteAttemptEvidence() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User creator = users.save(new User("qr-admin-" + suffix, "{noop}password",
            Set.of(roles.findByCodeAndActiveTrue(RoleCode.ADMIN).orElseThrow())));
        Employee employee = employees.save(new Employee("QR-" + suffix, "DNI", suffix,
            "QR", "Employee", null, null, null, LocalDate.now(), null));
        Long area = insertCatalog("areas", "QRA-" + suffix);
        Long position = insertCatalog("positions", "QRP-" + suffix);
        Long assignedSite = insertSite("QRS-" + suffix, true);
        Long otherSite = insertSite("QRO-" + suffix, true);
        jdbc.sql("insert into employee_assignments(employee_id,site_id,area_id,position_id,valid_from,active) values(:employee,:site,:area,:position,:date,true)")
            .param("employee", employee.getId()).param("site", assignedSite).param("area", area)
            .param("position", position).param("date", LocalDate.now()).update();

        String validCode = "valid-" + suffix;
        insertQr(assignedSite, creator.getId(), validCode, true, Instant.now().minusSeconds(5), Instant.now().plusSeconds(120));
        var validation = security.validate(employee.getId(), LocalDate.now(), PunchType.ENTRY,
            "127.0.0.1", null, null, null, validCode, null, "AUTOMATED_TEST");
        security.accepted(employee.getId(), validation, PunchType.ENTRY, "127.0.0.1",
            null, null, null, "AUTOMATED_TEST");
        assertThat(validation.siteId()).isEqualTo(assignedSite);

        assertRejected("QR_INVALID", () -> security.validate(employee.getId(), LocalDate.now(),
            PunchType.EXIT, "127.0.0.1", null, null, null, "unknown", null, "AUTOMATED_TEST"));
        String expired = "expired-" + suffix;
        insertQr(assignedSite, creator.getId(), expired, true, Instant.now().minusSeconds(120), Instant.now().minusSeconds(1));
        assertRejected("QR_EXPIRED", () -> security.validate(employee.getId(), LocalDate.now(),
            PunchType.EXIT, "127.0.0.1", null, null, null, expired, null, "AUTOMATED_TEST"));
        String replaced = "replaced-" + suffix;
        insertQr(assignedSite, creator.getId(), replaced, false, Instant.now().minusSeconds(5), Instant.now().plusSeconds(120));
        assertRejected("QR_INACTIVE", () -> security.validate(employee.getId(), LocalDate.now(),
            PunchType.EXIT, "127.0.0.1", null, null, null, replaced, null, "AUTOMATED_TEST"));
        String wrongSite = "wrong-" + suffix;
        insertQr(otherSite, creator.getId(), wrongSite, true, Instant.now().minusSeconds(5), Instant.now().plusSeconds(120));
        assertRejected("QR_SITE_MISMATCH", () -> security.validate(employee.getId(), LocalDate.now(),
            PunchType.EXIT, "127.0.0.1", null, null, null, wrongSite, null, "AUTOMATED_TEST"));

        var attempts = jdbc.sql("select site_id,punch_type,reason_code,correlation_id from punch_attempts where employee_id=:employee order by id")
            .param("employee", employee.getId()).query().listOfRows();
        assertThat(attempts).hasSize(5);
        assertThat(attempts).allSatisfy(row -> {
            assertThat(row.get("SITE_ID")).isEqualTo(assignedSite);
            assertThat(row.get("PUNCH_TYPE")).isNotNull();
            assertThat(row.get("CORRELATION_ID")).isNotNull();
        });
    }

    private Long insertCatalog(String table, String code) {
        jdbc.sql("insert into " + table + "(code,name,active) values(:code,:name,true)")
            .param("code", code).param("name", code).update();
        return jdbc.sql("select id from " + table + " where code=:code").param("code", code).query(Long.class).single();
    }

    private Long insertSite(String code, boolean requireQr) {
        jdbc.sql("insert into sites(code,name,time_zone,active,require_dynamic_qr) values(:code,:name,'America/Lima',true,:qr)")
            .param("code", code).param("name", code).param("qr", requireQr).update();
        return jdbc.sql("select id from sites where code=:code").param("code", code).query(Long.class).single();
    }

    private void insertQr(Long site, Long creator, String code, boolean active, Instant from, Instant to) {
        jdbc.sql("insert into dynamic_qr_challenges(site_id,code_hash,valid_from,expires_at,active,created_by) values(:site,:hash,:from,:to,:active,:creator)")
            .param("site", site).param("hash", AttendanceSecurityService.hash(code)).param("from", from)
            .param("to", to).param("active", active).param("creator", creator).update();
    }

    private void assertRejected(String code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getReason()).isEqualTo(code));
    }
}
