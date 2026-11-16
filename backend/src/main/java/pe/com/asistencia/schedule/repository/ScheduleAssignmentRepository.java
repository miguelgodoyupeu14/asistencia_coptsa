package pe.com.asistencia.schedule.repository;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import pe.com.asistencia.schedule.domain.ScheduleAssignment; import java.time.LocalDate; import java.util.*;
public interface ScheduleAssignmentRepository extends JpaRepository<ScheduleAssignment,Long>{
 @Query("select a from ScheduleAssignment a join fetch a.schedule s left join fetch s.shifts where a.employee.id=:employeeId and a.active=true and s.active=true and a.validFrom<=:date and (a.validTo is null or a.validTo>=:date)") List<ScheduleAssignment> findApplicable(@Param("employeeId") Long employeeId,@Param("date") LocalDate date);
 @Query("select count(a)>0 from ScheduleAssignment a where a.employee.id=:employeeId and a.active=true and a.validFrom<=coalesce(:toDate,a.validFrom) and (a.validTo is null or a.validTo>=:fromDate)") boolean overlaps(@Param("employeeId") Long employeeId,@Param("fromDate") LocalDate from,@Param("toDate") LocalDate to);
 @Query("select distinct a from ScheduleAssignment a join fetch a.employee join fetch a.schedule s left join fetch s.shifts where a.active=true and a.employee.active=true and s.active=true and a.validFrom<=:date and (a.validTo is null or a.validTo>=:date)") List<ScheduleAssignment> findAllApplicable(@Param("date") LocalDate date);
 @Query("select a from ScheduleAssignment a join fetch a.schedule where a.employee.id=:employeeId order by a.validFrom desc") List<ScheduleAssignment> findAllForEmployee(@Param("employeeId") Long employeeId);
}
