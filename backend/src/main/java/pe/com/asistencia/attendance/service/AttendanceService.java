package pe.com.asistencia.attendance.service;
import org.springframework.beans.factory.annotation.Value; import org.springframework.boot.context.properties.EnableConfigurationProperties; import org.springframework.http.HttpStatus; import org.springframework.jdbc.core.simple.JdbcClient; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.server.ResponseStatusException; import pe.com.asistencia.attendance.config.AttendanceRulesProperties; import pe.com.asistencia.attendance.domain.*; import pe.com.asistencia.attendance.repository.*; import pe.com.asistencia.attendance.security.AttendanceSecurityService; import pe.com.asistencia.employee.repository.EmployeeRepository; import pe.com.asistencia.schedule.domain.Shift; import pe.com.asistencia.schedule.repository.ScheduleAssignmentRepository; import pe.com.asistencia.request.domain.Incident; import pe.com.asistencia.request.repository.IncidentRepository; import java.time.*;
@Service @Transactional
@EnableConfigurationProperties(AttendanceRulesProperties.class)
public class AttendanceService {
 private final EmployeeRepository employees; private final ScheduleAssignmentRepository assignments; private final AttendanceDayRepository days; private final PunchRepository punches; private final IncidentRepository incidents; private final ZoneId zone; private final AttendanceRulesProperties rules; private final AttendanceSecurityService security; private final JdbcClient jdbc; private final AttendanceCalendarService calendar; private final AttendanceRuleValidator validator=new AttendanceRuleValidator();
 public AttendanceService(EmployeeRepository e,ScheduleAssignmentRepository a,AttendanceDayRepository d,PunchRepository p,IncidentRepository incidents,@Value("${app.time-zone}") String zone,AttendanceRulesProperties rules,AttendanceSecurityService security,JdbcClient jdbc,AttendanceCalendarService calendar){employees=e;assignments=a;days=d;punches=p;this.incidents=incidents;this.zone=ZoneId.of(zone);this.rules=rules;this.security=security;this.jdbc=jdbc;this.calendar=calendar;}
 public Result punch(String username,PunchType type,String channel,String ip,String device,Double lat,Double lon,String qr,String wifiBssid){return punch(username,type,channel,ip,device,lat,lon,0.0,qr,wifiBssid);}
  public Result punch(String username,PunchType type,String channel,String ip,String device,Double lat,Double lon,Double accuracyMeters,String qr,String wifiBssid){
  var employee=employees.findByUserUsernameIgnoreCase(username).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"EMPLOYEE_NOT_LINKED"));
  if(!employee.isActive())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"EMPLOYEE_INACTIVE");
   Instant now=Instant.now();LocalDate calendarDate=now.atZone(zone).toLocalDate();
   LocalDate date=resolveWorkDate(employee.getId(),type,calendarDate);
   var validation=security.validate(employee.getId(),date,type,ip,device,lat,lon,accuracyMeters,qr,wifiBssid,channel);
  AttendanceCalendarService.Plan plan;
  try{plan=calendar.resolve(employee.getId(),validation.siteId(),date);}catch(AttendanceCalendarService.RuleViolation ex){return reject(employee.getId(),validation.siteId(),type,ex.code(),ip,device,lat,lon,accuracyMeters,wifiBssid,channel);}
  var existing=days.findByEmployeeIdAndWorkDate(employee.getId(),date).orElse(null);
  try{validator.validate(type,existing,date,plan.startTime(),plan.endTime(),plan.crossesMidnight(),now,zone,rules);}catch(AttendanceRuleValidator.Violation ex){return reject(employee.getId(),validation.siteId(),type,ex.code(),ip,device,lat,lon,accuracyMeters,wifiBssid,channel);}
   var day=existing==null?new AttendanceDay(employee,date,plan.shift()):existing;
   try {
    day.apply(type,now,zone,rules.entryToleranceMinutes(),rules.exitToleranceMinutes(),plan.startTime(),plan.endTime(),plan.crossesMidnight(),plan.breakMinutes());day=days.saveAndFlush(day);
    var punch=punches.saveAndFlush(new Punch(employee,day,now,type,channel,ip,validation.deviceHash(),lat,lon,accuracyMeters,wifiBssid,validation.siteId(),null,null,null));
    security.accepted(employee.getId(),validation,type,ip,lat,lon,accuracyMeters,wifiBssid,channel);return Result.of(day,punch);
   } catch(org.springframework.dao.DataIntegrityViolationException ex) {
    throw new ResponseStatusException(HttpStatus.CONFLICT,type==PunchType.ENTRY?"ALREADY_CHECKED_IN":"ALREADY_CHECKED_OUT",ex);
   }
 }
  private LocalDate resolveWorkDate(Long employeeId,PunchType type,LocalDate calendarDate){
   if(type==PunchType.EXIT){
    var today=days.findByEmployeeIdAndWorkDate(employeeId,calendarDate).orElse(null);
    if(today!=null&&today.getFirstEntryAt()!=null&&today.getLastExitAt()==null)return calendarDate;
    var previous=days.findByEmployeeIdAndWorkDate(employeeId,calendarDate.minusDays(1)).orElse(null);
    if(previous!=null&&previous.getFirstEntryAt()!=null&&previous.getLastExitAt()==null&&previous.getShift()!=null&&previous.getShift().isCrossesMidnight())return calendarDate.minusDays(1);
   }
   return calendarDate;
  }
 private Result reject(Long employeeId,Long siteId,PunchType type,String reason,String ip,String device,Double lat,Double lon,Double accuracyMeters,String wifiBssid,String channel){security.denied(employeeId,siteId,type,reason,ip,device,lat,lon,accuracyMeters,wifiBssid,channel);throw new ResponseStatusException(HttpStatus.FORBIDDEN,reason);}
  @Transactional(readOnly=true) public Context context(String username){
   var employee=employees.findByUserUsernameIgnoreCase(username).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"EMPLOYEE_NOT_LINKED"));
   if(!employee.isActive())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"EMPLOYEE_INACTIVE");
   var now=Instant.now();var calendarDate=now.atZone(zone).toLocalDate();var date=resolveWorkDate(employee.getId(),PunchType.EXIT,calendarDate);
   var sites=jdbc.sql("select s.id,s.name,s.require_geofence,s.require_registered_device,s.require_dynamic_qr,s.require_wifi from employee_assignments ea join sites s on s.id=ea.site_id where ea.employee_id=:employee and ea.active=true and s.active=true and ea.valid_from<=:date and (ea.valid_to is null or ea.valid_to>=:date) and ea.assignment_type=(case when exists(select 1 from employee_assignments t where t.employee_id=ea.employee_id and t.assignment_type='TEMPORARY' and t.active=true and t.valid_from<=:date and (t.valid_to is null or t.valid_to>=:date)) then 'TEMPORARY' else 'PERMANENT' end)")
    .param("employee",employee.getId()).param("date",date).query((rs,n)->new ContextSite(rs.getLong("id"),rs.getString("name"),rs.getBoolean("require_geofence"),rs.getBoolean("require_registered_device"),rs.getBoolean("require_dynamic_qr"),rs.getBoolean("require_wifi"))).list();
   if(sites.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"NO_ACTIVE_ASSIGNMENT");if(sites.size()>1)throw new ResponseStatusException(HttpStatus.CONFLICT,"MULTIPLE_ACTIVE_ASSIGNMENTS");
   var site=sites.getFirst();var day=days.findByEmployeeIdAndWorkDate(employee.getId(),date).orElse(null);AttendanceCalendarService.Plan plan;
   try{plan=calendar.resolve(employee.getId(),site.id(),date);}catch(AttendanceCalendarService.RuleViolation ex){return context(now,date,site,null,day,java.util.List.of(),ex.code());}
   var actions=new java.util.ArrayList<PunchType>();String blocking=null;
   PunchType candidate=day==null||day.getFirstEntryAt()==null?PunchType.ENTRY:day.getLastExitAt()==null?PunchType.EXIT:null;
   if(candidate==null)blocking="ATTENDANCE_COMPLETED";
   else try{validator.validate(candidate,day,date,plan.startTime(),plan.endTime(),plan.crossesMidnight(),now,zone,rules);actions.add(candidate);}catch(AttendanceRuleValidator.Violation ex){blocking=ex.code();}
   return context(now,date,site,plan,day,java.util.List.copyOf(actions),blocking);
  }
 private Context context(Instant now,LocalDate date,ContextSite site,AttendanceCalendarService.Plan plan,AttendanceDay day,java.util.List<PunchType> actions,String blocking){
  var controls=new java.util.ArrayList<ContextControl>();
  controls.add(new ContextControl("GPS",site.requireGeofence()?"PENDING":"NOT_REQUIRED"));
  String qrStatus="NOT_REQUIRED";if(site.requireDynamicQr()){int active=jdbc.sql("select count(*) from dynamic_qr_challenges where site_id=:site and active=true and valid_from<=:now and expires_at>=:now").param("site",site.id()).param("now",now).query(Integer.class).single();qrStatus=active>0?"PENDING":"UNAVAILABLE";if(active==0&&blocking==null)blocking="NO_ACTIVE_QR";}
  controls.add(new ContextControl("QR",qrStatus));controls.add(new ContextControl("DEVICE",site.requireRegisteredDevice()?"REQUIRED":"NOT_REQUIRED"));controls.add(new ContextControl("WIFI",site.requireWifi()?"REQUIRED":"NOT_REQUIRED"));
  boolean checkIn=actions.contains(PunchType.ENTRY)&&!"NO_ACTIVE_QR".equals(blocking);boolean checkOut=actions.contains(PunchType.EXIT)&&!"NO_ACTIVE_QR".equals(blocking);
  var allowed=new java.util.ArrayList<>(actions);if(!checkIn)allowed.remove(PunchType.ENTRY);if(!checkOut)allowed.remove(PunchType.EXIT);
  return new Context(now,date,site,plan==null?null:new ContextShift(plan.startTime(),plan.endTime(),plan.crossesMidnight()),day==null?null:Result.of(day,null),java.util.List.copyOf(allowed),java.util.List.copyOf(controls),checkIn,checkOut,blocking);
 }
 public int processAbsences(LocalDate date){
  if(date.isAfter(LocalDate.now(zone)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No se procesan fechas futuras");
  int created=0;var now=Instant.now();var processed=new java.util.HashSet<Long>();
  for(var assignment:assignments.findAllApplicable(date)){
   Long employeeId=assignment.getEmployee().getId();if(!processed.add(employeeId))continue;
   var siteIds=jdbc.sql("select site_id from employee_assignments ea where employee_id=:employee and active=true and valid_from<=:date and (valid_to is null or valid_to>=:date) and assignment_type=(case when exists(select 1 from employee_assignments t where t.employee_id=ea.employee_id and t.assignment_type='TEMPORARY' and t.active=true and t.valid_from<=:date and (t.valid_to is null or t.valid_to>=:date)) then 'TEMPORARY' else 'PERMANENT' end)").param("employee",employeeId).param("date",date).query(Long.class).list();
   if(siteIds.size()>1)continue;
   AttendanceCalendarService.Plan plan;
   if(siteIds.isEmpty()){
    var legacyShift=assignment.getSchedule().getShifts().stream().filter(s->s.getDayOfWeek()==date.getDayOfWeek().getValue()).findFirst().orElse(null);if(legacyShift==null)continue;
    plan=new AttendanceCalendarService.Plan(legacyShift,legacyShift.getStartTime(),legacyShift.getEndTime(),legacyShift.isCrossesMidnight(),legacyShift.getBreakMinutes(),"LEGACY_WITHOUT_SITE");
   }else try{plan=calendar.resolve(employeeId,siteIds.getFirst(),date);}catch(AttendanceCalendarService.RuleViolation ex){continue;}
   var endDate=plan.crossesMidnight()?date.plusDays(1):date;var shiftStart=date.atTime(plan.startTime()).atZone(zone).toInstant();var shiftEnd=endDate.atTime(plan.endTime()).atZone(zone).toInstant();var closesAt=shiftEnd.plusSeconds(rules.exitCloseAfterMinutes()*60L);if(now.isBefore(closesAt))continue;
   var existing=days.findByEmployeeIdAndWorkDate(employeeId,date).orElse(null);
   if(existing==null){var day=new AttendanceDay(assignment.getEmployee(),date,plan.shift());day.markAbsent();boolean excused=hasFullShiftPermission(employeeId,date,shiftStart,shiftEnd);if(excused)day.markExcused();day=days.saveAndFlush(day);if(!excused)createAutomaticIncident(day,"ABSENCE","Ausencia detectada al cerrar la jornada");created++;}
   else if(existing.getStatus()==AttendanceStatus.OPEN){existing.markIncomplete();existing=days.saveAndFlush(existing);createAutomaticIncident(existing,"MISSING_EXIT","Jornada cerrada sin marcación de salida");}
  }
  return created;
 }
 private boolean hasFullShiftPermission(Long employeeId,LocalDate date,Instant shiftStart,Instant shiftEnd){return jdbc.sql("select count(*) from approved_permissions where employee_id=:employee and affected_date=:date and requested_from<=:start and requested_to>=:end").param("employee",employeeId).param("date",date).param("start",shiftStart).param("end",shiftEnd).query(Integer.class).single()>0;}
 private void createAutomaticIncident(AttendanceDay day,String type,String description){if(!incidents.existsByAttendanceDayIdAndTypeAndSource(day.getId(),type,"AUTOMATIC"))incidents.save(new Incident(day.getEmployee(),day,type,description,"AUTOMATIC",day.getWorkDate()));}
 @Transactional(readOnly=true) public java.util.List<Result> history(String username,LocalDate from,LocalDate to){var e=employees.findByUserUsernameIgnoreCase(username).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Usuario sin empleado"));return days.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(e.getId(),from,to).stream().map(d->Result.of(d,null)).toList();}
 public record Result(Long attendanceId,Long punchId,LocalDate workDate,Instant firstEntryAt,Instant lastExitAt,int workedMinutes,int lateMinutes,int earlyExitMinutes,AttendanceStatus status,PunchType registeredType,Instant registeredAt){public static Result of(AttendanceDay d,Punch p){return new Result(d.getId(),p==null?null:p.getId(),d.getWorkDate(),d.getFirstEntryAt(),d.getLastExitAt(),d.getWorkedMinutes(),d.getLateMinutes(),d.getEarlyExitMinutes(),d.getStatus(),p==null?null:p.getType(),p==null?null:p.getOccurredAt());}}
  public record Context(Instant serverTime,LocalDate workDate,ContextSite site,ContextShift shift,Result attendance,java.util.List<PunchType> allowedActions,java.util.List<ContextControl> requiredControls,boolean checkInAvailable,boolean checkOutAvailable,String blockingReason){}
  public record ContextControl(String control,String status){}
  public record ContextSite(Long id,String name,boolean requireGeofence,boolean requireRegisteredDevice,boolean requireDynamicQr,boolean requireWifi){}
  public record ContextShift(LocalTime startTime,LocalTime endTime,boolean crossesMidnight){}
}
