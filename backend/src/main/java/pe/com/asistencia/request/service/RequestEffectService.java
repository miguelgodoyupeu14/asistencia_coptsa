package pe.com.asistencia.request.service;

import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.com.asistencia.attendance.config.AttendanceRulesProperties;
import pe.com.asistencia.attendance.domain.AttendanceDay;
import pe.com.asistencia.attendance.domain.Punch;
import pe.com.asistencia.attendance.domain.PunchType;
import pe.com.asistencia.attendance.repository.AttendanceDayRepository;
import pe.com.asistencia.attendance.repository.PunchRepository;
import pe.com.asistencia.attendance.service.AttendanceCalendarService;
import pe.com.asistencia.request.domain.EmployeeRequest;
import pe.com.asistencia.request.domain.RequestType;
import pe.com.asistencia.security.domain.User;

@Service
public class RequestEffectService {
    private final AttendanceDayRepository days;
    private final PunchRepository punches;
    private final AttendanceRulesProperties rules;
    private final ZoneId zone;
    private final JdbcClient jdbc;
    private final AttendanceCalendarService calendar;

    public RequestEffectService(AttendanceDayRepository days, PunchRepository punches, AttendanceRulesProperties rules,
                                @Value("${app.time-zone}") String zone, JdbcClient jdbc, AttendanceCalendarService calendar) {
        this.days = days; this.punches = punches; this.rules = rules; this.zone = ZoneId.of(zone); this.jdbc = jdbc;
        this.calendar = calendar;
    }

    public void applyApproved(EmployeeRequest request, User reviewer) {
        if (request.getType() == RequestType.JUSTIFICATION) {
            applyExcuse(request, reviewer);
        } else if (request.getType() == RequestType.PERMISSION) {
            applyPermission(request, reviewer);
        } else if (request.getType() == RequestType.CORRECTION) {
            applyCorrection(request, reviewer);
        }
    }

    private void applyPermission(EmployeeRequest request, User reviewer) {
        if (request.getAffectedDate() == null || request.getRequestedFrom() == null || request.getRequestedTo() == null) {
            throw conflict("El permiso requiere fecha, inicio y fin");
        }
        jdbc.sql("insert into approved_permissions(request_id,employee_id,affected_date,requested_from,requested_to,approved_by) values(:request,:employee,:date,:from,:to,:reviewer)")
            .param("request", request.getId()).param("employee", request.getEmployee().getId())
            .param("date", request.getAffectedDate()).param("from", request.getRequestedFrom())
            .param("to", request.getRequestedTo()).param("reviewer", reviewer.getId()).update();
        days.findByEmployeeIdAndWorkDate(request.getEmployee().getId(), request.getAffectedDate())
            .filter(day -> day.getStatus() == pe.com.asistencia.attendance.domain.AttendanceStatus.ABSENT)
            .filter(day -> coversWholeShift(request, day))
            .ifPresent(day -> {
                String before = snapshot(day);
                day.markExcused();
                days.save(day);
                audit(request, day, reviewer, "PERMISSION", before, snapshot(day));
            });
    }

    private boolean coversWholeShift(EmployeeRequest request, AttendanceDay day) {
        if (day.getShift() == null) return true;
        var shift = day.getShift();
        var start = day.getWorkDate().atTime(shift.getStartTime()).atZone(zone).toInstant();
        var endDate = shift.isCrossesMidnight() ? day.getWorkDate().plusDays(1) : day.getWorkDate();
        var end = endDate.atTime(shift.getEndTime()).atZone(zone).toInstant();
        return !request.getRequestedFrom().isAfter(start) && !request.getRequestedTo().isBefore(end);
    }

    private void applyExcuse(EmployeeRequest request, User reviewer) {
        var day = requiredDay(request);
        String before = snapshot(day);
        try { day.markExcused(); }
        catch (IllegalStateException ex) { throw conflict(ex.getMessage()); }
        days.save(day);
        audit(request, day, reviewer, request.getType().name(), before, snapshot(day));
    }

    private void applyCorrection(EmployeeRequest request, User reviewer) {
        PunchType type = request.getProposedPunchType();
        var at = request.getProposedAt();
        if (type == null || at == null) throw conflict("CORRECTION_REQUIRES_TYPE_AND_TIME");
        var resolved = correctionDay(request);
        var day = resolved.day();
        if (type == PunchType.ENTRY && day.getFirstEntryAt() != null) throw conflict("La jornada ya tiene entrada");
        if (type == PunchType.EXIT && day.getFirstEntryAt() == null) throw conflict("No se puede corregir una salida sin entrada");
        if (type == PunchType.EXIT && at.isBefore(day.getFirstEntryAt())) throw conflict("La salida propuesta es anterior a la entrada");
        String before = snapshot(day);
        day.apply(type, at, zone, rules.entryToleranceMinutes(), rules.exitToleranceMinutes(), resolved.plan().startTime(),
            resolved.plan().endTime(), resolved.plan().crossesMidnight(), resolved.plan().breakMinutes());
        days.saveAndFlush(day);
        punches.saveAndFlush(new Punch(request.getEmployee(), day, at, type, "CORRECTION", null, null, null, null,
            null, null, resolved.siteId(), reviewer.getId(), request.getReason(), request.getId()));
        audit(request, day, reviewer, "CORRECTION", before, snapshot(day));
    }

