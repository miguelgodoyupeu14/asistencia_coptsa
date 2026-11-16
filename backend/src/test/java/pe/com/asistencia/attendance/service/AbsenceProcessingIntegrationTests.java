package pe.com.asistencia.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import pe.com.asistencia.attendance.domain.AttendanceStatus;
import pe.com.asistencia.attendance.repository.AttendanceDayRepository;
import pe.com.asistencia.employee.domain.Employee;
import pe.com.asistencia.employee.repository.EmployeeRepository;
import pe.com.asistencia.request.domain.EmployeeRequest;
import pe.com.asistencia.request.domain.RequestStatus;
import pe.com.asistencia.request.domain.RequestType;
import pe.com.asistencia.request.repository.EmployeeRequestRepository;
import pe.com.asistencia.request.service.RequestEffectService;
import pe.com.asistencia.security.domain.User;
import pe.com.asistencia.security.repository.UserRepository;

@SpringBootTest
class AbsenceProcessingIntegrationTests {
    @Autowired AttendanceService attendance;
    @Autowired AttendanceDayRepository days;
    @Autowired EmployeeRepository employees;
    @Autowired JdbcClient jdbc;
    @Autowired UserRepository users;
    @Autowired EmployeeRequestRepository requests;
    @Autowired RequestEffectService effects;

    @Test
    void closesScheduledDayAsAbsentCreatesOneIncidentAndIsIdempotent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        LocalDate date = LocalDate.now().minusDays(1);
        Employee employee = employees.save(new Employee("ABS-" + suffix, "DNI", "A" + suffix,
            "Absent", "Employee", null, null, null, date.minusMonths(1), null));
        jdbc.sql("insert into schedules(code,name,active) values(:code,:name,true)")
            .param("code", "SCH-" + suffix).param("name", "Schedule " + suffix).update();
        Long schedule = jdbc.sql("select id from schedules where code=:code")
            .param("code", "SCH-" + suffix).query(Long.class).single();
        jdbc.sql("insert into shifts(schedule_id,day_of_week,start_time,end_time,crosses_midnight,break_minutes) values(:schedule,:day,'00:00:00','00:01:00',false,0)")
            .param("schedule", schedule).param("day", date.getDayOfWeek().getValue()).update();
        jdbc.sql("insert into schedule_assignments(employee_id,schedule_id,valid_from,active) values(:employee,:schedule,:date,true)")
            .param("employee", employee.getId()).param("schedule", schedule).param("date", date).update();

