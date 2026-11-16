package pe.com.asistencia.security.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class SupervisorScopeService {
    private final JdbcClient jdbc;
    public SupervisorScopeService(JdbcClient jdbc) { this.jdbc = jdbc; }

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public Set<Long> employeeIds(Authentication authentication, LocalDate date) {
        if (isAdmin(authentication)) return new HashSet<>(jdbc.sql("select id from employees").query(Long.class).list());
        return new HashSet<>(jdbc.sql("""
            select distinct ea.employee_id
              from supervisor_scopes ss
              join users u on u.id=ss.user_id
              join employee_assignments ea on ea.site_id=ss.site_id
               and (ss.area_id is null or ss.area_id=ea.area_id)
             where lower(u.username)=lower(:username) and ss.active=true and ea.active=true
               and ea.valid_from<=:date and (ea.valid_to is null or ea.valid_to>=:date)
            """).param("username", authentication.getName()).param("date", date).query(Long.class).list());
    }

    public boolean canAccess(Authentication authentication, Long employeeId, LocalDate date) {
        return isAdmin(authentication) || employeeIds(authentication, date).contains(employeeId);
    }
}
