package pe.com.asistencia.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.asistencia.security.domain.Role;
import pe.com.asistencia.security.domain.RoleCode;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCodeAndActiveTrue(RoleCode code);
}
