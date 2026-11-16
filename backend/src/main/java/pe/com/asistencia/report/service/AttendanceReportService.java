package pe.com.asistencia.report.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttendanceReportService {
    private final JdbcClient jdbc;

    public AttendanceReportService(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<Summary> summary(LocalDate from, LocalDate to, Long employeeId, Long siteId, Long areaId, String supervisorUsername) {
        StringBuilder sql = new StringBuilder("select e.id,e.employee_code,e.first_names,e.last_names,e.photo_url from employees e where e.active=true");
        if (employeeId != null) sql.append(" and e.id=:employeeId");
        if (supervisorUsername != null) sql.append(" and exists (select 1 from supervisor_scopes ss join users su on su.id=ss.user_id join employee_assignments sea on sea.employee_id=e.id and sea.site_id=ss.site_id and (ss.area_id is null or ss.area_id=sea.area_id) where lower(su.username)=lower(:supervisor) and ss.active=true and sea.active=true and sea.valid_from<=:toDate and (sea.valid_to is null or sea.valid_to>=:fromDate))");
        if (siteId != null || areaId != null) {
            sql.append(" and exists (select 1 from employee_assignments ea where ea.employee_id=e.id and ea.active=true and ea.valid_from<=:toDate and (ea.valid_to is null or ea.valid_to>=:fromDate)");
            if (siteId != null) sql.append(" and ea.site_id=:siteId");
            if (areaId != null) sql.append(" and ea.area_id=:areaId");
            sql.append(")");
        }
        sql.append(" order by e.last_names,e.first_names");

        var query = jdbc.sql(sql.toString());
        if (employeeId != null) query = query.param("employeeId", employeeId);
        if (siteId != null || areaId != null || supervisorUsername != null) query = query.param("fromDate", from).param("toDate", to);
        if (supervisorUsername != null) query = query.param("supervisor", supervisorUsername);
        if (siteId != null) query = query.param("siteId", siteId);
        if (areaId != null) query = query.param("areaId", areaId);
        var employees = query.query((rs, row) -> new EmployeeRow(rs.getLong("id"),
            rs.getString("employee_code"), rs.getString("first_names") + " " + rs.getString("last_names"),
            rs.getString("photo_url"))).list();

        var result = new ArrayList<Summary>();
        for (var employee : employees) result.add(build(employee, from, to));
        return result;
    }

    private Summary build(EmployeeRow employee, LocalDate from, LocalDate to) {
        var attendance = jdbc.sql("""
            select count(*) recorded_days,
                   sum(case when status='COMPLETED' then 1 else 0 end) completed_days,
                   sum(case when status='ABSENT' then 1 else 0 end) absences,
                   sum(case when status='EXCUSED' then 1 else 0 end) excused_days,
                   sum(case when status in ('OPEN','INCOMPLETE') then 1 else 0 end) incomplete_days,
                   sum(case when late_minutes>0 then 1 else 0 end) late_days,
                   coalesce(sum(late_minutes),0) late_minutes,
                   sum(case when early_exit_minutes>0 then 1 else 0 end) early_exit_days,
                   coalesce(sum(early_exit_minutes),0) early_exit_minutes,
                   coalesce(sum(worked_minutes),0) worked_minutes
              from attendance_days
             where employee_id=:employee and work_date between :fromDate and :toDate
            """).param("employee", employee.id).param("fromDate", from).param("toDate", to)
            .query((rs, row) -> new Totals(rs.getInt("recorded_days"), rs.getInt("completed_days"),
                rs.getInt("absences"), rs.getInt("excused_days"), rs.getInt("incomplete_days"),
                rs.getInt("late_days"), rs.getInt("late_minutes"), rs.getInt("early_exit_days"),
                rs.getInt("early_exit_minutes"), rs.getInt("worked_minutes"))).single();

        var shifts = jdbc.sql("""
            select sa.valid_from,sa.valid_to,sh.day_of_week
              from schedule_assignments sa
              join schedules s on s.id=sa.schedule_id and s.active=true
              join shifts sh on sh.schedule_id=s.id
             where sa.employee_id=:employee and sa.active=true
               and sa.valid_from<=:toDate and (sa.valid_to is null or sa.valid_to>=:fromDate)
            """).param("employee", employee.id).param("fromDate", from).param("toDate", to)
            .query((rs, row) -> new ScheduledShift(rs.getObject("valid_from", LocalDate.class),
                rs.getObject("valid_to", LocalDate.class), rs.getInt("day_of_week"))).list();
        Set<LocalDate> scheduledDates = new HashSet<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            for (var shift : shifts) {
                if (!day.isBefore(shift.from) && (shift.to == null || !day.isAfter(shift.to))
                    && day.getDayOfWeek().getValue() == shift.dayOfWeek) {
                    scheduledDates.add(day);
                    break;
                }
            }
        }
        int punctualDays = Math.max(0, attendance.completedDays - attendance.lateDays);
        double punctuality = attendance.completedDays == 0 ? 0
            : Math.round((punctualDays * 10000.0) / attendance.completedDays) / 100.0;
        return new Summary(employee.id, employee.code, employee.name, employee.photoUrl,
            scheduledDates.size(), attendance.recordedDays, attendance.completedDays,
            attendance.absences, attendance.excusedDays, attendance.incompleteDays,
            attendance.lateDays, attendance.lateMinutes, attendance.earlyExitDays,
            attendance.earlyExitMinutes, attendance.workedMinutes, punctuality);
    }

    private record EmployeeRow(Long id, String code, String name, String photoUrl) {}
    private record ScheduledShift(LocalDate from, LocalDate to, int dayOfWeek) {}
    private record Totals(int recordedDays, int completedDays, int absences, int excusedDays,
                          int incompleteDays, int lateDays, int lateMinutes,
                          int earlyExitDays, int earlyExitMinutes, int workedMinutes) {}

    public record Summary(Long employeeId, String employeeCode, String employeeName, String photoUrl,
                          int scheduledDays, int recordedDays, int completedDays, int absences,
                          int excusedDays, int incompleteDays, int lateDays, int lateMinutes,
                          int earlyExitDays, int earlyExitMinutes, int workedMinutes,
                          double punctualityPercent) {}
}
