package pe.com.asistencia.audit.api;
import org.springframework.data.domain.*; import org.springframework.format.annotation.DateTimeFormat; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import pe.com.asistencia.audit.domain.AuditLog; import pe.com.asistencia.audit.repository.AuditLogRepository; import java.time.Instant;
@RestController @RequestMapping("/api/v1/audit") @PreAuthorize("hasRole('ADMIN')") public class AuditController {
 private final AuditLogRepository logs; public AuditController(AuditLogRepository logs){this.logs=logs;}
 @GetMapping public Page<View> list(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return logs.findByOccurredAtBetweenOrderByOccurredAtDesc(from,to,PageRequest.of(page,Math.min(size,200))).map(View::of);}
 public record View(Long id,Instant occurredAt,String username,String action,String resource,int status,String ipAddress,long durationMs,String correlationId){static View of(AuditLog a){return new View(a.getId(),a.getOccurredAt(),a.getUsername(),a.getAction(),a.getResource(),a.getResultStatus(),a.getIpAddress(),a.getDurationMs(),a.getCorrelationId());}}
}