        assertThat(attendance.processAbsences(date)).isEqualTo(1);
        assertThat(attendance.processAbsences(date)).isZero();
        var day = days.findByEmployeeIdAndWorkDate(employee.getId(), date).orElseThrow();
        assertThat(day.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(jdbc.sql("select count(*) from incidents where attendance_day_id=:day and incident_type='ABSENCE' and source='AUTOMATIC'")
            .param("day", day.getId()).query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void fullShiftPermissionClosesDayAsExcusedWithoutAbsenceIncident() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        LocalDate date = LocalDate.now().minusDays(1);
        var reviewer = users.save(new User("perm-reviewer-" + suffix, "{noop}password", Set.of()));
        Employee employee = employees.save(new Employee("EXC-" + suffix, "DNI", "E" + suffix,
            "Excused", "Employee", null, null, null, date.minusMonths(1), null));
        jdbc.sql("insert into schedules(code,name,active) values(:code,:name,true)")
            .param("code", "EXC-SCH-" + suffix).param("name", "Excused schedule " + suffix).update();
        Long schedule = jdbc.sql("select id from schedules where code=:code")
            .param("code", "EXC-SCH-" + suffix).query(Long.class).single();
        jdbc.sql("insert into shifts(schedule_id,day_of_week,start_time,end_time,crosses_midnight,break_minutes) values(:schedule,:day,'00:00:00','00:01:00',false,0)")
            .param("schedule", schedule).param("day", date.getDayOfWeek().getValue()).update();
        jdbc.sql("insert into schedule_assignments(employee_id,schedule_id,valid_from,active) values(:employee,:schedule,:date,true)")
            .param("employee", employee.getId()).param("schedule", schedule).param("date", date).update();
        var from = date.atStartOfDay(ZoneId.of("America/Lima")).toInstant();
        var to = date.plusDays(1).atStartOfDay(ZoneId.of("America/Lima")).toInstant();
        var request = requests.save(new EmployeeRequest(employee, null, RequestType.PERMISSION, date,
            "Permiso completo", from, to, null, null, null, null));
        request.review(reviewer, RequestStatus.APPROVED, "Conforme");
        effects.applyApproved(request, reviewer);

        assertThat(attendance.processAbsences(date)).isEqualTo(1);
        var day = days.findByEmployeeIdAndWorkDate(employee.getId(), date).orElseThrow();
        assertThat(day.getStatus()).isEqualTo(AttendanceStatus.EXCUSED);
        assertThat(jdbc.sql("select count(*) from incidents where attendance_day_id=:day and incident_type='ABSENCE'")
            .param("day", day.getId()).query(Integer.class).single()).isZero();
    }

    @Test
    void holidayDoesNotCreateFalseAbsence() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        LocalDate date = LocalDate.now().minusDays(1);
        var admin = users.save(new User("holiday-admin-" + suffix, "{noop}password", Set.of()));
        Employee employee = employees.save(new Employee("HOL-" + suffix, "DNI", "H" + suffix,
            "Holiday", "Employee", null, null, null, date.minusMonths(1), null));
        jdbc.sql("insert into areas(code,name,active) values(:code,:code,true)").param("code", "A-" + suffix).update();
        jdbc.sql("insert into positions(code,name,active) values(:code,:code,true)").param("code", "P-" + suffix).update();
        jdbc.sql("insert into sites(code,name,time_zone,active) values(:code,:code,'America/Lima',true)").param("code", "S-" + suffix).update();
        Long area = jdbc.sql("select id from areas where code=:code").param("code", "A-" + suffix).query(Long.class).single();
        Long position = jdbc.sql("select id from positions where code=:code").param("code", "P-" + suffix).query(Long.class).single();
        Long site = jdbc.sql("select id from sites where code=:code").param("code", "S-" + suffix).query(Long.class).single();
        jdbc.sql("insert into employee_assignments(employee_id,site_id,area_id,position_id,valid_from,active) values(:employee,:site,:area,:position,:date,true)")
            .param("employee", employee.getId()).param("site", site).param("area", area).param("position", position).param("date", date.minusDays(2)).update();
        jdbc.sql("insert into schedules(code,name,active) values(:code,:code,true)").param("code", "HS-" + suffix).update();
        Long schedule = jdbc.sql("select id from schedules where code=:code").param("code", "HS-" + suffix).query(Long.class).single();
        jdbc.sql("insert into shifts(schedule_id,day_of_week,start_time,end_time,crosses_midnight,break_minutes) values(:schedule,:day,'00:00:00','00:01:00',false,0)")
            .param("schedule", schedule).param("day", date.getDayOfWeek().getValue()).update();
        jdbc.sql("insert into schedule_assignments(employee_id,schedule_id,valid_from,active) values(:employee,:schedule,:date,true)")
            .param("employee", employee.getId()).param("schedule", schedule).param("date", date.minusDays(2)).update();
        jdbc.sql("insert into attendance_calendar(site_id,calendar_date,day_type,description,active,created_by) values(:site,:date,'HOLIDAY','Feriado de prueba',true,:admin)")
            .param("site", site).param("date", date).param("admin", admin.getId()).update();

        assertThat(attendance.processAbsences(date)).isZero();
        assertThat(days.findByEmployeeIdAndWorkDate(employee.getId(), date)).isEmpty();
    }
}
