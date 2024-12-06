/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eltagonde.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;

/**
 *
 * @author Angel Marie Eltagonde
 */
@Entity
@Table(name="SHIFT")
public class Shift {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    private String day_of_week;
    private LocalTime shift_start;
    private LocalTime shift_end;
    private Boolean deleted;
    
    @ManyToOne
    @JoinColumn
    private Tanod tanod;
    
    @OneToMany(mappedBy = "shift", cascade=CascadeType.REMOVE)
    private List<Attendance> attendances;
    
    public Shift(){
        this.deleted = false;
    }
    
    public Shift(Tanod tanod, String day_of_week, LocalTime shift_start, LocalTime shift_end){
        this.tanod = tanod;
        this.day_of_week = day_of_week;
        this.shift_start = shift_start;
        this.shift_end = shift_end;
        this.deleted = false;
    }
    
    private void setId(Long id){
        this.id = id;
    }
    
    public Long getId(){
        return this.id;
    }
    
    public String getDayOfWeek(){
       return this.day_of_week;
    }
    
    public void setDayOfWeek(String day_of_week){
        this.day_of_week = day_of_week;
    }
    
    public LocalTime getShiftStart(){
        return this.shift_start;
    }
    
    public void setShiftStart(LocalTime shift_start){
        this.shift_start = shift_start;
    }
    
    public LocalTime getShiftEnd(){
        return this.shift_end;
    }
    
    public void setShiftEnd(LocalTime shift_end){
        this.shift_end = shift_end;
    }
    
    public Tanod getTanod(){
        return this.tanod;
    }
    
    public void setTanod(Tanod tanod){
        this.tanod = tanod;
    }
    
    public Attendance getAttendance(LocalDate date, Session session){
        Attendance attendance;
        
        try{
            attendance = session.createQuery("FROM Attendance a WHERE a.date = :date AND a.shift.id = :id", Attendance.class)
            .setParameter("date", Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC)))
            .setParameter("id", this.id)
            .getSingleResult();
        } catch(NoResultException e){
            attendance = new Attendance(
                    this.tanod,
                    this.shift_start,
                    this.shift_end,
                    Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC)),
                    this
            );
            
            session.persist(attendance);
        }

        return attendance;
    }
}
