/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eltagonde.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 *
 * @author Angel Marie Eltagonde
 */
@Entity
@Table(name="ATTENDANCE")
public class Attendance {    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn
    private Tanod tanod;
    
    @ManyToOne
    @JoinColumn
    private Shift shift;
    
    private Date date;
    
    @Column(nullable=false)
    private LocalTime supposed_clock_in;
    
    @Column(nullable=false)
    private LocalTime supposed_clock_out;
    
    @Column(nullable=false)
    private int supposed_shift_duration;
    
    private LocalTime actual_clock_in;
    private LocalTime actual_clock_out;
    private int actual_shift_duration;
    
    public Attendance() {}
    
    public Attendance(
            Tanod tanod, 
            LocalTime supposed_clock_in,
            LocalTime supposed_clock_out,
            Date date,
            Shift shift
    ){
        this.tanod = tanod;
        this.date = date;
        this.supposed_clock_in = supposed_clock_in;
        this.supposed_clock_out = supposed_clock_out;
        this.shift = shift;
        this.calculate_supposed_shift_duration();
    }
    
    private void setId(Long id){
        this.id = id;
    }
    
    public Long getId(){
        return this.id;
    }
    
    public Tanod getTanod(){
        return this.tanod;
    }
    
    public LocalTime getSupposedClockIn(){
        return this.supposed_clock_in;
    }
    
    public void setSupposedClockIn(LocalTime supposed_clock_in){
        this.supposed_clock_in = supposed_clock_in;
        this.calculate_supposed_shift_duration();
    }
    
    public LocalTime getSupposedClockOut(){
        return this.supposed_clock_out;
    }
    
    public void setSupposedClockOut(LocalTime supposed_clock_out){
        this.supposed_clock_out = supposed_clock_out;
        this.calculate_supposed_shift_duration();
    }
    
    public LocalTime getActualClockIn(){
        return this.actual_clock_in;
    }
    
    public void setActualClockIn(LocalTime actual_clock_in){
        this.actual_clock_in = actual_clock_in;
        this.calculate_actual_shift_duration();
    }
    
    public LocalTime getActualClockOut(){
        return this.actual_clock_out;
    }
    
    public void setActualClockOut(LocalTime actual_clock_out){
        this.actual_clock_out = actual_clock_out;
        this.calculate_actual_shift_duration();
    }
    
    private void calculate_supposed_shift_duration(){
        LocalDateTime clock_in_datetime = this.supposed_clock_in.atDate(LocalDateTime.now().toLocalDate());
        LocalDateTime clock_out_datetime = this.supposed_clock_out.atDate(LocalDateTime.now().toLocalDate());
        
        if(clock_out_datetime.isBefore(clock_in_datetime)){
            clock_out_datetime = clock_out_datetime.plusDays(1);
        }
        
        this.supposed_shift_duration = (int) Duration.between(clock_in_datetime, clock_out_datetime).toMinutes();
    }
    
    public int getSupposedShiftDuration(){
        return this.supposed_shift_duration;
    }
    
    private void calculate_actual_shift_duration(){
        if(this.actual_clock_in != null && this.actual_clock_out != null){
            LocalDateTime clock_in_datetime = this.actual_clock_in.atDate(LocalDateTime.now().toLocalDate());
            LocalDateTime clock_out_datetime = this.actual_clock_out.atDate(LocalDateTime.now().toLocalDate());
        
            if(clock_out_datetime.isBefore(clock_in_datetime)){
                clock_out_datetime = clock_out_datetime.plusDays(1);
            }
        
            this.actual_shift_duration = (int) Duration.between(clock_in_datetime, clock_out_datetime).toMinutes();
        } else {
            this.actual_shift_duration = 0;
        }
    }
    public int getActualShiftDuration(){
        return this.actual_shift_duration;
    }
    
    public Date getDate(){
        return this.date;
    }
    
    public void persistToShift(){
        this.supposed_clock_in = this.shift.getShiftStart();
        this.supposed_clock_out = this.shift.getShiftEnd();
        this.calculate_supposed_shift_duration();
    }
    
    public void removeShift(){
        this.shift = null;
        if(this.actual_clock_in == null || this.actual_clock_out == null){
            this.actual_shift_duration = 0;
        }
    }
    
    public Boolean isLocked(){
        return this.shift == null;
    }
}
