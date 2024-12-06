/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eltagonde.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;

/**
 *
 * @author Angel Marie Eltagonde
 */
@Entity
@Table(name="PAYROLL")
public class Payroll {
    public static final double SALARY = 1200.00;
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn
    private Tanod tanod;
    
    private double missedMinutes;
    private double rateBaseOnExpectedWorkedHours;
    private double deduction;
    private double netSalary;
    
    private LocalDate issuedOn;
    private LocalDate startDate;
    private LocalDate endDate;
    
    public Payroll(){
        this.issuedOn = LocalDate.now();
    }
    
    public Payroll(LocalDate startDate, LocalDate endDate, Tanod tanod){
        this.startDate = startDate;
        this.endDate = endDate;
        this.tanod = tanod;
        this.issuedOn = LocalDate.now();
    }
    
    public void preview(Session session){
        this.ProccessUp(session);
    }
    
    private void ProccessUp(Session session){
        
        List<Attendance> attendances = session.createQuery("FROM Attendance a WHERE a.date >= :start AND a.date <= :end AND a.tanod.id = :id", Attendance.class)
                .setParameter("start", Date.from(this.startDate.atStartOfDay().toInstant(ZoneOffset.UTC)))
                .setParameter("end", Date.from(this.endDate.atStartOfDay().toInstant(ZoneOffset.UTC)))
                .setParameter("id", this.tanod.getId())
                .getResultList();
        
        double expected_working_time = 0;
        double actual_working_time = 0;
        
        for(Attendance attendance : attendances){
           expected_working_time += (double) attendance.getSupposedShiftDuration();
           actual_working_time += (double) attendance.getActualShiftDuration();
        }
        
        
        if(expected_working_time == 0){
            this.rateBaseOnExpectedWorkedHours = 0;
        } else {
            this.rateBaseOnExpectedWorkedHours = SALARY / (double) expected_working_time;
        }

        this.missedMinutes = Math.max(0, expected_working_time - actual_working_time);
        
        this.deduction = this.rateBaseOnExpectedWorkedHours * this.missedMinutes;
        
        this.netSalary = this.rateBaseOnExpectedWorkedHours == 0 ? 0 : SALARY - deduction;
    }
    
    public String getTanodFullName(){
        return this.tanod.getFullname();
    }
    
    public String getPeriod(){
        return this.startDate.toString() + " to " + this.endDate.toString();
    }
    
    public Double getDeduction(){
        return this.deduction;
    }
    
    public Double getNetSalary(){
        return this.netSalary;
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
    
    public LocalDate getIssuedOn(){
        return this.issuedOn;
    }
}
