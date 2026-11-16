package pe.com.asistencia.request.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import pe.com.asistencia.attendance.domain.*;
import pe.com.asistencia.attendance.repository.*;
import pe.com.asistencia.employee.domain.Employee;
import pe.com.asistencia.employee.repository.EmployeeRepository;
import pe.com.asistencia.request.domain.*;
import pe.com.asistencia.request.repository.EmployeeRequestRepository;
import pe.com.asistencia.security.domain.User;
import pe.com.asistencia.security.repository.UserRepository;

@SpringBootTest
@Transactional
class RequestEffectServiceIntegrationTests {
    @Autowired RequestEffectService effects;
    @Autowired UserRepository users;
    @Autowired EmployeeRepository employees;
    @Autowired AttendanceDayRepository days;
    @Autowired PunchRepository punches;
    @Autowired EmployeeRequestRepository requests;
    @Autowired JdbcClient jdbc;

    @Test void approvedExitCorrectionCompletesAndAuditsTheAttendanceDay() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var reviewer = users.save(new User("reviewer-" + suffix, "{noop}password", Set.of()));
        var employeeUser = users.save(new User("worker-" + suffix, "{noop}password", Set.of()));
        var employee = employees.save(new Employee("W-" + suffix, "DNI", suffix, "Prueba", "Corrección", null, null, null, LocalDate.now(), employeeUser));
        var date = LocalDate.now();
        var entry = date.atTime(8, 0).atZone(ZoneId.of("America/Lima")).toInstant();
        var exit = entry.plusSeconds(8 * 3600L);
        var day = new AttendanceDay(employee, date, null);
        day.apply(PunchType.ENTRY, entry, ZoneId.of("America/Lima"), 0, 0);
        day = days.save(day);
        var request = requests.save(new EmployeeRequest(employee, day, RequestType.CORRECTION, date, "Olvidé marcar salida", null, null, PunchType.EXIT, exit, "Salida propuesta", null));
        request.review(reviewer, RequestStatus.APPROVED, "Conforme");

        effects.applyApproved(request, reviewer);
        days.flush();

        var updated = days.findById(day.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AttendanceStatus.COMPLETED);
        assertThat(updated.getWorkedMinutes()).isEqualTo(480);
        assertThat(punches.countByAttendanceDayIdAndType(day.getId(), PunchType.EXIT)).isEqualTo(1);
        assertThat(jdbc.sql("select count(*) from attendance_adjustments where request_id=:request").param("request", request.getId()).query(Integer.class).single()).isEqualTo(1);
    }

    @Test void approvedFuturePermissionIsPersistedWithoutRequiringAnAttendanceDay() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var reviewer = users.save(new User("reviewer-perm-" + suffix, "{noop}password", Set.of()));
        var employee = employees.save(new Employee("P-" + suffix, "DNI", "P" + suffix,
            "Prueba", "Permiso", null, null, null, LocalDate.now(), null));
        var date = LocalDate.now().plusDays(2);
        var from = date.atStartOfDay(ZoneId.of("America/Lima")).toInstant();
        var to = date.plusDays(1).atStartOfDay(ZoneId.of("America/Lima")).toInstant();
        var request = requests.save(new EmployeeRequest(employee, null, RequestType.PERMISSION, date,
            "Permiso de jornada completa", from, to, null, null, null, null));
        request.review(reviewer, RequestStatus.APPROVED, "Conforme");

        effects.applyApproved(request, reviewer);

        assertThat(jdbc.sql("select count(*) from approved_permissions where request_id=:request")
            .param("request", request.getId()).query(Integer.class).single()).isEqualTo(1);
        assertThat(days.findByEmployeeIdAndWorkDate(employee.getId(), date)).isEmpty();
    }
}
