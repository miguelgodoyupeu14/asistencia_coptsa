package pe.com.asistencia.attendance.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.com.asistencia.attendance.config.AttendanceRulesProperties;
import pe.com.asistencia.attendance.domain.AttendanceDay;
import pe.com.asistencia.attendance.domain.ContingencyReason;
import pe.com.asistencia.attendance.domain.Punch;
import pe.com.asistencia.attendance.domain.PunchType;
import pe.com.asistencia.attendance.repository.AttendanceDayRepository;
import pe.com.asistencia.attendance.repository.PunchRepository;
import pe.com.asistencia.employee.repository.EmployeeRepository;
import pe.com.asistencia.schedule.domain.Shift;
import pe.com.asistencia.schedule.repository.ScheduleAssignmentRepository;
import pe.com.asistencia.security.repository.UserRepository;
import pe.com.asistencia.security.service.SupervisorScopeService;

@Service
@Transactional
public class AttendanceContingencyService {
    private final EmployeeRepository employees;
    private final ScheduleAssignmentRepository schedules;
    private final AttendanceDayRepository days;
    private final PunchRepository punches;
    private final UserRepository users;
    private final SupervisorScopeService scope;
    private final AttendanceRulesProperties rules;
    private final AttendanceCalendarService calendar;
    private final AttendanceRuleValidator validator = new AttendanceRuleValidator();
    private final JdbcClient jdbc;
    private final ZoneId zone;

    public AttendanceContingencyService(EmployeeRepository employees, ScheduleAssignmentRepository schedules,
                                        AttendanceDayRepository days, PunchRepository punches, UserRepository users,
                                        SupervisorScopeService scope, AttendanceRulesProperties rules, JdbcClient jdbc,
                                        AttendanceCalendarService calendar,
                                        @Value("${app.time-zone}") String zone) {
        this.employees = employees; this.schedules = schedules; this.days = days; this.punches = punches;
        this.users = users; this.scope = scope; this.rules = rules; this.jdbc = jdbc; this.calendar = calendar; this.zone = ZoneId.of(zone);
    }

    @Transactional(readOnly = true)
    public Context context(Authentication actor, Long employeeId) {
        Instant now = Instant.now();
        LocalDate date = now.atZone(zone).toLocalDate();
        var resolved = resolve(actor, employeeId, date);
        var day = days.findByEmployeeIdAndWorkDate(employeeId, date).orElse(null);
        var actions = new java.util.ArrayList<PunchType>();
        String blocking = null;
        PunchType candidate = day == null || day.getFirstEntryAt() == null ? PunchType.ENTRY
            : day.getLastExitAt() == null ? PunchType.EXIT : null;
        if (candidate == null) blocking = "ATTENDANCE_COMPLETED";
        else try { validator.validate(candidate, day, date, resolved.plan.startTime(), resolved.plan.endTime(), resolved.plan.crossesMidnight(), now, zone, rules); actions.add(candidate); }
        catch (AttendanceRuleValidator.Violation violation) { blocking = violation.code(); }
        return new Context(now, date, employeeId, resolved.employeeCode, resolved.employeeName, resolved.photoUrl,
            resolved.siteId, resolved.siteName, resolved.plan.startTime(), resolved.plan.endTime(),
            day == null ? null : day.getStatus().name(), List.copyOf(actions), blocking);
    }

