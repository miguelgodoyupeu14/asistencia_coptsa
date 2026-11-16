package pe.com.asistencia.attendance.api;
import jakarta.servlet.http.HttpServletRequest; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.format.annotation.DateTimeFormat; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import pe.com.asistencia.attendance.domain.PunchType; import pe.com.asistencia.attendance.service.AttendanceService; import java.time.LocalDate; import java.util.List; import java.util.Map;
@RestController @RequestMapping("/api/v1/attendance")
public class AttendanceController {
 private final AttendanceService service; public AttendanceController(AttendanceService s){service=s;}
 @GetMapping("/context") public AttendanceService.Context context(Authentication auth){return service.context(auth.getName());}
 @PostMapping("/punch") public AttendanceService.Result punch(Authentication auth,HttpServletRequest http,@Valid @RequestBody PunchRequest r){return service.punch(auth.getName(),r.type(),r.channel(),clientIp(http),r.deviceIdentifier(),r.latitude(),r.longitude(),r.accuracyMeters(),r.qrCode(),r.wifiBssid());}
 @GetMapping("/me") public List<AttendanceService.Result> history(Authentication auth,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){if(to.isBefore(from)||from.isBefore(to.minusYears(1)))throw new IllegalArgumentException("Rango inválido");return service.history(auth.getName(),from,to);}
 @PostMapping("/process-absences") @PreAuthorize("hasRole('ADMIN')") public Map<String,Integer> process(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return Map.of("created",service.processAbsences(date));}
 private String clientIp(HttpServletRequest r){String forwarded=r.getHeader("X-Forwarded-For");return forwarded==null?r.getRemoteAddr():forwarded.split(",")[0].trim();}
 public record PunchRequest(@NotNull PunchType type,@NotBlank @Pattern(regexp="WEB|MOBILE|ANDROID|AUTOMATED_TEST") String channel,String deviceIdentifier,
  @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,@DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
  @Positive @DecimalMax("10000.0") Double accuracyMeters,String qrCode,@Size(max=32) String wifiBssid){}
}
