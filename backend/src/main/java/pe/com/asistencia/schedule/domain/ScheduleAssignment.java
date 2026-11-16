package pe.com.asistencia.schedule.domain;
import jakarta.persistence.*; import pe.com.asistencia.employee.domain.Employee; import java.time.LocalDate;
@Entity @Table(name="schedule_assignments")
public class ScheduleAssignment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id") private Employee employee; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="schedule_id") private Schedule schedule; @Column(name="valid_from",nullable=false) private LocalDate validFrom; @Column(name="valid_to") private LocalDate validTo; @Column(nullable=false) private boolean active=true;
 protected ScheduleAssignment(){} public ScheduleAssignment(Employee e,Schedule s,LocalDate from,LocalDate to){employee=e;schedule=s;validFrom=from;validTo=to;}
 public void close(LocalDate date){if(date.isBefore(validFrom))throw new IllegalArgumentException("La fecha de cierre no puede ser anterior al inicio");validTo=date;}
 public Long getId(){return id;} public Employee getEmployee(){return employee;} public Schedule getSchedule(){return schedule;} public LocalDate getValidFrom(){return validFrom;} public LocalDate getValidTo(){return validTo;} public boolean isActive(){return active;}
}
