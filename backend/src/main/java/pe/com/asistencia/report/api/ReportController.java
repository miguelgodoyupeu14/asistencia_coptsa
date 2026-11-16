package pe.com.asistencia.report.api;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.asistencia.report.service.AttendanceReportService;
import pe.com.asistencia.security.service.SupervisorScopeService;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class ReportController {
    private final AttendanceReportService reports;
    private final SupervisorScopeService scopes;

    public ReportController(AttendanceReportService reports, SupervisorScopeService scopes) { this.reports = reports; this.scopes = scopes; }

    @GetMapping("/attendance-summary")
    public List<AttendanceReportService.Summary> summary(Authentication authentication,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long employeeId,
        @RequestParam(required = false) Long siteId,
        @RequestParam(required = false) Long areaId) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new IllegalArgumentException("El rango debe ser válido y no superar 366 días");
        }
        String supervisor = scopes.isAdmin(authentication) ? null : authentication.getName();
        return reports.summary(from, to, employeeId, siteId, areaId, supervisor);
    }
}
