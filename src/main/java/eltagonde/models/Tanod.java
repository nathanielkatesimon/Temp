/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eltagonde.models;

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
        
    private String first_name;
    private String last_name;
    private String middle_name;

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
    
    public Tanod(String first_name, String last_name, String middle_name){
        this.first_name = first_name;
        this.last_name = last_name;
        this.middle_name = middle_name;
        this.created_at = new Date();
        this.archived = false;
    }

    public Long getId(){
        return this.id;
    }
        
    private void setId(Long id){
        this.id = id;
    }
        
    public Date getCreatedAt(){
        return this.created_at;
    }
        
    public void setCreatedAt(Date created_at){
        this.created_at = created_at;
    }
        
    public String getFirstname(){
        return this.first_name;
    }
        
    public void setFirstname(String first_name){
        this.first_name = first_name;
    }

    public String getLastname(){
        return last_name;
    }
        
    public void setLastname(String last_name){
        this.last_name = last_name;
    }
        
    public String getMiddlename(){
        return middle_name;
    }
        
    public void setMiddlename(String middle_name){
        this.middle_name = middle_name;
    }
    
    public List<Shift> getShifts(){
        return this.shifts;
    }
    
    public String getFullname(){
        return this.first_name + " " + this.last_name;
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
}