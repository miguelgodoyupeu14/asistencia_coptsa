package pe.com.asistencia.schedule.domain;
import jakarta.persistence.*; import java.util.*;
@Entity @Table(name="schedules")
public class Schedule {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true,length=40) private String code; @Column(nullable=false,length=150) private String name; private String description; @Column(nullable=false) private boolean active=true;
 @OneToMany(mappedBy="schedule",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.EAGER) @OrderBy("dayOfWeek") private List<Shift> shifts=new ArrayList<>();
 protected Schedule(){} public Schedule(String code,String name,String description){this.code=code.toUpperCase();this.name=name;this.description=description;}
 public void addShift(Shift shift){shift.attach(this);shifts.add(shift);} public Long getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public String getDescription(){return description;} public boolean isActive(){return active;} public List<Shift> getShifts(){return List.copyOf(shifts);}
 public void update(String name,String description,boolean active){this.name=name;this.description=description;this.active=active;}
}
