package pe.com.asistencia.request.repository;
import org.springframework.data.jpa.repository.JpaRepository; import pe.com.asistencia.request.domain.*; import java.util.*;
public interface EmployeeRequestRepository extends JpaRepository<EmployeeRequest,Long>{List<EmployeeRequest> findByEmployeeIdOrderBySubmittedAtDesc(Long employeeId);List<EmployeeRequest> findByStatusOrderBySubmittedAtAsc(RequestStatus status);}
