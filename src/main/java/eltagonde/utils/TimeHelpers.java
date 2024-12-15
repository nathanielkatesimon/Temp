/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package eltagonde.utils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import javax.swing.JComboBox;

/**
 *
 * @author Angel Marie Eltagonde
 */
public class TimeHelpers {
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
    
    public static LocalTime toLocalTime(JComboBox hour, JComboBox minute, JComboBox amPm){
        
        int hour_int = Integer.parseInt(hour.getSelectedItem().toString());
        int minute_int = Integer.parseInt(minute.getSelectedItem().toString());
        String amPm_str = (String) amPm.getSelectedItem();
        
        if(amPm_str.equals("PM") && hour_int != 12){
            hour_int += 12;
        } else if (amPm_str.equals("AM") && hour_int == 12){
            hour_int = 0;
        }
        
        return LocalTime.of(hour_int, minute_int);
    }
    
    public static String toAMPM(LocalTime time){
        if(time == null){
            return "01:00 AM";
        } else {
            return time.format(formatter);
        }
    }
    
    public static String[] splitTime(String time){
        String[] words = new String[3];
        
        words[0] = time.split(":")[0];
        words[1] = time.split(":")[1].split(" ")[0];
        words[2] = time.split(":")[1].split(" ")[1];
        
        return words;
    }
    
    public static String minuteToHrs(int min){
        int hours = min/60;
        int minutes = min%60;
        return String.format("%d hr %d min", hours, minutes);
    }
    
    public static LocalDate DateToLocalDate(Date date){
        Instant instant = date.toInstant();
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }
    
    public static String DateToSimpleFormat(Date date){
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy");
        return formatter.format(date);
    }
}
