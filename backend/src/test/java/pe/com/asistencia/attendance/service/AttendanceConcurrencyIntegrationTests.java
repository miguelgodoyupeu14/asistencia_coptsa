package pe.com.asistencia.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import pe.com.asistencia.attendance.domain.PunchType;
import pe.com.asistencia.employee.domain.Employee;
import pe.com.asistencia.employee.repository.EmployeeRepository;
import pe.com.asistencia.security.domain.RoleCode;
import pe.com.asistencia.security.domain.User;
import pe.com.asistencia.security.repository.RoleRepository;
import pe.com.asistencia.security.repository.UserRepository;

@SpringBootTest
class AttendanceConcurrencyIntegrationTests {
    @Autowired AttendanceService attendance;
    @Autowired JdbcClient jdbc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired EmployeeRepository employees;

    @Test
    void twoSimultaneousEntriesProduceOnlyOnePunch() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        LocalDate date = LocalDate.now(ZoneId.of("America/Lima"));
        LocalTime now = LocalTime.now(ZoneId.of("America/Lima"));
        User user = users.save(new User("concurrent-" + suffix, "{noop}password",
            Set.of(roles.findByCodeAndActiveTrue(RoleCode.EMPLOYEE).orElseThrow())));
        Employee employee = employees.save(new Employee("CON-" + suffix, "DNI", "C" + suffix,
            "Concurrent", "Employee", null, null, null, date.minusMonths(1), user));
        Long area = catalog("areas", "CA-" + suffix);
        Long position = catalog("positions", "CP-" + suffix);
        jdbc.sql("insert into sites(code,name,time_zone,active) values(:code,:name,'America/Lima',true)")
            .param("code", "CS-" + suffix).param("name", "Site " + suffix).update();
        Long site = jdbc.sql("select id from sites where code=:code").param("code", "CS-" + suffix).query(Long.class).single();
        jdbc.sql("insert into employee_assignments(employee_id,site_id,area_id,position_id,valid_from,active) values(:employee,:site,:area,:position,:date,true)")
            .param("employee", employee.getId()).param("site", site).param("area", area).param("position", position).param("date", date).update();
        jdbc.sql("insert into schedules(code,name,active) values(:code,:name,true)")
            .param("code", "CSC-" + suffix).param("name", "Schedule " + suffix).update();
        Long schedule = jdbc.sql("select id from schedules where code=:code").param("code", "CSC-" + suffix).query(Long.class).single();
        LocalTime start = now.minusMinutes(10);
        LocalTime end = now.plusHours(1);
        boolean crosses = end.isBefore(start);
        jdbc.sql("insert into shifts(schedule_id,day_of_week,start_time,end_time,crosses_midnight,break_minutes) values(:schedule,:day,:start,:end,:crosses,0)")
            .param("schedule", schedule).param("day", date.getDayOfWeek().getValue()).param("start", start)
            .param("end", end).param("crosses", crosses).update();
        jdbc.sql("insert into schedule_assignments(employee_id,schedule_id,valid_from,active) values(:employee,:schedule,:date,true)")
            .param("employee", employee.getId()).param("schedule", schedule).param("date", date).update();

        var ready = new CountDownLatch(2);
        var startGate = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> punch(user.getUsername(), ready, startGate));
            var second = executor.submit(() -> punch(user.getUsername(), ready, startGate));
            ready.await();
            startGate.countDown();
            var outcomes = List.of(first.get(), second.get());
            assertThat(outcomes).containsExactlyInAnyOrder("OK", "ALREADY_CHECKED_IN");
        }
        Integer punches = jdbc.sql("select count(*) from punches where employee_id=:employee and punch_type='ENTRY'")
            .param("employee", employee.getId()).query(Integer.class).single();
        assertThat(punches).isEqualTo(1);
    }

    private String punch(String username, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            attendance.punch(username, PunchType.ENTRY, "AUTOMATED_TEST", "127.0.0.1",
                null, null, null, null, null);
            return "OK";
        } catch (Exception exception) {
            if (exception instanceof org.springframework.web.server.ResponseStatusException status) return status.getReason();
            return exception.getClass().getSimpleName();
        }
    }

    private Long catalog(String table, String code) {
        jdbc.sql("insert into " + table + "(code,name,active) values(:code,:name,true)")
            .param("code", code).param("name", code).update();
        return jdbc.sql("select id from " + table + " where code=:code").param("code", code).query(Long.class).single();
    }
}
