package pe.com.asistencia.schedule.domain;
import jakarta.persistence.*; import java.time.*;
@Entity @Table(name="shifts")
public class Shift {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="schedule_id") private Schedule schedule; @Column(name="day_of_week",nullable=false) private short dayOfWeek; @Column(name="start_time",nullable=false) private LocalTime startTime; @Column(name="end_time",nullable=false) private LocalTime endTime; @Column(name="crosses_midnight",nullable=false) private boolean crossesMidnight; @Column(name="break_minutes",nullable=false) private int breakMinutes;
 protected Shift(){} public Shift(int dayOfWeek,LocalTime startTime,LocalTime endTime,int breakMinutes){this.dayOfWeek=(short)dayOfWeek;this.startTime=startTime;this.endTime=endTime;this.crossesMidnight=!endTime.isAfter(startTime);this.breakMinutes=breakMinutes;} void attach(Schedule s){schedule=s;}
 public Long getId(){return id;} public int getDayOfWeek(){return dayOfWeek;} public LocalTime getStartTime(){return startTime;} public LocalTime getEndTime(){return endTime;} public boolean isCrossesMidnight(){return crossesMidnight;} public int getBreakMinutes(){return breakMinutes;}
}
