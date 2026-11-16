package pe.com.asistencia.organization.domain;
import jakarta.persistence.*;
@Entity @Table(name="positions")
public class Position {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true,length=40) private String code; @Column(nullable=false,length=150) private String name; private String description; @Column(nullable=false) private boolean active=true;
 protected Position(){} public Position(String code,String name,String description){this.code=code.toUpperCase();this.name=name;this.description=description;}
 public Long getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public String getDescription(){return description;} public boolean isActive(){return active;}
 public void update(String name,String description,boolean active){this.name=name;this.description=description;this.active=active;}
}
