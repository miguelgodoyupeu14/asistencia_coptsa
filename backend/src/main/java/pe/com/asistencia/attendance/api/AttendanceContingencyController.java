package pe.com.asistencia.attendance.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.com.asistencia.attendance.domain.ContingencyReason;
import pe.com.asistencia.attendance.domain.PunchType;
import pe.com.asistencia.attendance.service.AttendanceContingencyService;

@RestController
@RequestMapping("/api/v1/attendance-contingencies")
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class AttendanceContingencyController {
    private final AttendanceContingencyService service;
    public AttendanceContingencyController(AttendanceContingencyService service) { this.service = service; }

    @GetMapping("/context")
    public AttendanceContingencyService.Context context(Authentication actor, @RequestParam Long employeeId) {
        return service.context(actor, employeeId);
    }

    @PostMapping("/punch")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceContingencyService.Result punch(Authentication actor, @Valid @RequestBody Request request) {
        return service.punch(actor, request.employeeId(), request.type(), request.reason(), request.comment());
    }

    public record Request(@NotNull Long employeeId, @NotNull PunchType type,
                          @NotNull ContingencyReason reason, @Size(max = 500) String comment) {}
}