    private CorrectionDay correctionDay(EmployeeRequest request) {
        if (request.getAffectedDate() == null) throw conflict("CORRECTION_DATE_REQUIRED");
        var existing = request.getAttendanceDay() != null ? request.getAttendanceDay()
            : days.findByEmployeeIdAndWorkDate(request.getEmployee().getId(), request.getAffectedDate()).orElse(null);
        var siteIds = jdbc.sql("select site_id from employee_assignments ea where employee_id=:employee and active=true and valid_from<=:date and (valid_to is null or valid_to>=:date) and assignment_type=(case when exists(select 1 from employee_assignments t where t.employee_id=ea.employee_id and t.assignment_type='TEMPORARY' and t.active=true and t.valid_from<=:date and (t.valid_to is null or t.valid_to>=:date)) then 'TEMPORARY' else 'PERMANENT' end)")
            .param("employee", request.getEmployee().getId()).param("date", request.getAffectedDate()).query(Long.class).list();
        if (existing != null && existing.getShift() != null) {
            var shift = existing.getShift();
            var plan = new AttendanceCalendarService.Plan(shift, shift.getStartTime(), shift.getEndTime(),
                shift.isCrossesMidnight(), shift.getBreakMinutes(), "HISTORICAL_SHIFT");
            return new CorrectionDay(existing, plan, siteIds.size() == 1 ? siteIds.getFirst() : null);
        }
        if (existing != null && siteIds.isEmpty()) {
            var reference = existing.getFirstEntryAt() == null ? request.getProposedAt() : existing.getFirstEntryAt();
            var start = reference.atZone(zone).toLocalTime();
            var end = request.getProposedAt().atZone(zone).toLocalTime();
            return new CorrectionDay(existing, new AttendanceCalendarService.Plan(null, start, end, end.isBefore(start), 0,
                "LEGACY_WITHOUT_SHIFT"), null);
        }
        if (siteIds.isEmpty()) throw conflict("NO_ACTIVE_SITE_ASSIGNMENT");
        if (siteIds.size() > 1) throw conflict("MULTIPLE_ACTIVE_ASSIGNMENTS");
        AttendanceCalendarService.Plan plan;
        try { plan = calendar.resolve(request.getEmployee().getId(), siteIds.getFirst(), request.getAffectedDate()); }
        catch (AttendanceCalendarService.RuleViolation violation) { throw conflict(violation.code()); }
        var day = existing != null ? existing
            : days.saveAndFlush(new AttendanceDay(request.getEmployee(), request.getAffectedDate(), plan.shift()));
        return new CorrectionDay(day, plan, siteIds.getFirst());
    }

    private AttendanceDay requiredDay(EmployeeRequest request) {
        if (request.getAttendanceDay() != null) return request.getAttendanceDay();
        if (request.getAffectedDate() == null) throw conflict("La solicitud requiere una fecha afectada");
        return days.findByEmployeeIdAndWorkDate(request.getEmployee().getId(), request.getAffectedDate())
            .orElseThrow(() -> conflict("No existe jornada para la fecha afectada"));
    }

    private void audit(EmployeeRequest request, AttendanceDay day, User reviewer, String type, String before, String after) {
        jdbc.sql("insert into attendance_adjustments(request_id,attendance_day_id,adjustment_type,previous_state,new_state,applied_by) values(:request,:day,:type,:before,:after,:reviewer)")
            .param("request", request.getId()).param("day", day.getId()).param("type", type)
            .param("before", before).param("after", after).param("reviewer", reviewer.getId()).update();
    }

    private static String snapshot(AttendanceDay day) {
        return "status=" + day.getStatus() + ";entry=" + day.getFirstEntryAt() + ";exit=" + day.getLastExitAt()
            + ";worked=" + day.getWorkedMinutes() + ";late=" + day.getLateMinutes() + ";early=" + day.getEarlyExitMinutes();
    }

    private static ResponseStatusException conflict(String detail) {
        return new ResponseStatusException(HttpStatus.CONFLICT, detail);
    }

    private record CorrectionDay(AttendanceDay day, AttendanceCalendarService.Plan plan, Long siteId) {}
}
