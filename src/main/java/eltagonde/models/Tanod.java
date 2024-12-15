/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eltagonde.models;

import eltagonde.utils.TimeHelpers;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Angel Marie Eltagonde
 */
@Entity
@Table(name="TANOD")
public class Tanod {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
        
    private String full_name;
    private String address;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date birth_date;

    @Column(nullable=false, columnDefinition="boolean default false")
    private Boolean archived;

    @Temporal(TemporalType.TIMESTAMP)
    private Date created_at;
    
    @OneToMany(mappedBy = "tanod", cascade = CascadeType.REMOVE)
    private List<Shift> shifts;
    
    public Tanod(){
        this.created_at = new Date();
        this.archived = false;
    }
    
    public Tanod(String full_name, String address, Date birth_date){
        this.full_name = full_name;
        this.address = address;
        this.birth_date = birth_date;
        this.created_at = new Date();
        this.archived = false;
    }

    public Long getId(){
        return this.id;
    }
        
    private void setId(Long id){
        this.id = id;
    }
        
    public String getCreatedAt(){
        return TimeHelpers.DateToSimpleFormat(this.created_at);
    }

    public void setCreatedAt(Date created_at){
        this.created_at = created_at;
    }
        
    public String getFullname(){
        return this.full_name;
    }
        
    public void setFullname(String full_name){
        this.full_name = full_name;
    }
        
    public String getAddress(){
        return this.address;
    }
        
    public void setAddress(String address){
        this.address = address;
    }
    
    public List<Shift> getShifts(){
        return this.shifts;
    }
    
    public Date getBirthDate(){
        return this.birth_date;
    }
    
    public String getSimpleBirthDate(){
        return TimeHelpers.DateToSimpleFormat(this.birth_date);
    }

    public void setBirthDate(Date birth_date){
        this.birth_date = birth_date;
    }
    
    public Boolean is_archived(){
        return this.archived;
    }
    
    public void archive(){
        this.archived = true;
    }
    
    public void unarchive(){
        this.archived = false;
    }

    public int getAge() {
        if (birth_date == null) {
            throw new IllegalStateException("Birth date is not set");
        }

        LocalDate birthDateLocal = birth_date.toInstant()
                                              .atZone(ZoneId.systemDefault())
                                              .toLocalDate();
        LocalDate currentDate = LocalDate.now();
        return Period.between(birthDateLocal, currentDate).getYears();
    }
}