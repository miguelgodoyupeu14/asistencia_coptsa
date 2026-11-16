package pe.com.asistencia.request.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.asistencia.employee.domain.Employee;
import pe.com.asistencia.employee.repository.EmployeeRepository;
import pe.com.asistencia.attendance.domain.AttendanceDay;
import pe.com.asistencia.attendance.domain.AttendanceStatus;
import pe.com.asistencia.attendance.repository.AttendanceDayRepository;
import pe.com.asistencia.request.domain.EmployeeRequest;
import pe.com.asistencia.request.domain.RequestType;
import pe.com.asistencia.request.repository.EmployeeRequestRepository;
import pe.com.asistencia.security.domain.RoleCode;
import pe.com.asistencia.security.domain.User;
import pe.com.asistencia.security.repository.RoleRepository;
import pe.com.asistencia.security.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeRequestReviewIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired EmployeeRepository employees;
    @Autowired EmployeeRequestRepository requests;
    @Autowired AttendanceDayRepository attendanceDays;
    @Autowired JdbcClient jdbc;

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "REJECTED"})
    @WithMockUser(username = "supervisor-review-test", roles = "SUPERVISOR")
    void supervisorCanResolveARequestAndReceiveTheUpdatedView(String decision) throws Exception {
        var supervisor = users.findByUsernameIgnoreCase("supervisor-review-test").orElseGet(() ->
            users.save(new User("supervisor-review-test", "{noop}password",
                Set.of(roles.findByCodeAndActiveTrue(RoleCode.SUPERVISOR).orElseThrow()))));
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var employeeUser = users.save(new User("employee-" + suffix, "{noop}password",
            Set.of(roles.findByCodeAndActiveTrue(RoleCode.EMPLOYEE).orElseThrow())));
        var employee = employees.save(new Employee("E-" + suffix, "DNI", suffix,
            "Empleado", "Prueba", null, "employee-" + suffix + "@test.local", null,
            LocalDate.now(), employeeUser));
        jdbc.sql("insert into sites(code,name,time_zone,active) values(:code,:name,'America/Lima',true)")
            .param("code", "S-" + suffix).param("name", "Sede " + suffix).update();
        jdbc.sql("insert into areas(code,name,active) values(:code,:name,true)")
            .param("code", "A-" + suffix).param("name", "Área " + suffix).update();
        jdbc.sql("insert into positions(code,name,active) values(:code,:name,true)")
            .param("code", "P-" + suffix).param("name", "Cargo " + suffix).update();
        Long siteId = jdbc.sql("select id from sites where code=:code").param("code", "S-" + suffix).query(Long.class).single();
        Long areaId = jdbc.sql("select id from areas where code=:code").param("code", "A-" + suffix).query(Long.class).single();
        Long positionId = jdbc.sql("select id from positions where code=:code").param("code", "P-" + suffix).query(Long.class).single();
        jdbc.sql("insert into employee_assignments(employee_id,site_id,area_id,position_id,valid_from,active) values(:employee,:site,:area,:position,:date,true)")
            .param("employee", employee.getId()).param("site", siteId).param("area", areaId)
            .param("position", positionId).param("date", LocalDate.now()).update();
        jdbc.sql("insert into supervisor_scopes(user_id,site_id,area_id,active) values(:user,:site,:area,true)")
            .param("user", supervisor.getId()).param("site", siteId).param("area", areaId).update();
        var attendance = new AttendanceDay(employee, LocalDate.now(), null);
        attendance.markAbsent();
        attendance = attendanceDays.save(attendance);
        var request = requests.save(new EmployeeRequest(employee, attendance, RequestType.JUSTIFICATION,
            LocalDate.now(), "Solicitud de prueba", null, null, null, null, null, null));

        mvc.perform(patch("/api/v1/requests/{id}/review", request.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\",\"comment\":\"Revisado\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(decision))
            .andExpect(jsonPath("$.reviewedBy").value(supervisor.getUsername()));

        mvc.perform(get("/api/v1/requests/me")
                .with(user(employeeUser.getUsername()).roles("EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value(decision))
            .andExpect(jsonPath("$[0].reviewedBy").value(supervisor.getUsername()));

        mvc.perform(get("/api/v1/requests").param("status", decision))
            .andExpect(status().isOk());

        var resultingDay = attendanceDays.findById(attendance.getId()).orElseThrow();
        if (decision.equals("APPROVED")) {
            org.assertj.core.api.Assertions.assertThat(resultingDay.getStatus()).isEqualTo(AttendanceStatus.EXCUSED);
        } else {
            org.assertj.core.api.Assertions.assertThat(resultingDay.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        }
    }
}
