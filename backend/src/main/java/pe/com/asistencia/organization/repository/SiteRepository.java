package pe.com.asistencia.organization.repository;
import org.springframework.data.jpa.repository.JpaRepository; import pe.com.asistencia.organization.domain.Site;
public interface SiteRepository extends JpaRepository<Site,Long>{ boolean existsByCodeIgnoreCase(String code); }
