package pe.com.asistencia.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import pe.com.asistencia.attendance.domain.ContingencyReason;
import pe.com.asistencia.attendance.domain.PunchType;
import pe.com.asistencia.employee.domain.Employee;
import pe.com.asistencia.employee.repository.EmployeeRepository;
import pe.com.asistencia.security.domain.User;
import pe.com.asistencia.security.repository.UserRepository;

@SpringBootTest
@Transactional
class AttendanceContingencyIntegrationTests {
    @Autowired AttendanceContingencyService service;
    @Autowired UserRepository users;
    @Autowired EmployeeRepository employees;
    @Autowired JdbcClient jdbc;

    @Test
    void adminContingencyCreatesClearlyAttributedAndAuditedPunch() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        LocalDate today = LocalDate.now(ZoneId.of("America/Lima"));
        LocalTime now = LocalTime.now(ZoneId.of("America/Lima")).withSecond(0).withNano(0);
        LocalTime start = now.minusMinutes(10);
        LocalTime end = now.plusHours(1);
        var admin = users.save(new User("cont-admin-" + suffix, "{noop}password", Set.of()));
        Employee employee = employees.save(new Employee("CONT-" + suffix, "DNI", "C" + suffix,
            "Empleado", "Contingencia", null, null, null, today.minusMonths(1), null));
        jdbc.sql("insert into areas(code,name,active) values(:code,:code,true)").param("code", "CA-" + suffix).update();
        jdbc.sql("insert into positions(code,name,active) values(:code,:code,true)").param("code", "CP-" + suffix).update();
        jdbc.sql("insert into sites(code,name,time_zone,active) values(:code,:code,'America/Lima',true)").param("code", "CS-" + suffix).update();
        Long area = id("areas", "CA-" + suffix), position = id("positions", "CP-" + suffix), site = id("sites", "CS-" + suffix);
        jdbc.sql("insert into employee_assignments(employee_id,site_id,area_id,position_id,valid_from,active) values(:employee,:site,:area,:position,:date,true)")
            .param("employee", employee.getId()).param("site", site).param("area", area).param("position", position).param("date", today.minusDays(1)).update();
        jdbc.sql("insert into schedules(code,name,active) values(:code,:code,true)").param("code", "CC-" + suffix).update();
        Long schedule = id("schedules", "CC-" + suffix);
        jdbc.sql("insert into shifts(schedule_id,day_of_week,start_time,end_time,crosses_midnight,break_minutes) values(:schedule,:day,:start,:end,false,0)")
            .param("schedule", schedule).param("day", today.getDayOfWeek().getValue()).param("start", start).param("end", end).update();
        jdbc.sql("insert into schedule_assignments(employee_id,schedule_id,valid_from,active) values(:employee,:schedule,:date,true)")
            .param("employee", employee.getId()).param("schedule", schedule).param("date", today.minusDays(1)).update();
        var actor = new UsernamePasswordAuthenticationToken(admin.getUsername(), "n/a",
            Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var result = service.punch(actor, employee.getId(), PunchType.ENTRY, ContingencyReason.DEVICE_BATTERY, null);

        assertThat(result.channel()).isEqualTo("MANUAL_ADMIN");
        var punch = jdbc.sql("select channel,created_by,reason_code,site_id from punches where id=:id")
            .param("id", result.punchId()).query().singleRow();
        assertThat(punch.get("CHANNEL")).isEqualTo("MANUAL_ADMIN");
        assertThat(punch.get("CREATED_BY")).isEqualTo(admin.getId());
        assertThat(punch.get("REASON_CODE")).isEqualTo("DEVICE_BATTERY");
        assertThat(punch.get("SITE_ID")).isEqualTo(site);
        assertThat(jdbc.sql("select count(*) from attendance_business_events where punch_id=:punch and event_type='MANUAL_CHECK_IN' and result_status='APPLIED'")
            .param("punch", result.punchId()).query(Integer.class).single()).isEqualTo(1);
    }

    private Long id(String table, String code) {
        return jdbc.sql("select id from " + table + " where code=:code").param("code", code).query(Long.class).single();
    }
}
