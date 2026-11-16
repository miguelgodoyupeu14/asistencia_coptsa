package pe.com.asistencia.employee.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/employee-assignments")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeAssignmentController {
    private final JdbcClient jdbc;
    public EmployeeAssignmentController(JdbcClient jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam Long employeeId) {
        return jdbc.sql("""
                select ea.id,ea.employee_id,ea.site_id,s.name site_name,ea.area_id,a.name area_name,
                       ea.position_id,p.name position_name,ea.valid_from,ea.valid_to,
                       ea.assignment_type,ea.assignment_reason,ea.active
                  from employee_assignments ea
                  join sites s on s.id=ea.site_id
                  join areas a on a.id=ea.area_id
                  join positions p on p.id=ea.position_id
                 where ea.employee_id=:employee
                 order by ea.valid_from desc
                """).param("employee", employeeId).query().listOfRows();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody Request request) {
        if (request.validTo() != null && request.validTo().isBefore(request.validFrom()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_ASSIGNMENT_RANGE");
        if ("TEMPORARY".equals(request.assignmentType())
            && (request.validTo() == null || request.reason() == null || request.reason().isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TEMPORARY_ASSIGNMENT_REQUIRES_END_AND_REASON");
        int overlaps = jdbc.sql("""
                select count(*) from employee_assignments
                 where employee_id=:employee and assignment_type=:type and active=true
                   and valid_from<=coalesce(:toDate,valid_from)
                   and (valid_to is null or valid_to>=:fromDate)
                """).param("employee", request.employeeId()).param("type", request.assignmentType())
            .param("toDate", request.validTo()).param("fromDate", request.validFrom()).query(Integer.class).single();
        if (overlaps > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "ASSIGNMENT_OVERLAP");
        jdbc.sql("""
                insert into employee_assignments(employee_id,site_id,area_id,position_id,valid_from,valid_to,
                    assignment_type,assignment_reason,active)
                values(:employee,:site,:area,:position,:fromDate,:toDate,:type,:reason,true)
                """).param("employee", request.employeeId()).param("site", request.siteId())
            .param("area", request.areaId()).param("position", request.positionId())
            .param("fromDate", request.validFrom()).param("toDate", request.validTo())
            .param("type", request.assignmentType()).param("reason", request.reason()).update();
        return Map.of("status", "CREATED");
    }

    @PatchMapping("/{id}/close")
    public Map<String, Object> close(@PathVariable Long id, @Valid @RequestBody CloseRequest request) {
        var from = jdbc.sql("select valid_from from employee_assignments where id=:id").param("id", id)
            .query(LocalDate.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ASSIGNMENT_NOT_FOUND"));
        if (request.validTo().isBefore(from))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_ASSIGNMENT_END");
        jdbc.sql("update employee_assignments set valid_to=:toDate where id=:id")
            .param("toDate", request.validTo()).param("id", id).update();
        return Map.of("status", "CLOSED");
    }

    public record Request(@NotNull Long employeeId, @NotNull Long siteId, @NotNull Long areaId,
                          @NotNull Long positionId, @NotNull LocalDate validFrom, LocalDate validTo,
                          @NotBlank @Pattern(regexp = "PERMANENT|TEMPORARY") String assignmentType,
                          @Size(max = 500) String reason) {}
    public record CloseRequest(@NotNull LocalDate validTo) {}
}