    public Result punch(Authentication actor, Long employeeId, PunchType type, ContingencyReason reason, String comment) {
        if (reason == ContingencyReason.OTHER && (comment == null || comment.isBlank()))
            throw conflict("CONTINGENCY_COMMENT_REQUIRED");
        Instant effectiveAt = Instant.now();
        LocalDate date = effectiveAt.atZone(zone).toLocalDate();
        var resolved = resolve(actor, employeeId, date);
        var existing = days.findByEmployeeIdAndWorkDate(employeeId, date).orElse(null);
        try { validator.validate(type, existing, date, resolved.plan.startTime(), resolved.plan.endTime(), resolved.plan.crossesMidnight(), effectiveAt, zone, rules); }
        catch (AttendanceRuleValidator.Violation violation) { throw conflict(violation.code()); }
        var actorUser = users.findByUsernameIgnoreCase(actor.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ACTOR_NOT_FOUND"));
        var day = existing == null ? new AttendanceDay(resolved.employee, date, resolved.plan.shift()) : existing;
        day.apply(type, effectiveAt, zone, rules.entryToleranceMinutes(), rules.exitToleranceMinutes(), resolved.plan.startTime(), resolved.plan.endTime(), resolved.plan.crossesMidnight(), resolved.plan.breakMinutes());
        day = days.saveAndFlush(day);
        String channel = scope.isAdmin(actor) ? "MANUAL_ADMIN" : "MANUAL_SUPERVISOR";
        try {
            var punch = punches.saveAndFlush(new Punch(resolved.employee, day, effectiveAt, type, channel, null,
                null, null, null, null, null, resolved.siteId, actorUser.getId(), reason.name(), null));
            String auditReason = comment == null || comment.isBlank() ? reason.name() : reason.name() + ": " + comment.trim();
            jdbc.sql("""
                    insert into attendance_business_events(
                        attendance_day_id,punch_id,employee_id,actor_user_id,event_type,channel,
                        result_status,reason_code,detail,effective_at
                    ) values(:day,:punch,:employee,:actor,:type,:channel,'APPLIED',:reason,:detail,:effectiveAt)
                    """)
                .param("day", day.getId()).param("punch", punch.getId()).param("employee", employeeId)
                .param("actor", actorUser.getId())
                .param("type", type == PunchType.ENTRY ? "MANUAL_CHECK_IN" : "MANUAL_CHECK_OUT")
                .param("channel", channel).param("reason", reason.name()).param("detail", auditReason)
                .param("effectiveAt", effectiveAt).update();
            return new Result(day.getId(), punch.getId(), employeeId, type, channel, effectiveAt, Instant.now(), reason, comment);
        } catch (DataIntegrityViolationException exception) {
            throw conflict(type == PunchType.ENTRY ? "ALREADY_CHECKED_IN" : "ALREADY_CHECKED_OUT");
        }
    }

    private Resolved resolve(Authentication actor, Long employeeId, LocalDate date) {
        if (!scope.canAccess(actor, employeeId, date)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EMPLOYEE_OUT_OF_SCOPE");
        var employee = employees.findById(employeeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND"));
        if (!employee.isActive()) throw conflict("EMPLOYEE_INACTIVE");
        var sites = jdbc.sql("select s.id,s.name from employee_assignments ea join sites s on s.id=ea.site_id where ea.employee_id=:employee and ea.active=true and s.active=true and ea.valid_from<=:date and (ea.valid_to is null or ea.valid_to>=:date) and ea.assignment_type=(case when exists(select 1 from employee_assignments t where t.employee_id=ea.employee_id and t.assignment_type='TEMPORARY' and t.active=true and t.valid_from<=:date and (t.valid_to is null or t.valid_to>=:date)) then 'TEMPORARY' else 'PERMANENT' end)")
            .param("employee", employeeId).param("date", date)
            .query((rs, row) -> new Site(rs.getLong("id"), rs.getString("name"))).list();
        if (sites.isEmpty()) throw conflict("NO_ACTIVE_SITE_ASSIGNMENT");
        if (sites.size() > 1) throw conflict("MULTIPLE_ACTIVE_ASSIGNMENTS");
        var site = sites.getFirst();
        AttendanceCalendarService.Plan plan;
        try { plan = calendar.resolve(employeeId, site.id, date); }
        catch (AttendanceCalendarService.RuleViolation violation) { throw conflict(violation.code()); }
        return new Resolved(employee, employee.getCode(), employee.getFirstNames() + " " + employee.getLastNames(),
            employee.getPhotoUrl(), site.id, site.name, plan);
    }

    private static ResponseStatusException conflict(String code) { return new ResponseStatusException(HttpStatus.CONFLICT, code); }
    private record Site(Long id, String name) {}
    private record Resolved(pe.com.asistencia.employee.domain.Employee employee, String employeeCode,
                            String employeeName, String photoUrl, Long siteId, String siteName, AttendanceCalendarService.Plan plan) {}
    public record Context(Instant serverTime, LocalDate workDate, Long employeeId, String employeeCode,
                          String employeeName, String employeePhotoUrl, Long siteId, String siteName,
                          java.time.LocalTime startTime, java.time.LocalTime endTime, String attendanceStatus,
                          List<PunchType> allowedActions, String blockingReason) {}
    public record Result(Long attendanceId, Long punchId, Long employeeId, PunchType type, String channel,
                         Instant effectiveAt, Instant recordedAt, ContingencyReason reason, String comment) {}
}
