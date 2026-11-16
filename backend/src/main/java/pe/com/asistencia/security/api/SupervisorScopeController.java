package pe.com.asistencia.security.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/supervisor-scopes")
@PreAuthorize("hasRole('ADMIN')")
public class SupervisorScopeController {
    private final JdbcClient jdbc;
    public SupervisorScopeController(JdbcClient jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> list() {
        return jdbc.sql("""
            select ss.id,ss.user_id,u.username,ss.site_id,s.name site_name,
                   ss.area_id,a.name area_name,ss.active
              from supervisor_scopes ss join users u on u.id=ss.user_id
              join sites s on s.id=ss.site_id left join areas a on a.id=ss.area_id
             where ss.active=true order by u.username,s.name,a.name
            """).query().listOfRows();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@Valid @RequestBody Request request) {
        jdbc.sql("insert into supervisor_scopes(user_id,site_id,area_id,active) values(:user,:site,:area,true)")
            .param("user", request.userId()).param("site", request.siteId()).param("area", request.areaId()).update();
        return Map.of("status", "CREATED");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        jdbc.sql("update supervisor_scopes set active=false where id=:id").param("id", id).update();
    }

    public record Request(@NotNull Long userId, @NotNull Long siteId, Long areaId) {}
}
