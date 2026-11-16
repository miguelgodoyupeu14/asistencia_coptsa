package pe.com.asistencia.audit.domain;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="audit_logs")
public class AuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="occurred_at",nullable=false,updatable=false) private Instant occurredAt; @Column(updatable=false) private String username; @Column(nullable=false,updatable=false) private String action; @Column(nullable=false,updatable=false,length=500) private String resource; @Column(name="result_status",nullable=false,updatable=false) private int resultStatus; @Column(name="ip_address",updatable=false) private String ipAddress; @Column(name="duration_ms",nullable=false,updatable=false) private long durationMs; @Column(name="correlation_id",nullable=false,updatable=false) private String correlationId;
 protected AuditLog(){} public AuditLog(Instant at,String username,String action,String resource,int status,String ip,long duration,String correlation){occurredAt=at;this.username=username;this.action=action;this.resource=resource;resultStatus=status;ipAddress=ip;durationMs=duration;correlationId=correlation;}
 public Long getId(){return id;} public Instant getOccurredAt(){return occurredAt;} public String getUsername(){return username;} public String getAction(){return action;} public String getResource(){return resource;} public int getResultStatus(){return resultStatus;} public String getIpAddress(){return ipAddress;} public long getDurationMs(){return durationMs;} public String getCorrelationId(){return correlationId;}
}
