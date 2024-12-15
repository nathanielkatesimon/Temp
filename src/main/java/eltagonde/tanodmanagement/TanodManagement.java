package eltagonde.tanodmanagement;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import eltagonde.utils.AdminLogin;
import eltagonde.models.Attendance;
import eltagonde.models.Payroll;
import static eltagonde.models.Payroll.SALARY;
import eltagonde.models.Shift;
import eltagonde.models.Tanod;
import eltagonde.utils.Hibernate;
import eltagonde.utils.TimeHelpers;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author Angel Marie Eltagonde
 */
public class TanodManagement extends javax.swing.JFrame {
   private final SessionFactory sessionFactory = Hibernate.getSessionFactory();
   private static HashMap<String, Object> appState = new HashMap<>();
   
   private void FillPayrollTable(){
       try(Session session = sessionFactory.openSession()){
           session.beginTransaction();
           
            DefaultTableModel tableModel = new DefaultTableModel(
                new Object [][] {},
                new String [] {
                    "ID", "Tanod", "IssuedOn"
                }
            );
            
            List<Payroll> payrolls = session.createQuery("FROM Payroll", Payroll.class).list();
           
            for(Payroll payroll : payrolls){
                tableModel.addRow(new Object[]{
                    payroll.getId(),
                    payroll.getTanod().getFullname(),
                    payroll.getIssuedOn()
                });
            }
            
            PayrollTable.setModel(tableModel);
            
            session.getTransaction().commit();
            session.close();
       }
   }
   
   private void fillReadOnlyAttendances(){
       try(Session session = sessionFactory.openSession()){
           session.beginTransaction();
           
           DefaultTableModel tableModel = new DefaultTableModel(
                new Object [][] {},
                new String [] {
                    "Attendance ID", "Tanod", "Schedule", "Clock In", "Clock Out", "Duration"
                }
           );
           
           List<Attendance> attendances = session.createQuery("FROM Attendance a WHERE a.date = :date", Attendance.class)
                   .setParameter("date", Date.from(calendarPanel1.getSelectedDate().atStartOfDay().toInstant(ZoneOffset.UTC)))
                   .getResultList();
           

           for(Attendance attendance : attendances){
                tableModel.addRow(new Object[]{
                    attendance.getId(),
                    attendance.getTanod().getFirstname() + " " + attendance.getTanod().getLastname(),
                    TimeHelpers.toAMPM(attendance.getSupposedClockIn()) + " " + TimeHelpers.toAMPM(attendance.getSupposedClockOut()),
                    TimeHelpers.toAMPM(attendance.getActualClockIn()),
                    TimeHelpers.toAMPM(attendance.getActualClockOut()),
                    TimeHelpers.minuteToHrs(attendance.getActualShiftDuration())
                });
           }

           LockButton.setEnabled(false);
           ManualAdjustSave.setEnabled(false);
           ClockIn.setEnabled(false);
           ClockOut.setEnabled(false);
           ShiftsTable2.setModel(tableModel);
           ShiftsTable2.setEnabled(false);
           session.getTransaction().commit();
           session.close();
       }
   }
   
   
   private void fillShiftsAttendancingTable(){
       try(Session session = sessionFactory.openSession()){
           session.beginTransaction();
           
           DefaultTableModel tableModel = new DefaultTableModel(
                new Object [][] {},
                new String [] {
                    "Attendance ID", "Tanod", "Schedule", "Clock In", "Clock Out", "Duration"
                }
           );
           
           List<Shift> shifts = session.createQuery("FROM Shift s WHERE s.day_of_week LIKE :day ORDER BY s.shift_start ASC", Shift.class)
                   .setParameter("day", calendarPanel1.getSelectedDate().getDayOfWeek().toString())
                   .getResultList();
           
           for(Shift shift : shifts){
               Attendance attendance = shift.getAttendance(calendarPanel1.getSelectedDate(), session);
               
               if(shift.getShiftEnd() != attendance.getSupposedClockOut() || shift.getShiftStart() != attendance.getSupposedClockIn()){
                    attendance.persistToShift();
                    session.merge(attendance);
               }

               tableModel.addRow(new Object[]{
                   attendance.getId(),
                   shift.getTanod().getFirstname() + " " + shift.getTanod().getLastname(),
                   TimeHelpers.toAMPM(shift.getShiftStart()) + " - " + TimeHelpers.toAMPM(shift.getShiftEnd()),
                   attendance.getActualClockIn() != null ? TimeHelpers.toAMPM(attendance.getActualClockIn()) : null,
                   attendance.getActualClockOut() != null ? TimeHelpers.toAMPM(attendance.getActualClockOut()) : null,
                   TimeHelpers.minuteToHrs(attendance.getActualShiftDuration())
               });
           }
           
           ShiftsTable2.setModel(tableModel);
           ShiftsTable2.setEnabled(true);
           session.getTransaction().commit();
           session.close();
       }
   }
   
   private void fillShiftsTable(){
       try(Session session = sessionFactory.openSession()){
           session.beginTransaction();

           if(DayOfWeekFilter.getSelectedItem().toString().equals("All Days")){
               Tanod tanod = session.find(Tanod.class, appState.get("editingTanodId"));
               performFillOnShiftsTable(tanod.getShifts());
           } else {
               List<Shift> shifts = session.createQuery("FROM Shift s JOIN s.tanod t WHERE s.day_of_week = :day AND t.id = :id", Shift.class)
                       .setParameter("day", DayOfWeekFilter.getSelectedItem().toString())
                       .setParameter("id", appState.get("editingTanodId"))
                       .getResultList();
               
               performFillOnShiftsTable(shifts);
           }

           session.getTransaction().commit();
           session.close();
       }
   }
   
   private void performFillOnShiftsTable(List<Shift> shifts){
       DefaultTableModel tableModel = new DefaultTableModel(
            new Object [][] {},
            new String [] { "ID", "Day", "Shift Start", "Shift End"}
       );
       
       for(Shift shift : shifts){
            tableModel.addRow(new Object[]{
            shift.getId(), shift.getDayOfWeek(), TimeHelpers.toAMPM(shift.getShiftStart()), TimeHelpers.toAMPM(shift.getShiftEnd())
        });
       }
       ShiftsTable.setModel(tableModel);
       fillShiftsAttendancingTable();
   }
    
   private List<Tanod> getAllTanods(){
       List<Tanod> result;
       try(Session session = sessionFactory.openSession()){
           session.beginTransaction();
           result = session.createQuery("from Tanod").list();
           session.getTransaction().commit();
           session.close();
       }
       return result;
   }
   
   private List<Tanod> searchTanods(String searchTerm){
       List<Tanod> tanods;
       try(Session session = sessionFactory.openSession()){
           session.beginTransaction();
           
           String hql = "SELECT t FROM Tanod t WHERE t.first_name LIKE :searchTerm OR t.last_name LIKE :searchTerm OR t.middle_name LIKE :searchTerm";
           
           Query<Tanod> query = session.createQuery(hql, Tanod.class);
           query.setParameter("searchTerm", "%" + searchTerm + "%");
           tanods = query.getResultList();

           session.getTransaction().commit();
           session.close();
       }
       return tanods;
   }
   
   private void fillTanodsTable(String searchTerm){
       performFillOnTanodsTable(searchTanods(searchTerm));
   }
 
   private void fillTanodsTable(){
       performFillOnTanodsTable(getAllTanods());
   }
   
   private void fillTanodsTable(Long id){
       List<Tanod> tanods = new ArrayList<>();
       try(Session session = sessionFactory.openSession()){
           session.beginTransaction();
           
           Tanod tanod = session.find(Tanod.class, id);
           
           if(tanod != null){
                tanods.add(tanod);
           }
           
           session.getTransaction().commit();
           session.close();
       }
       performFillOnTanodsTable(tanods);
   }
   
   private void performFillOnTanodsTable(List<Tanod> tanods){
    DefaultTableModel tableModel = new DefaultTableModel(
        new Object [][] {},
        new String [] { "ID", "First Name", "Last Name", "Middle Name", "Created At"}
    );
    
    DefaultTableModel archivesTableModel = new DefaultTableModel(
        new Object [][] {},
        new String [] { "ID", "First Name", "Last Name", "Middle Name", "Created At"}
    );

    for(Tanod tanod: tanods){
        if(tanod.is_archived()){
            archivesTableModel.addRow(new Object[]{
                tanod.getId(), tanod.getFirstname(), tanod.getLastname(), tanod.getMiddlename(), tanod.getCreatedAt()
            });
        }
        else{
            tableModel.addRow(new Object[]{
                tanod.getId(), tanod.getFirstname(), tanod.getLastname(), tanod.getMiddlename(), tanod.getCreatedAt()
            });
        }
    }

    TanodsTable.setModel(tableModel);
    ArchivesTable.setModel(archivesTableModel);
   }
    
    /**
     * Creates new form TanodManagement
     */
    public TanodManagement() {
        FlatMacLightLaf.setup();
        IconFontSwing.register(FontAwesome.getIconFont());
        initComponents();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        TanodForm = new javax.swing.JFrame();
        TanodFormPanel = new javax.swing.JPanel();
        tanod_first_name = new javax.swing.JTextField();
        tanod_last_name = new javax.swing.JTextField();
        tanod_middle_name = new javax.swing.JTextField();
        SaveTanod = new javax.swing.JButton();
        ArchiveTanod = new javax.swing.JButton();
        tanod_first_name_label = new javax.swing.JLabel();
        tanod_last_name_label = new javax.swing.JLabel();
        tanod_middle_name_label = new javax.swing.JLabel();
        ShiftForm = new javax.swing.JFrame();
        ShiftFormPanel = new javax.swing.JPanel();
        SaveShift = new javax.swing.JButton();
        DeleteShift = new javax.swing.JButton();
        Shift_for = new javax.swing.JLabel();
        Shift_starts_at = new javax.swing.JLabel();
        Shift_ends_at = new javax.swing.JLabel();
        Shift_starts_at_hr = new javax.swing.JComboBox<>();
        Shift_starts_at_mn = new javax.swing.JComboBox<>();
        Shift_starts_at_md = new javax.swing.JComboBox<>();
        Shift_ends_at_hr = new javax.swing.JComboBox<>();
        Shift_ends_at_mn = new javax.swing.JComboBox<>();
        Shift_ends_at_md = new javax.swing.JComboBox<>();
        Shift_day = new javax.swing.JComboBox<>();
        RestorationDialog = new javax.swing.JFrame();
        RestoreTanodDialog = new javax.swing.JPanel();
        RestoreTanodBtn = new javax.swing.JButton();
        DeleteTanod = new javax.swing.JButton();
        tanod_first_name_label1 = new javax.swing.JLabel();
        tanod_last_name_label1 = new javax.swing.JLabel();
        tanod_middle_name_label1 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jLabel34 = new javax.swing.JLabel();
        userText = new javax.swing.JTextField();
        passwordText = new javax.swing.JPasswordField();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        jButton10 = new javax.swing.JButton();
        gradientBackgroundPanel1 = new eltagonde.utils.GradientBackgroundPanel();
        ImageIcon logo = new ImageIcon("C:\\Users\\Nathaniel Kate Simon\\Downloads\\logo.png");
        basicBackgroundPanel2 = new eltagonde.utils.BasicBackgroundPanel(logo.getImage());
        jLabel37 = new javax.swing.JLabel();
        ImageIcon bg = new ImageIcon("C:\\Users\\Nathaniel Kate Simon\\Downloads\\Background.jpg");
        basicBackgroundPanel1 = new eltagonde.utils.BasicBackgroundPanel(bg.getImage());
        jPanel4 = new javax.swing.JPanel();
        Main = new javax.swing.JPanel();
        CardLayout = new javax.swing.JPanel();
        Dashboard = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        jPanel16 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jLabel33 = new javax.swing.JLabel();
        jPanel15 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel38 = new javax.swing.JLabel();
        ImageIcon banner = new ImageIcon("C:\\Users\\Nathaniel Kate Simon\\Downloads\\banner.jpg");
        basicBackgroundPanel3 = new eltagonde.utils.BasicBackgroundPanel(banner.getImage());
        ClockInClockOut = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        ShiftsTable2 = new javax.swing.JTable();
        ShiftsManagementContent2 = new javax.swing.JPanel();
        calendarPanel1 = new com.github.lgooddatepicker.components.CalendarPanel();
        jLabel5 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        clock_in_hr = new javax.swing.JComboBox<>();
        clock_in_mn = new javax.swing.JComboBox<>();
        clock_in_md = new javax.swing.JComboBox<>();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        clock_out_hr = new javax.swing.JComboBox<>();
        clock_out_mn = new javax.swing.JComboBox<>();
        clock_out_md = new javax.swing.JComboBox<>();
        ManualAdjustSave = new javax.swing.JButton();
        ClockIn = new javax.swing.JButton();
        ClockOut = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        LockButton = new javax.swing.JButton();
        TanodManagement = new javax.swing.JPanel();
        TanodManagementHead = new javax.swing.JPanel();
        TanodSearchField = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        TanodsTable = new javax.swing.JTable();
        jScrollPane7 = new javax.swing.JScrollPane();
        ArchivesTable = new javax.swing.JTable();
        ShiftsManagement = new javax.swing.JPanel();
        ShiftsManagementHead = new javax.swing.JPanel();
        TanodSearchField2 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        ShiftsManagementContent = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        SMTanod_id = new javax.swing.JLabel();
        SMFirst_name = new javax.swing.JLabel();
        SMLast_name = new javax.swing.JLabel();
        SMMiddle_name = new javax.swing.JLabel();
        SMCreated_at = new javax.swing.JLabel();
        UpdateTanod = new javax.swing.JButton();
        AddShift = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        ShiftsTable = new javax.swing.JTable();
        DayOfWeekFilter = new javax.swing.JComboBox<>();
        Payroll = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        SearchTanodField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        PayrollTable = new javax.swing.JTable();
        payrollStart = new com.github.lgooddatepicker.components.DatePicker();
        payrollEnd = new com.github.lgooddatepicker.components.DatePicker();
        jButton2 = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jSeparator6 = new javax.swing.JSeparator();
        jLabel26 = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        TanodIdLabel = new javax.swing.JLabel();
        TanodFullName = new javax.swing.JLabel();
        PayrollPeriodLabel = new javax.swing.JLabel();
        IssuedOnLabel = new javax.swing.JLabel();
        GrossLabel = new javax.swing.JLabel();
        DeductionLabel = new javax.swing.JLabel();
        NetLabel = new javax.swing.JLabel();
        System = new javax.swing.JPanel();
        UpdateAdminCredentials = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        username = new javax.swing.JTextField();
        password = new javax.swing.JPasswordField();
        jLabel31 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        SidePanel = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        DashboardBtn = new javax.swing.JButton();
        ClockInClockOutBtn = new javax.swing.JButton();
        TanodsBtn = new javax.swing.JButton();
        ShiftsBtn = new javax.swing.JButton();
        PayrollBtn = new javax.swing.JButton();
        SystemBtn = new javax.swing.JButton();
        SystemBtn1 = new javax.swing.JButton();
        jPanel12 = new javax.swing.JPanel();

        TanodForm.setTitle("New Tanod");
        TanodForm.setLocation(new java.awt.Point(0, 0));
        TanodForm.setMinimumSize(new java.awt.Dimension(300, 200));
        TanodForm.setResizable(false);
        TanodForm.setSize(new java.awt.Dimension(300, 290));
        TanodForm.setLocationRelativeTo(null);

        TanodFormPanel.setBackground(new java.awt.Color(255, 255, 255));
        TanodFormPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        TanodFormPanel.setName("asdasd"); // NOI18N
        TanodFormPanel.setPreferredSize(new java.awt.Dimension(300, 221));

        tanod_first_name.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tanod_first_nameActionPerformed(evt);
            }
        });

        tanod_last_name.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tanod_last_nameActionPerformed(evt);
            }
        });

        tanod_middle_name.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tanod_middle_nameActionPerformed(evt);
            }
        });

        SaveTanod.setIcon(IconFontSwing.buildIcon(
            FontAwesome.FLOPPY_O,
            15,
            java.awt.Color.WHITE
        ));
        SaveTanod.setText("Save");
        SaveTanod.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        SaveTanod.setBorder(ClockIn.getBorder());
        SaveTanod.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        SaveTanod.setForeground(new java.awt.Color(255, 255, 255));
        SaveTanod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveTanodActionPerformed(evt);
            }
        });

        ArchiveTanod.setIcon(IconFontSwing.buildIcon(
            FontAwesome.ARCHIVE,
            15,
            java.awt.Color.WHITE
        ));
        ArchiveTanod.setText("Archive");
        ArchiveTanod.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Yellow"));
        ArchiveTanod.setBorder(ClockIn.getBorder());
        ArchiveTanod.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        ArchiveTanod.setForeground(new java.awt.Color(255, 255, 255));
        ArchiveTanod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ArchiveTanodActionPerformed(evt);
            }
        });

        tanod_first_name_label.setText("Given name:");

        tanod_last_name_label.setText("Family name:");

        tanod_middle_name_label.setText("Middle name:");

        javax.swing.GroupLayout TanodFormPanelLayout = new javax.swing.GroupLayout(TanodFormPanel);
        TanodFormPanel.setLayout(TanodFormPanelLayout);
        TanodFormPanelLayout.setHorizontalGroup(
            TanodFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TanodFormPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(TanodFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tanod_last_name)
                    .addComponent(tanod_first_name)
                    .addGroup(TanodFormPanelLayout.createSequentialGroup()
                        .addGroup(TanodFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tanod_first_name_label)
                            .addComponent(tanod_last_name_label)
                            .addComponent(tanod_middle_name_label))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(tanod_middle_name)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TanodFormPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(ArchiveTanod)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(SaveTanod)))
                .addContainerGap())
        );
        TanodFormPanelLayout.setVerticalGroup(
            TanodFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TanodFormPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tanod_first_name_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tanod_first_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(tanod_last_name_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tanod_last_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(tanod_middle_name_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tanod_middle_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(TanodFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(SaveTanod)
                    .addComponent(ArchiveTanod))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout TanodFormLayout = new javax.swing.GroupLayout(TanodForm.getContentPane());
        TanodForm.getContentPane().setLayout(TanodFormLayout);
        TanodFormLayout.setHorizontalGroup(
            TanodFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TanodFormPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        TanodFormLayout.setVerticalGroup(
            TanodFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TanodFormPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
        );

        ShiftForm.setTitle("Add Shift");
        ShiftForm.setLocation(new java.awt.Point(0, 0));
        ShiftForm.setMinimumSize(new java.awt.Dimension(300, 200));
        ShiftForm.setResizable(false);
        ShiftForm.setSize(new java.awt.Dimension(370, 290));
        ShiftForm.setLocationRelativeTo(null);

        ShiftFormPanel.setBorder(TanodFormPanel.getBorder());
        ShiftFormPanel.setName("asdasd"); // NOI18N
        ShiftFormPanel.setPreferredSize(new java.awt.Dimension(300, 221));

        SaveShift.setIcon(IconFontSwing.buildIcon(
            FontAwesome.FLOPPY_O,
            15,
            java.awt.Color.WHITE
        )
    );
    SaveShift.setText("Save");
    SaveShift.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
    SaveShift.setBorder(ClockIn.getBorder());
    SaveShift.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    SaveShift.setForeground(new java.awt.Color(255, 255, 255));
    SaveShift.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            SaveShiftActionPerformed(evt);
        }
    });

    DeleteShift.setIcon(ArchiveTanod.getIcon());
    DeleteShift.setText("Delete");
    DeleteShift.setBackground(javax.swing.UIManager.getDefaults().getColor("Component.error.borderColor"));
    DeleteShift.setBorder(ClockIn.getBorder());
    DeleteShift.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    DeleteShift.setForeground(new java.awt.Color(255, 255, 255));
    DeleteShift.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            DeleteShiftActionPerformed(evt);
        }
    });

    Shift_for.setText("Shift for:");

    Shift_starts_at.setText("Starts at:");

    Shift_ends_at.setText("Ends at:");

    Shift_starts_at_hr.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" }));

    Shift_starts_at_mn.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59" }));

    Shift_starts_at_md.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "AM", "PM" }));

    Shift_ends_at_hr.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" }));

    Shift_ends_at_mn.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59" }));

    Shift_ends_at_md.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "AM", "PM" }));

    Shift_day.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" }));

    javax.swing.GroupLayout ShiftFormPanelLayout = new javax.swing.GroupLayout(ShiftFormPanel);
    ShiftFormPanel.setLayout(ShiftFormPanelLayout);
    ShiftFormPanelLayout.setHorizontalGroup(
        ShiftFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ShiftFormPanelLayout.createSequentialGroup()
            .addGap(23, 23, 23)
            .addGroup(ShiftFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addGroup(ShiftFormPanelLayout.createSequentialGroup()
                    .addComponent(DeleteShift)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(SaveShift))
                .addGroup(ShiftFormPanelLayout.createSequentialGroup()
                    .addGroup(ShiftFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(ShiftFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(Shift_ends_at, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(Shift_starts_at, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(Shift_for, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(ShiftFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ShiftFormPanelLayout.createSequentialGroup()
                            .addComponent(Shift_ends_at_hr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(Shift_ends_at_mn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(Shift_ends_at_md, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(ShiftFormPanelLayout.createSequentialGroup()
                            .addComponent(Shift_starts_at_hr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(Shift_starts_at_mn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(Shift_starts_at_md, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(Shift_day, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    ShiftFormPanelLayout.setVerticalGroup(
        ShiftFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ShiftFormPanelLayout.createSequentialGroup()
            .addGap(14, 14, 14)
            .addGroup(ShiftFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(Shift_for)
                .addComponent(Shift_day, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(21, 21, 21)
            .addGroup(ShiftFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(Shift_starts_at)
                .addComponent(Shift_starts_at_mn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(Shift_starts_at_md, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(Shift_starts_at_hr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addGroup(ShiftFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(Shift_ends_at)
                .addComponent(Shift_ends_at_md, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(Shift_ends_at_hr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(Shift_ends_at_mn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(53, 53, 53)
            .addGroup(ShiftFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(SaveShift)
                .addComponent(DeleteShift))
            .addContainerGap(126, Short.MAX_VALUE))
    );

    javax.swing.GroupLayout ShiftFormLayout = new javax.swing.GroupLayout(ShiftForm.getContentPane());
    ShiftForm.getContentPane().setLayout(ShiftFormLayout);
    ShiftFormLayout.setHorizontalGroup(
        ShiftFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(ShiftFormPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
    );
    ShiftFormLayout.setVerticalGroup(
        ShiftFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(ShiftFormPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
    );

    ShiftForm.getAccessibleContext().setAccessibleName("");

    RestorationDialog.setTitle("New Tanod");
    RestorationDialog.setLocation(new java.awt.Point(0, 0));
    RestorationDialog.setMinimumSize(new java.awt.Dimension(300, 200));
    RestorationDialog.setResizable(false);
    RestorationDialog.setSize(new java.awt.Dimension(300, 290));
    TanodForm.setLocationRelativeTo(null);

    RestoreTanodDialog.setBackground(new java.awt.Color(255, 255, 255));
    RestoreTanodDialog.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
    RestoreTanodDialog.setName("asdasd"); // NOI18N
    RestoreTanodDialog.setPreferredSize(new java.awt.Dimension(300, 221));

    RestoreTanodBtn.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
    RestoreTanodBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    RestoreTanodBtn.setForeground(new java.awt.Color(255, 255, 255));
    RestoreTanodBtn.setIcon(IconFontSwing.buildIcon(
        FontAwesome.HISTORY,
        15,
        java.awt.Color.WHITE
    ));
    RestoreTanodBtn.setText("Restore");
    RestoreTanodBtn.setBorder(ClockIn.getBorder());
    RestoreTanodBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            RestoreTanodBtnActionPerformed(evt);
        }
    });

    DeleteTanod.setBackground(javax.swing.UIManager.getDefaults().getColor("Component.error.borderColor"));
    DeleteTanod.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    DeleteTanod.setForeground(new java.awt.Color(255, 255, 255));
    DeleteTanod.setIcon(IconFontSwing.buildIcon(
        FontAwesome.TRASH,
        15,
        java.awt.Color.WHITE
    ));
    DeleteTanod.setText("Delete");
    DeleteTanod.setBorder(ClockIn.getBorder());
    DeleteTanod.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            DeleteTanodActionPerformed(evt);
        }
    });

    tanod_first_name_label1.setText("Given name:");

    tanod_last_name_label1.setText("Family name:");

    tanod_middle_name_label1.setText("Middle name:");

    jLabel39.setText("TanodGivenName");

    jLabel40.setText("TanodFamilyName");

    jLabel41.setText("TanodMiddlename");

    javax.swing.GroupLayout RestoreTanodDialogLayout = new javax.swing.GroupLayout(RestoreTanodDialog);
    RestoreTanodDialog.setLayout(RestoreTanodDialogLayout);
    RestoreTanodDialogLayout.setHorizontalGroup(
        RestoreTanodDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(RestoreTanodDialogLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(RestoreTanodDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RestoreTanodDialogLayout.createSequentialGroup()
                    .addGap(0, 116, Short.MAX_VALUE)
                    .addComponent(DeleteTanod)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(RestoreTanodBtn))
                .addGroup(RestoreTanodDialogLayout.createSequentialGroup()
                    .addGroup(RestoreTanodDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(tanod_first_name_label1)
                        .addComponent(tanod_last_name_label1)
                        .addComponent(tanod_middle_name_label1)
                        .addComponent(jLabel39)
                        .addComponent(jLabel40)
                        .addComponent(jLabel41))
                    .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
    );
    RestoreTanodDialogLayout.setVerticalGroup(
        RestoreTanodDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(RestoreTanodDialogLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(tanod_first_name_label1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel39)
            .addGap(24, 24, 24)
            .addComponent(tanod_last_name_label1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel40)
            .addGap(24, 24, 24)
            .addComponent(tanod_middle_name_label1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel41)
            .addGap(24, 24, 24)
            .addGroup(RestoreTanodDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(RestoreTanodBtn)
                .addComponent(DeleteTanod))
            .addContainerGap(86, Short.MAX_VALUE))
    );

    javax.swing.GroupLayout RestorationDialogLayout = new javax.swing.GroupLayout(RestorationDialog.getContentPane());
    RestorationDialog.getContentPane().setLayout(RestorationDialogLayout);
    RestorationDialogLayout.setHorizontalGroup(
        RestorationDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(RestoreTanodDialog, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
    );
    RestorationDialogLayout.setVerticalGroup(
        RestorationDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(RestoreTanodDialog, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
    );

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setTitle("TM");
    getContentPane().setLayout(new java.awt.CardLayout());

    jPanel7.setOpaque(false);

    jPanel9.setAutoscrolls(true);
    jPanel9.setOpaque(false);
    jPanel9.setLayout(new java.awt.GridBagLayout());

    jPanel14.setAlignmentX(jPanel9.getWidth() / (float) 2.0);
    jPanel14.setBackground(new java.awt.Color(255, 255, 255));
    jPanel14.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 30, 20, 30));
    jPanel14.setMaximumSize(new java.awt.Dimension(300, 300));
    jPanel14.setPreferredSize(new java.awt.Dimension(300, 300));

    jLabel34.setText("Login");
    jLabel34.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
    jLabel34.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

    jLabel35.setText("Username");

    jLabel36.setText("Password");

    jButton10.setText("Login");
    jButton10.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Green"));
    jButton10.setBorder(javax.swing.BorderFactory.createCompoundBorder(ClockIn.getBorder(), javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    jButton10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    jButton10.setForeground(new java.awt.Color(255, 255, 255));
    jButton10.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton10ActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
    jPanel14.setLayout(jPanel14Layout);
    jPanel14Layout.setHorizontalGroup(
        jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(jButton10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(jPanel14Layout.createSequentialGroup()
            .addGap(83, 83, 83)
            .addComponent(jLabel34)
            .addContainerGap(93, Short.MAX_VALUE))
        .addComponent(passwordText)
        .addGroup(jPanel14Layout.createSequentialGroup()
            .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel35)
                .addComponent(jLabel36))
            .addGap(0, 0, Short.MAX_VALUE))
        .addGroup(jPanel14Layout.createSequentialGroup()
            .addComponent(userText)
            .addContainerGap())
    );
    jPanel14Layout.setVerticalGroup(
        jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel14Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel34)
            .addGap(23, 23, 23)
            .addComponent(jLabel35)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(userText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(12, 12, 12)
            .addComponent(jLabel36)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(passwordText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(33, 33, 33)
            .addComponent(jButton10)
            .addContainerGap(23, Short.MAX_VALUE))
    );

    jPanel9.add(jPanel14, new java.awt.GridBagConstraints());

    basicBackgroundPanel2.setBackground(new java.awt.Color(4, 34, 90));
    basicBackgroundPanel2.setMaximumSize(new java.awt.Dimension(50, 50));
    basicBackgroundPanel2.setPreferredSize(new java.awt.Dimension(50, 50));

    jLabel37.setText("Brgy. Kadaohan Tanod Management System");
    jLabel37.setFont(new java.awt.Font("Times New Roman", 1, 36)); // NOI18N
    jLabel37.setForeground(new java.awt.Color(255, 255, 255));

    javax.swing.GroupLayout gradientBackgroundPanel1Layout = new javax.swing.GroupLayout(gradientBackgroundPanel1);
    gradientBackgroundPanel1.setLayout(gradientBackgroundPanel1Layout);
    gradientBackgroundPanel1Layout.setHorizontalGroup(
        gradientBackgroundPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(gradientBackgroundPanel1Layout.createSequentialGroup()
            .addGap(26, 26, 26)
            .addComponent(basicBackgroundPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(jLabel37)
            .addContainerGap(407, Short.MAX_VALUE))
    );
    gradientBackgroundPanel1Layout.setVerticalGroup(
        gradientBackgroundPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(gradientBackgroundPanel1Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(gradientBackgroundPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                .addComponent(basicBackgroundPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                .addComponent(jLabel37, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
    jPanel7.setLayout(jPanel7Layout);
    jPanel7Layout.setHorizontalGroup(
        jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(gradientBackgroundPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(jPanel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1217, Short.MAX_VALUE)
    );
    jPanel7Layout.setVerticalGroup(
        jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel7Layout.createSequentialGroup()
            .addComponent(gradientBackgroundPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE))
    );

    basicBackgroundPanel1.setLayout(new javax.swing.OverlayLayout(basicBackgroundPanel1));

    javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
    jPanel6.setLayout(jPanel6Layout);
    jPanel6Layout.setHorizontalGroup(
        jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(basicBackgroundPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1217, Short.MAX_VALUE))
    );
    jPanel6Layout.setVerticalGroup(
        jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(basicBackgroundPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE))
    );

    getContentPane().add(jPanel6, "AuthenticationCard");

    jPanel4.setBackground(ClockInClockOut.getBackground());

    Main.setBackground(new java.awt.Color(255, 153, 153));
    Main.setMaximumSize(new java.awt.Dimension(880, 32767));
    Main.setPreferredSize(new java.awt.Dimension(880, 524));

    CardLayout.setLayout(new java.awt.CardLayout());

    Dashboard.setBackground(ClockInClockOut.getBackground());

    jScrollPane6.setBorder(null);
    jScrollPane6.setPreferredSize(new java.awt.Dimension(880, 880));

    jPanel5.setBackground(Dashboard.getBackground());
    jPanel5.setMinimumSize(new java.awt.Dimension(410, 74));
    jPanel5.setLayout(new java.awt.GridLayout(2, 4, 10, 10));

    jButton5.setIcon(IconFontSwing.buildIcon(
        FontAwesome.CLOCK_O,
        36,
        java.awt.Color.WHITE
    )
    );
    jButton5.setText("Clock In/Out");
    jButton5.setBackground(new java.awt.Color(32, 40, 57));
    jButton5.setBorder(null);
    jButton5.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
    jButton5.setForeground(new java.awt.Color(255, 255, 255));
    jButton5.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton5ActionPerformed(evt);
        }
    });
    jPanel5.add(jButton5);

    jButton6.setIcon(IconFontSwing.buildIcon(
        FontAwesome.USERS,
        36,
        java.awt.Color.WHITE
    )
    );
    jButton6.setText("Tanods");
    jButton6.setBackground(new java.awt.Color(246, 95, 81));
    jButton6.setBorder(null);
    jButton6.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
    jButton6.setForeground(new java.awt.Color(255, 255, 255));
    jButton6.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton6ActionPerformed(evt);
        }
    });
    jPanel5.add(jButton6);

    jButton7.setIcon(IconFontSwing.buildIcon(
        FontAwesome.CALENDAR,
        36,
        java.awt.Color.WHITE
    )
    );
    jButton7.setText("Shifts");
    jButton7.setBackground(new java.awt.Color(255, 156, 59));
    jButton7.setBorder(null);
    jButton7.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
    jButton7.setForeground(new java.awt.Color(255, 255, 255));
    jButton7.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton7ActionPerformed(evt);
        }
    });
    jPanel5.add(jButton7);

    jButton8.setIcon(IconFontSwing.buildIcon(
        FontAwesome.WPFORMS,
        36,
        java.awt.Color.WHITE
    )
    );
    jButton8.setText("Payroll");
    jButton8.setBackground(new java.awt.Color(3, 151, 128));
    jButton8.setBorder(null);
    jButton8.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
    jButton8.setForeground(new java.awt.Color(255, 255, 255));
    jButton8.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton8ActionPerformed(evt);
        }
    });
    jPanel5.add(jButton8);

    jButton9.setIcon(IconFontSwing.buildIcon(
        FontAwesome.COGS,
        36,
        java.awt.Color.WHITE
    )
    );
    jButton9.setText("System");
    jButton9.setBackground(new java.awt.Color(29, 138, 207));
    jButton9.setBorder(null);
    jButton9.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
    jButton9.setForeground(new java.awt.Color(255, 255, 255));
    jButton9.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton9ActionPerformed(evt);
        }
    });
    jPanel5.add(jButton9);

    jPanel10.setOpaque(false);

    javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
    jPanel10.setLayout(jPanel10Layout);
    jPanel10Layout.setHorizontalGroup(
        jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 240, Short.MAX_VALUE)
    );
    jPanel10Layout.setVerticalGroup(
        jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 118, Short.MAX_VALUE)
    );

    jPanel5.add(jPanel10);

    jPanel8.setOpaque(false);

    javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
    jPanel8.setLayout(jPanel8Layout);
    jPanel8Layout.setHorizontalGroup(
        jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 240, Short.MAX_VALUE)
    );
    jPanel8Layout.setVerticalGroup(
        jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 118, Short.MAX_VALUE)
    );

    jPanel5.add(jPanel8);

    jLabel33.setText("DASHBOARD");
    jLabel33.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N

    jPanel15.setOpaque(false);

    jScrollPane5.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    jScrollPane5.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    jScrollPane5.setBorder(null);
    jScrollPane5.setEnabled(false);
    jScrollPane5.setOpaque(false);

    jTextArea1.setColumns(20);
    jTextArea1.setEditable(false);
    jTextArea1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
    jTextArea1.setLineWrap(true);
    jTextArea1.setRows(5);
    jTextArea1.setText("Empowering Brgy. Tanods with a digital system fosters a safer, more organized, and resilient community by streamlining operations and enhancing transparency in barangay governance.");
    jTextArea1.setWrapStyleWord(true);
    jTextArea1.setBorder(null);
    jTextArea1.setFocusable(false);
    jTextArea1.setOpaque(false);
    jScrollPane5.setViewportView(jTextArea1);

    jLabel38.setText("MISSION STATEMENT");
    jLabel38.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N

    basicBackgroundPanel3.setEnabled(false);
    basicBackgroundPanel3.setMaximumSize(new java.awt.Dimension(880, 660));
    basicBackgroundPanel3.setMinimumSize(new java.awt.Dimension(880, 660));
    basicBackgroundPanel3.setPreferredSize(new java.awt.Dimension(880, 660));
    basicBackgroundPanel3.setRequestFocusEnabled(false);

    javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
    jPanel15.setLayout(jPanel15Layout);
    jPanel15Layout.setHorizontalGroup(
        jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel15Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane5)
                .addGroup(jPanel15Layout.createSequentialGroup()
                    .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel38, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(basicBackgroundPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
    );
    jPanel15Layout.setVerticalGroup(
        jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel15Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel38)
            .addGap(5, 5, 5)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(basicBackgroundPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 435, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
    jPanel16.setLayout(jPanel16Layout);
    jPanel16Layout.setHorizontalGroup(
        jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel16Layout.createSequentialGroup()
            .addComponent(jLabel33, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(0, 768, Short.MAX_VALUE))
        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel16Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, 993, Short.MAX_VALUE))
                .addContainerGap()))
    );
    jPanel16Layout.setVerticalGroup(
        jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel16Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel33)
            .addContainerGap(791, Short.MAX_VALUE))
        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(5, 5, 5)))
    );

    jScrollPane6.setViewportView(jPanel16);

    javax.swing.GroupLayout DashboardLayout = new javax.swing.GroupLayout(Dashboard);
    Dashboard.setLayout(DashboardLayout);
    DashboardLayout.setHorizontalGroup(
        DashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 1011, Short.MAX_VALUE)
    );
    DashboardLayout.setVerticalGroup(
        DashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(jScrollPane6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE)
    );

    CardLayout.add(Dashboard, "DashboardCard");

    ClockInClockOut.setBackground(new java.awt.Color(236, 236, 236));

    jScrollPane4.setBorder(null);

    ShiftsTable2.setDefaultEditor(Object.class, null);
    ShiftsTable2.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {},
        new String [] {
            "Attendance ID", "Tanod", "Schedule", "Clock In", "Clock Out", "Duration"
        }
    ));
    ShiftsTable2.setFocusable(false);
    ShiftsTable2.setShowGrid(false);
    ShiftsTable2.getTableHeader().setReorderingAllowed(false);
    ShiftsTable2.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            ShiftsTable2MouseClicked(evt);
        }
    });
    jScrollPane4.setViewportView(ShiftsTable2);

    ShiftsManagementContent2.setBackground(new java.awt.Color(255, 255, 255));
    ShiftsManagementContent2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 0, true));

    calendarPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            calendarPanel1MouseClicked(evt);
        }
    });
    calendarPanel1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            calendarPanel1PropertyChange(evt);
        }
    });

    jLabel5.setText("Select Date:");
    jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N

    clock_in_hr.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" }));

    clock_in_mn.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59" }));

    clock_in_md.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "AM", "PM" }));

    jLabel7.setText("Adjust Clock-In/Out");
    jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N

    jLabel8.setText("Clock In:");

    jLabel9.setText("Clock Out:");

    clock_out_hr.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" }));

    clock_out_mn.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59" }));

    clock_out_md.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "AM", "PM" }));

    ManualAdjustSave.setIcon(IconFontSwing.buildIcon(
        FontAwesome.FLOPPY_O,
        15,
        java.awt.Color.WHITE
    ));
    ManualAdjustSave.setText("Save");
    ManualAdjustSave.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
    ManualAdjustSave.setBorder(ClockIn.getBorder());
    ManualAdjustSave.setBorderPainted(false);
    ManualAdjustSave.setEnabled(false);
    ManualAdjustSave.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    ManualAdjustSave.setForeground(new java.awt.Color(255, 255, 255));
    ManualAdjustSave.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            ManualAdjustSaveActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout ShiftsManagementContent2Layout = new javax.swing.GroupLayout(ShiftsManagementContent2);
    ShiftsManagementContent2.setLayout(ShiftsManagementContent2Layout);
    ShiftsManagementContent2Layout.setHorizontalGroup(
        ShiftsManagementContent2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ShiftsManagementContent2Layout.createSequentialGroup()
            .addGroup(ShiftsManagementContent2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ShiftsManagementContent2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jSeparator2))
                .addGroup(ShiftsManagementContent2Layout.createSequentialGroup()
                    .addGap(15, 15, 15)
                    .addGroup(ShiftsManagementContent2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel5)
                        .addComponent(calendarPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
        .addGroup(ShiftsManagementContent2Layout.createSequentialGroup()
            .addGap(14, 14, 14)
            .addGroup(ShiftsManagementContent2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addComponent(ManualAdjustSave)
                .addGroup(ShiftsManagementContent2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jLabel7)
                    .addGroup(ShiftsManagementContent2Layout.createSequentialGroup()
                        .addComponent(clock_in_hr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clock_in_mn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clock_in_md, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(ShiftsManagementContent2Layout.createSequentialGroup()
                        .addComponent(clock_out_hr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clock_out_mn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clock_out_md, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel9)))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    ShiftsManagementContent2Layout.setVerticalGroup(
        ShiftsManagementContent2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ShiftsManagementContent2Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel5)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(calendarPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel7)
            .addGap(18, 18, 18)
            .addComponent(jLabel8)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(ShiftsManagementContent2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(clock_in_mn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(clock_in_md, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(clock_in_hr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addComponent(jLabel9)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(ShiftsManagementContent2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(clock_out_mn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(clock_out_md, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(clock_out_hr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addComponent(ManualAdjustSave)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    ClockIn.setIcon(IconFontSwing.buildIcon(
        FontAwesome.CLOCK_O,
        15,
        java.awt.Color.WHITE
    ));
    ClockIn.setText("Clock In");
    ClockIn.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
    ClockIn.setBorder(clock_in_hr.getBorder());
    ClockIn.setBorderPainted(false);
    ClockIn.setEnabled(false);
    ClockIn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    ClockIn.setForeground(new java.awt.Color(255, 255, 255));
    ClockIn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            ClockInActionPerformed(evt);
        }
    });

    ClockOut.setIcon(ClockIn.getIcon());
    ClockOut.setText("Clock Out");
    ClockOut.setBackground(new java.awt.Color(113, 194, 236));
    ClockOut.setBorder(ClockIn.getBorder());
    ClockOut.setEnabled(false);
    ClockOut.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    ClockOut.setForeground(new java.awt.Color(255, 255, 255));
    ClockOut.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            ClockOutActionPerformed(evt);
        }
    });

    jLabel6.setText("Shift Schedule");
    jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N

    jPanel1.setBackground(new java.awt.Color(255, 255, 255));

    LockButton.setIcon(IconFontSwing.buildIcon(
        FontAwesome.LOCK,
        15,
        java.awt.Color.WHITE
    ));
    LockButton.setText("Lock Records");
    LockButton.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Yellow"));
    LockButton.setBorder(ClockIn.getBorder());
    LockButton.setBorderPainted(false);
    LockButton.setEnabled(false);
    LockButton.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    LockButton.setForeground(new java.awt.Color(255, 255, 255));
    LockButton.setToolTipText("Lock Records: Finalize and lock all attendance records for the selected day. Once locked, attendance data for the day cannot be modified. This action ensures the data is set for payroll calculation.");
    LockButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            LockButtonActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(LockButton)
            .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(LockButton)
            .addContainerGap())
    );

    javax.swing.GroupLayout ClockInClockOutLayout = new javax.swing.GroupLayout(ClockInClockOut);
    ClockInClockOut.setLayout(ClockInClockOutLayout);
    ClockInClockOutLayout.setHorizontalGroup(
        ClockInClockOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ClockInClockOutLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(ShiftsManagementContent2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(ClockInClockOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ClockInClockOutLayout.createSequentialGroup()
                    .addComponent(jLabel6)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ClockIn)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(ClockOut))
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
            .addContainerGap())
    );
    ClockInClockOutLayout.setVerticalGroup(
        ClockInClockOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ClockInClockOutLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(ClockInClockOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(ShiftsManagementContent2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(ClockInClockOutLayout.createSequentialGroup()
                    .addGroup(ClockInClockOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ClockInClockOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ClockIn)
                            .addComponent(ClockOut))
                        .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addContainerGap())
    );

    CardLayout.add(ClockInClockOut, "ClockInClockOutCard");

    TanodManagement.setBackground(new java.awt.Color(255, 255, 255));
    TanodManagement.setBorder(javax.swing.BorderFactory.createLineBorder(ClockInClockOut.getBackground(), 10));

    TanodManagementHead.setBackground(new java.awt.Color(255, 255, 255));

    TanodSearchField.setName("Search"); // NOI18N
    TanodSearchField.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            TanodSearchFieldActionPerformed(evt);
        }
    });
    TanodSearchField.addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyPressed(java.awt.event.KeyEvent evt) {
            TanodSearchFieldKeyPressed(evt);
        }
    });

    jButton1.setIcon(IconFontSwing.buildIcon(
        FontAwesome.USER,
        15,
        java.awt.Color.WHITE
    ));
    jButton1.setText("New");
    jButton1.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
    jButton1.setBorder(ClockIn.getBorder());
    jButton1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    jButton1.setForeground(new java.awt.Color(255, 255, 255));
    jButton1.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton1ActionPerformed(evt);
        }
    });

    jLabel2.setText("Search:");

    javax.swing.GroupLayout TanodManagementHeadLayout = new javax.swing.GroupLayout(TanodManagementHead);
    TanodManagementHead.setLayout(TanodManagementHeadLayout);
    TanodManagementHeadLayout.setHorizontalGroup(
        TanodManagementHeadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(TanodManagementHeadLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel2)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(TanodSearchField, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 578, Short.MAX_VALUE)
            .addComponent(jButton1)
            .addContainerGap())
    );
    TanodManagementHeadLayout.setVerticalGroup(
        TanodManagementHeadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(TanodManagementHeadLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(TanodManagementHeadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jButton1)
                .addComponent(TanodSearchField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel2))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    TanodsTable.setDefaultEditor(Object.class, null);
    DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "ID", "First Name", "Last Name", "Middle Name", "Created At"
        }
    );
    TanodsTable.setModel(tableModel);
    fillTanodsTable();
    TanodsTable.setFocusable(false);
    TanodsTable.addFocusListener(new java.awt.event.FocusAdapter() {
        public void focusGained(java.awt.event.FocusEvent evt) {
            TanodsTableFocusGained(evt);
        }
    });
    TanodsTable.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            TanodsTableMouseClicked(evt);
        }
    });
    jScrollPane1.setViewportView(TanodsTable);

    jTabbedPane1.addTab("Tanods", jScrollPane1);

    ArchivesTable.setDefaultEditor(Object.class, null);
    DefaultTableModel archivesTableModel = new DefaultTableModel(
        new Object [][] {},
        new String [] { "ID", "First Name", "Last Name", "Middle Name", "Created At"}
    );
    ArchivesTable.setModel(archivesTableModel);
    fillTanodsTable();
    ArchivesTable.setFocusable(false);
    ArchivesTable.addFocusListener(new java.awt.event.FocusAdapter() {
        public void focusGained(java.awt.event.FocusEvent evt) {
            ArchivesTableFocusGained(evt);
        }
    });
    ArchivesTable.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            ArchivesTableMouseClicked(evt);
        }
    });
    jScrollPane7.setViewportView(ArchivesTable);

    jTabbedPane1.addTab("Archive", jScrollPane7);

    javax.swing.GroupLayout TanodManagementLayout = new javax.swing.GroupLayout(TanodManagement);
    TanodManagement.setLayout(TanodManagementLayout);
    TanodManagementLayout.setHorizontalGroup(
        TanodManagementLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(TanodManagementLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(TanodManagementLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jTabbedPane1)
                .addComponent(TanodManagementHead, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator1))
            .addContainerGap())
    );
    TanodManagementLayout.setVerticalGroup(
        TanodManagementLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TanodManagementLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(TanodManagementHead, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 551, Short.MAX_VALUE)
            .addContainerGap())
    );

    CardLayout.add(TanodManagement, "TanodManagementCard");

    ShiftsManagement.setBackground(ClockInClockOut.getBackground());

    ShiftsManagementHead.setBackground(new java.awt.Color(255, 255, 255));

    TanodSearchField2.setName("Search"); // NOI18N
    TanodSearchField2.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            TanodSearchField2ActionPerformed(evt);
        }
    });
    TanodSearchField2.addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyPressed(java.awt.event.KeyEvent evt) {
            TanodSearchField2KeyPressed(evt);
        }
    });

    jLabel3.setText("Tanod ID:");

    javax.swing.GroupLayout ShiftsManagementHeadLayout = new javax.swing.GroupLayout(ShiftsManagementHead);
    ShiftsManagementHead.setLayout(ShiftsManagementHeadLayout);
    ShiftsManagementHeadLayout.setHorizontalGroup(
        ShiftsManagementHeadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ShiftsManagementHeadLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel3)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(TanodSearchField2, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    ShiftsManagementHeadLayout.setVerticalGroup(
        ShiftsManagementHeadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ShiftsManagementHeadLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(ShiftsManagementHeadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(TanodSearchField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel3))
            .addContainerGap(7, Short.MAX_VALUE))
    );

    ShiftsManagementContent.setBackground(new java.awt.Color(255, 255, 255));

    jLabel4.setText("Tanod");
    jLabel4.setFocusable(false);
    jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N

    SMTanod_id.setText("ID:");

    SMFirst_name.setText("First Name:");

    SMLast_name.setText("Last Name:");

    SMMiddle_name.setText("Middle Name:");

    SMCreated_at.setText("Created At: ");

    UpdateTanod.setIcon(ManualAdjustSave.getIcon());
    UpdateTanod.setText("Update Info");
    UpdateTanod.setBackground(ClockIn.getBackground());
    UpdateTanod.setBorder(ClockIn.getBorder());
    UpdateTanod.setEnabled(false);
    UpdateTanod.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    UpdateTanod.setForeground(new java.awt.Color(255, 255, 255));
    UpdateTanod.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            UpdateTanodActionPerformed(evt);
        }
    });

    AddShift.setIcon(IconFontSwing.buildIcon(
        FontAwesome.CALENDAR,
        15,
        java.awt.Color.WHITE
    ));
    AddShift.setText("Add Shift");
    AddShift.setBackground(ClockIn.getBackground());
    AddShift.setBorder(ClockIn.getBorder());
    AddShift.setEnabled(false);
    AddShift.setForeground(new java.awt.Color(255, 255, 255));
    AddShift.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            AddShiftActionPerformed(evt);
        }
    });

    ShiftsTable.setDefaultEditor(Object.class, null);
    ShiftsTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {},
        new String [] {
            "ID", "Day", "Shift Start", "Shift End"
        }
    ));
    ShiftsTable.setFocusable(false);
    ShiftsTable.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            ShiftsTableMouseClicked(evt);
        }
    });
    jScrollPane2.setViewportView(ShiftsTable);

    DayOfWeekFilter.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All Days", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" }));
    DayOfWeekFilter.setEnabled(false);
    DayOfWeekFilter.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            DayOfWeekFilterActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout ShiftsManagementContentLayout = new javax.swing.GroupLayout(ShiftsManagementContent);
    ShiftsManagementContent.setLayout(ShiftsManagementContentLayout);
    ShiftsManagementContentLayout.setHorizontalGroup(
        ShiftsManagementContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ShiftsManagementContentLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(ShiftsManagementContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ShiftsManagementContentLayout.createSequentialGroup()
                    .addComponent(jScrollPane2)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(ShiftsManagementContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(AddShift, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(DayOfWeekFilter, 0, 0, Short.MAX_VALUE))
                    .addContainerGap())
                .addGroup(ShiftsManagementContentLayout.createSequentialGroup()
                    .addGroup(ShiftsManagementContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel4)
                        .addGroup(ShiftsManagementContentLayout.createSequentialGroup()
                            .addGap(6, 6, 6)
                            .addGroup(ShiftsManagementContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(SMFirst_name)
                                .addComponent(UpdateTanod)
                                .addComponent(SMTanod_id)
                                .addComponent(SMLast_name)
                                .addComponent(SMMiddle_name)
                                .addComponent(SMCreated_at))))
                    .addGap(537, 890, Short.MAX_VALUE))))
    );
    ShiftsManagementContentLayout.setVerticalGroup(
        ShiftsManagementContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ShiftsManagementContentLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel4)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(SMTanod_id)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(SMFirst_name)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(SMLast_name)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(SMMiddle_name)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(SMCreated_at)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(UpdateTanod)
            .addGap(11, 11, 11)
            .addGroup(ShiftsManagementContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ShiftsManagementContentLayout.createSequentialGroup()
                    .addComponent(DayOfWeekFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(AddShift)
                    .addGap(0, 0, Short.MAX_VALUE))
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE))
            .addContainerGap())
    );

    javax.swing.GroupLayout ShiftsManagementLayout = new javax.swing.GroupLayout(ShiftsManagement);
    ShiftsManagement.setLayout(ShiftsManagementLayout);
    ShiftsManagementLayout.setHorizontalGroup(
        ShiftsManagementLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ShiftsManagementLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(ShiftsManagementLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(ShiftsManagementHead, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(ShiftsManagementContent, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap())
    );
    ShiftsManagementLayout.setVerticalGroup(
        ShiftsManagementLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(ShiftsManagementLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(ShiftsManagementHead, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(ShiftsManagementContent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addContainerGap())
    );

    CardLayout.add(ShiftsManagement, "ShiftsManagementCard");

    Payroll.setBackground(ClockInClockOut.getBackground());

    jPanel2.setBackground(new java.awt.Color(255, 255, 255));

    SearchTanodField.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            SearchTanodFieldActionPerformed(evt);
        }
    });

    jLabel10.setText("Tanod ID");

    PayrollTable.setDefaultEditor(Object.class, null);
    PayrollTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {},
        new String [] {
            "ID", "Tanod", "IssuedOn"
        }
    ));
    PayrollTable.setFocusable(false);
    FillPayrollTable();
    PayrollTable.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            PayrollTableMouseClicked(evt);
        }
    });
    jScrollPane3.setViewportView(PayrollTable);

    payrollStart.setText("January 1, 2024");
    payrollStart.setBackground(new java.awt.Color(255, 255, 255));
    payrollStart.setBorder(javax.swing.BorderFactory.createCompoundBorder(clock_in_hr.getBorder(), javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1)));
    payrollStart.getComponentDateTextField().setEnabled(false);
    payrollStart.getComponentDateTextField().setBorder(null);

    payrollStart.getComponentToggleCalendarButton().setBorder(javax.swing.BorderFactory.createCompoundBorder(
        clock_in_hr.getBorder(), javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5)
    ));
    payrollStart.getComponentToggleCalendarButton().setText("");
    payrollStart.getComponentToggleCalendarButton().setBackground(java.awt.Color.decode("#007bfe"));

    payrollStart.getComponentToggleCalendarButton().setIcon(
        IconFontSwing.buildIcon(
            FontAwesome.CALENDAR,
            10,
            java.awt.Color.WHITE
        )
    );

    payrollEnd.setText("March 1, 2024");
    payrollEnd.setBackground(new java.awt.Color(255, 255, 255));
    payrollEnd.getComponentDateTextField().setEnabled(false);
    payrollEnd.getComponentDateTextField().setBorder(null);

    payrollEnd.getComponentToggleCalendarButton().setBorder(
        payrollStart.getComponentToggleCalendarButton().getBorder()
    );
    payrollEnd.getComponentToggleCalendarButton().setText("");
    payrollEnd.getComponentToggleCalendarButton().setBackground(
        payrollStart.getComponentToggleCalendarButton().getBackground()
    );

    payrollEnd.getComponentToggleCalendarButton().setIcon(
        payrollStart.getComponentToggleCalendarButton().getIcon()
    );
    payrollEnd.setBorder(payrollStart.getBorder());

    jButton2.setIcon(IconFontSwing.buildIcon(
        FontAwesome.EYE,
        15,
        java.awt.Color.WHITE
    ));
    jButton2.setText("Preview");
    jButton2.setBackground(ClockOut.getBackground());
    jButton2.setBorder(ClockIn.getBorder());
    jButton2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    jButton2.setForeground(new java.awt.Color(255, 255, 255));
    jButton2.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton2ActionPerformed(evt);
        }
    });

    jLabel23.setText("Generate Salary Slip");
    jLabel23.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

    jLabel24.setText("Start:");

    jLabel25.setText("End:");

    jLabel26.setText("All Issued Salary Slips");
    jLabel26.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

    jButton3.setIcon(ManualAdjustSave.getIcon());
    jButton3.setText("Save");
    jButton3.setBackground(ClockIn.getBackground());
    jButton3.setBorder(ClockIn.getBorder());
    jButton3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    jButton3.setForeground(new java.awt.Color(255, 255, 255));
    jButton3.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton3ActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
    jPanel2.setLayout(jPanel2Layout);
    jPanel2Layout.setHorizontalGroup(
        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel2Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE)
                .addComponent(jSeparator6)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(jButton2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButton3))
                        .addComponent(jLabel23)
                        .addComponent(jLabel26)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(jLabel10)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(SearchTanodField, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(jLabel24)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(payrollStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jLabel25)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(payrollEnd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
    );
    jPanel2Layout.setVerticalGroup(
        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel2Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel23)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel10)
                .addComponent(SearchTanodField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(11, 11, 11)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel24)
                .addComponent(payrollStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel25)
                .addComponent(payrollEnd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jButton2)
                .addComponent(jButton3))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel26)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addContainerGap())
    );

    jPanel3.setBackground(new java.awt.Color(255, 255, 255));

    jLabel11.setText("Salary Receipt");
    jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
    jLabel11.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

    jLabel12.setText("ID");
    jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel13.setText("Name");
    jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel14.setText("Period");
    jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel15.setText("Generated On");
    jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel16.setText(":");
    jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel17.setText(":");
    jLabel17.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel18.setText(":");
    jLabel18.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel19.setText(":");
    jLabel19.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel20.setText("Gross");
    jLabel20.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel21.setText("Deductions");
    jLabel21.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel22.setText("Net");
    jLabel22.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel27.setText(":");
    jLabel27.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel28.setText(":");
    jLabel28.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel29.setText(":");
    jLabel29.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
    jPanel3.setLayout(jPanel3Layout);
    jPanel3Layout.setHorizontalGroup(
        jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel3Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel15)
                        .addComponent(jLabel14)
                        .addComponent(jLabel13)
                        .addComponent(jLabel12))
                    .addGap(27, 27, 27)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addComponent(jLabel16)
                            .addGap(18, 18, 18)
                            .addComponent(IssuedOnLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabel19, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGap(18, 18, 18)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(TanodIdLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(TanodFullName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(PayrollPeriodLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel22, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel20, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel21, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(36, 36, 36)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel28, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel29, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel27, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGap(18, 18, 18)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(GrossLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(DeductionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(NetLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addComponent(jSeparator4)
                .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator5))
            .addContainerGap())
    );
    jPanel3Layout.setVerticalGroup(
        jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel3Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel11)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel12)
                .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(TanodIdLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel13)
                .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(TanodFullName))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel14)
                .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(PayrollPeriodLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel15)
                .addComponent(jLabel16)
                .addComponent(IssuedOnLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addComponent(jLabel20)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jLabel21)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jLabel22))
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(GrossLabel))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(DeductionLabel))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(NetLabel))))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    javax.swing.GroupLayout PayrollLayout = new javax.swing.GroupLayout(Payroll);
    Payroll.setLayout(PayrollLayout);
    PayrollLayout.setHorizontalGroup(
        PayrollLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(PayrollLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
    );
    PayrollLayout.setVerticalGroup(
        PayrollLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(PayrollLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(PayrollLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(PayrollLayout.createSequentialGroup()
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 313, Short.MAX_VALUE)))
            .addContainerGap())
    );

    CardLayout.add(Payroll, "PayrollCard");

    System.setBackground(ClockInClockOut.getBackground());

    UpdateAdminCredentials.setBackground(new java.awt.Color(255, 255, 255));
    UpdateAdminCredentials.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

    jLabel30.setText("Update Admin Credentials");
    jLabel30.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

    jLabel31.setText("Username");

    jLabel32.setText("Password");

    jButton4.setIcon(ManualAdjustSave.getIcon());
    jButton4.setText("Update");
    jButton4.setBackground(ClockIn.getBackground());
    jButton4.setBorder(ClockIn.getBorder());
    jButton4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
    jButton4.setForeground(new java.awt.Color(255, 255, 255));
    jButton4.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton4ActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout UpdateAdminCredentialsLayout = new javax.swing.GroupLayout(UpdateAdminCredentials);
    UpdateAdminCredentials.setLayout(UpdateAdminCredentialsLayout);
    UpdateAdminCredentialsLayout.setHorizontalGroup(
        UpdateAdminCredentialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(UpdateAdminCredentialsLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(UpdateAdminCredentialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(password)
                .addGroup(UpdateAdminCredentialsLayout.createSequentialGroup()
                    .addGroup(UpdateAdminCredentialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, 297, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel31)
                        .addComponent(jLabel32))
                    .addGap(0, 40, Short.MAX_VALUE))
                .addComponent(username)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UpdateAdminCredentialsLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jButton4)))
            .addContainerGap())
    );
    UpdateAdminCredentialsLayout.setVerticalGroup(
        UpdateAdminCredentialsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(UpdateAdminCredentialsLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel30)
            .addGap(8, 8, 8)
            .addComponent(jLabel31)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(17, 17, 17)
            .addComponent(jLabel32)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(password, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(jButton4)
            .addContainerGap(20, Short.MAX_VALUE))
    );

    javax.swing.GroupLayout SystemLayout = new javax.swing.GroupLayout(System);
    System.setLayout(SystemLayout);
    SystemLayout.setHorizontalGroup(
        SystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(SystemLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(UpdateAdminCredentials, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(636, Short.MAX_VALUE))
    );
    SystemLayout.setVerticalGroup(
        SystemLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(SystemLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(UpdateAdminCredentials, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(409, Short.MAX_VALUE))
    );

    CardLayout.add(System, "SystemCard");

    javax.swing.GroupLayout MainLayout = new javax.swing.GroupLayout(Main);
    Main.setLayout(MainLayout);
    MainLayout.setHorizontalGroup(
        MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 1011, Short.MAX_VALUE)
        .addGroup(MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(CardLayout, javax.swing.GroupLayout.DEFAULT_SIZE, 880, Short.MAX_VALUE))
    );
    MainLayout.setVerticalGroup(
        MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 640, Short.MAX_VALUE)
        .addGroup(MainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(CardLayout, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 524, Short.MAX_VALUE))
    );

    SidePanel.setBackground(new java.awt.Color(32, 40, 57));
    SidePanel.setMaximumSize(new java.awt.Dimension(200, 32767));
    SidePanel.setPreferredSize(new java.awt.Dimension(200, 0));
    SidePanel.setOpaque(false);
    SidePanel.setLayout(new java.awt.GridLayout(2, 0));

    jPanel11.setBackground(new java.awt.Color(32, 40, 57));
    jPanel11.setLayout(new java.awt.GridLayout(8, 0));

    jPanel13.setBackground(new java.awt.Color(246, 95, 81));
    jPanel13.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));

    jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabel1.setText("Brgy. Tanod Management System");
    jLabel1.setFont(new java.awt.Font("Gill Sans Ultra Bold Condensed", 0, 12)); // NOI18N
    jLabel1.setForeground(new java.awt.Color(255, 255, 255));

    javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
    jPanel13.setLayout(jPanel13Layout);
    jPanel13Layout.setHorizontalGroup(
        jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
    );
    jPanel13Layout.setVerticalGroup(
        jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
    );

    jPanel11.add(jPanel13);

    DashboardBtn.setBackground(SidePanel.getBackground());
    DashboardBtn.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
    DashboardBtn.setForeground(new java.awt.Color(255, 255, 255));
    DashboardBtn.setIcon(IconFontSwing.buildIcon(
        FontAwesome.TACHOMETER,
        14,
        java.awt.Color.WHITE
    )
    );
    DashboardBtn.setText("Dashboard");
    DashboardBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 20, 10, 20));
    DashboardBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    DashboardBtn.setIconTextGap(15);
    DashboardBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            DashboardBtnActionPerformed(evt);
        }
    });
    jPanel11.add(DashboardBtn);

    ClockInClockOutBtn.setBackground(DashboardBtn.getBackground());
    ClockInClockOutBtn.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
    ClockInClockOutBtn.setForeground(DashboardBtn.getForeground());
    ClockInClockOutBtn.setIcon(IconFontSwing.buildIcon(
        FontAwesome.CLOCK_O,
        14,
        java.awt.Color.WHITE
    ));
    ClockInClockOutBtn.setText("Clock In/Out");
    ClockInClockOutBtn.setBorder(DashboardBtn.getBorder());
    ClockInClockOutBtn.setHorizontalAlignment(DashboardBtn.getHorizontalAlignment());
    ClockInClockOutBtn.setHorizontalTextPosition(DashboardBtn.getHorizontalTextPosition());
    ClockInClockOutBtn.setIconTextGap(DashboardBtn.getIconTextGap());
    ClockInClockOutBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            ClockInClockOutBtnActionPerformed(evt);
        }
    });
    jPanel11.add(ClockInClockOutBtn);

    TanodsBtn.setBackground(DashboardBtn.getBackground());
    TanodsBtn.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
    TanodsBtn.setForeground(DashboardBtn.getForeground());
    TanodsBtn.setIcon(IconFontSwing.buildIcon(
        FontAwesome.USERS,
        14,
        java.awt.Color.WHITE
    )
    );
    TanodsBtn.setText("Tanods");
    TanodsBtn.setBorder(DashboardBtn.getBorder());
    TanodsBtn.setHorizontalAlignment(DashboardBtn.getHorizontalAlignment());
    TanodsBtn.setHorizontalTextPosition(DashboardBtn.getHorizontalTextPosition());
    TanodsBtn.setIconTextGap(DashboardBtn.getIconTextGap());
    TanodsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            TanodsBtnActionPerformed(evt);
        }
    });
    jPanel11.add(TanodsBtn);

    ShiftsBtn.setBackground(DashboardBtn.getBackground());
    ShiftsBtn.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
    ShiftsBtn.setForeground(DashboardBtn.getForeground());
    ShiftsBtn.setIcon(IconFontSwing.buildIcon(
        FontAwesome.CALENDAR,
        14,
        java.awt.Color.WHITE
    )
    );
    ShiftsBtn.setText("Shifts");
    ShiftsBtn.setBorder(DashboardBtn.getBorder());
    ShiftsBtn.setHorizontalAlignment(DashboardBtn.getHorizontalAlignment());
    ShiftsBtn.setHorizontalTextPosition(DashboardBtn.getHorizontalTextPosition());
    ShiftsBtn.setIconTextGap(DashboardBtn.getIconTextGap());
    ShiftsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            ShiftsBtnActionPerformed(evt);
        }
    });
    jPanel11.add(ShiftsBtn);

    PayrollBtn.setBackground(DashboardBtn.getBackground());
    PayrollBtn.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
    PayrollBtn.setForeground(DashboardBtn.getForeground());
    PayrollBtn.setIcon(IconFontSwing.buildIcon(
        FontAwesome.WPFORMS,
        14,
        java.awt.Color.WHITE
    )
    );
    PayrollBtn.setText("Payroll");
    PayrollBtn.setBorder(DashboardBtn.getBorder());
    PayrollBtn.setHorizontalAlignment(DashboardBtn.getHorizontalAlignment());
    PayrollBtn.setHorizontalTextPosition(DashboardBtn.getHorizontalTextPosition());
    PayrollBtn.setIconTextGap(DashboardBtn.getIconTextGap());
    PayrollBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            PayrollBtnActionPerformed(evt);
        }
    });
    jPanel11.add(PayrollBtn);

    SystemBtn.setBackground(DashboardBtn.getBackground());
    SystemBtn.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
    SystemBtn.setForeground(DashboardBtn.getForeground());
    SystemBtn.setIcon(IconFontSwing.buildIcon(
        FontAwesome.COGS,
        14,
        java.awt.Color.WHITE
    )
    );
    SystemBtn.setText("System");
    SystemBtn.setBorder(DashboardBtn.getBorder());
    SystemBtn.setHorizontalAlignment(DashboardBtn.getHorizontalAlignment());
    SystemBtn.setHorizontalTextPosition(DashboardBtn.getHorizontalTextPosition());
    SystemBtn.setIconTextGap(DashboardBtn.getIconTextGap());
    SystemBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            SystemBtnActionPerformed(evt);
        }
    });
    jPanel11.add(SystemBtn);

    SystemBtn1.setBackground(DashboardBtn.getBackground());
    SystemBtn1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
    SystemBtn1.setForeground(DashboardBtn.getForeground());
    SystemBtn1.setIcon(IconFontSwing.buildIcon(
        FontAwesome.SIGN_OUT,
        14,
        java.awt.Color.WHITE
    )
    );
    SystemBtn1.setText("Sign Out");
    SystemBtn1.setBorder(DashboardBtn.getBorder());
    SystemBtn1.setHorizontalAlignment(DashboardBtn.getHorizontalAlignment());
    SystemBtn1.setHorizontalTextPosition(DashboardBtn.getHorizontalTextPosition());
    SystemBtn1.setIconTextGap(DashboardBtn.getIconTextGap());
    SystemBtn1.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            SystemBtn1ActionPerformed(evt);
        }
    });
    jPanel11.add(SystemBtn1);

    SidePanel.add(jPanel11);

    jPanel12.setBackground(new java.awt.Color(32, 40, 57));

    javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
    jPanel12.setLayout(jPanel12Layout);
    jPanel12Layout.setHorizontalGroup(
        jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 200, Short.MAX_VALUE)
    );
    jPanel12Layout.setVerticalGroup(
        jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 0, Short.MAX_VALUE)
    );

    SidePanel.add(jPanel12);

    javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
    jPanel4.setLayout(jPanel4Layout);
    jPanel4Layout.setHorizontalGroup(
        jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
            .addComponent(SidePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(Main, javax.swing.GroupLayout.DEFAULT_SIZE, 1011, Short.MAX_VALUE))
    );
    jPanel4Layout.setVerticalGroup(
        jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(Main, javax.swing.GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE)
        .addComponent(SidePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
    );

    getContentPane().add(jPanel4, "AuthenticatedCard");

    pack();
    }// </editor-fold>//GEN-END:initComponents

    private void TanodSearchFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TanodSearchFieldActionPerformed
    }//GEN-LAST:event_TanodSearchFieldActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        appState.remove("editingTanodId");
        tanod_first_name.setText(""); tanod_last_name.setText(""); tanod_middle_name.setText("");
        ArchiveTanod.setVisible(false);
        TanodForm.setTitle("New Tanod");
        TanodForm.setVisible(true);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void SaveTanodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveTanodActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            Tanod tanod = appState.get("editingTanodId") != null ? session.get(Tanod.class, appState.get("editingTanodId")) : new Tanod();

            tanod.setFirstname(tanod_first_name.getText());
            tanod.setLastname(tanod_last_name.getText());
            tanod.setMiddlename(tanod_middle_name.getText());
                
            if(appState.get("editingTanodId") != null){
                session.persist(tanod);
                
                // Reset Shift Management Tanod View
                SMFirst_name.setText("First Name: " + tanod.getFirstname());
                SMLast_name.setText("Last Name: " + tanod.getLastname());
                SMMiddle_name.setText("Middle Name: " + tanod.getMiddlename());
            }
            else{
                session.merge(tanod);
            }            
            session.getTransaction().commit();
            session.close();
        }
        
        if(TanodSearchField.getText() != null){
            try{
                fillTanodsTable(Long.parseLong(TanodSearchField.getText()));
            }
            catch(NumberFormatException e){
                fillTanodsTable(TanodSearchField.getText());
            }
        }
        else{
            fillTanodsTable();
        }
        
        TanodForm.setVisible(false);
    }//GEN-LAST:event_SaveTanodActionPerformed

    private void tanod_middle_nameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tanod_middle_nameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tanod_middle_nameActionPerformed

    private void ArchiveTanodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ArchiveTanodActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            Tanod tanod = session.find(Tanod.class, appState.get("editingTanodId"));
            tanod.archive();
            session.merge(tanod);
            session.getTransaction().commit();
            session.close();
        }
        
        // Reset all dependent components
        SMTanod_id.setText("ID: ");
        SMFirst_name.setText("First Name: ");
        SMLast_name.setText("Last Name: ");
        SMMiddle_name.setText("Middle Name: ");
        SMCreated_at.setText("Created At: ");
        
        UpdateTanod.setEnabled(false);
        DayOfWeekFilter.setEnabled(false);
        AddShift.setEnabled(false);
        
        appState.remove("editingTanodId");
        
        if(TanodSearchField.getText() != null){
            try{
                fillTanodsTable(Long.parseLong(TanodSearchField.getText()));
            }
            catch(NumberFormatException e){
                fillTanodsTable(TanodSearchField.getText());
            }
        }
        else{
            fillTanodsTable();
        }
        
        tanod_first_name.setText(""); tanod_last_name.setText(""); tanod_middle_name.setText("");
        TanodForm.setVisible(false);
    }//GEN-LAST:event_ArchiveTanodActionPerformed

    private void TanodSearchFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TanodSearchFieldKeyPressed
        // TODO add your handling code here:
        if(evt.getKeyCode() == KeyEvent.VK_ENTER){
            try{
                fillTanodsTable(Long.parseLong(TanodSearchField.getText()));
            }
            catch(NumberFormatException e){
                fillTanodsTable(TanodSearchField.getText());
            }
        }
    }//GEN-LAST:event_TanodSearchFieldKeyPressed

    private void UpdateTanodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UpdateTanodActionPerformed
        // TODO add your handling code here:
        TanodForm.setVisible(true);
    }//GEN-LAST:event_UpdateTanodActionPerformed

    private void SaveShiftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveShiftActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            Tanod tanod = session.find(Tanod.class, appState.get("editingTanodId"));
            Shift shift = appState.get("editingShiftId") != null ? session.find(Shift.class, appState.get("editingShiftId")) : new Shift();
            
            shift.setDayOfWeek(Shift_day.getSelectedItem().toString());
            shift.setShiftStart(TimeHelpers.toLocalTime(Shift_starts_at_hr, Shift_starts_at_mn, Shift_starts_at_md));
            shift.setShiftEnd(TimeHelpers.toLocalTime(Shift_ends_at_hr, Shift_ends_at_mn, Shift_ends_at_md));
            
            if(appState.get("editingShiftId") != null){
                session.merge(shift);
            } else {
                shift.setTanod(tanod);
                session.persist(shift);
            }
            
            session.getTransaction().commit();
            
            fillShiftsTable();
            
            session.close();
        }
        ShiftForm.setVisible(false);
    }//GEN-LAST:event_SaveShiftActionPerformed

    private void AddShiftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddShiftActionPerformed
        // TODO add your handling code here:
        DeleteShift.setVisible(false);
        ShiftForm.setTitle("Add Shift");
        appState.remove("editingShiftId");
        ShiftForm.setVisible(true);
    }//GEN-LAST:event_AddShiftActionPerformed

    private void ShiftsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ShiftsTableMouseClicked
        // TODO add your handling code here:
        int row = ShiftsTable.getSelectedRow();
        boolean is_archived = false;
        appState.put("editingShiftId", ShiftsTable.getValueAt(row, 0));
        
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            
            Shift shift = session.find(Shift.class, appState.get("editingShiftId"));
            is_archived = shift.getTanod().is_archived();
        
            session.close();
        }
        
        if(!is_archived){
            Shift_day.setSelectedItem(ShiftsTable.getValueAt(row, 1));

            String[] starts_at = TimeHelpers.splitTime(ShiftsTable.getValueAt(row, 2).toString());
            Shift_starts_at_hr.setSelectedItem(starts_at[0]);
            Shift_starts_at_mn.setSelectedItem(starts_at[1]);
            Shift_starts_at_md.setSelectedItem(starts_at[2]);

            String[] ends_at = TimeHelpers.splitTime(ShiftsTable.getValueAt(row, 3).toString());
            Shift_ends_at_hr.setSelectedItem(ends_at[0]);
            Shift_ends_at_mn.setSelectedItem(ends_at[1]);
            Shift_ends_at_md.setSelectedItem(ends_at[2]);

            ShiftForm.setTitle("Update Shift");
            ShiftForm.setVisible(true);
            DeleteShift.setVisible(true);
        }
    }//GEN-LAST:event_ShiftsTableMouseClicked

    private void DeleteShiftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteShiftActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            Shift shift = session.find(Shift.class, appState.get("editingShiftId"));
            session.remove(shift);
            session.getTransaction().commit();
            
            fillShiftsTable();
            
            
            session.close();
        }
        ShiftForm.setVisible(false);
    }//GEN-LAST:event_DeleteShiftActionPerformed

    private void DayOfWeekFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DayOfWeekFilterActionPerformed
        // TODO add your handling code here:
        fillShiftsTable();
    }//GEN-LAST:event_DayOfWeekFilterActionPerformed

    private void TanodsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TanodsTableMouseClicked
        // TODO add your handling code here:
        int row = TanodsTable.getSelectedRow();

        appState.put("editingTanodId", TanodsTable.getValueAt(row, 0));

        tanod_first_name.setText((String) TanodsTable.getValueAt(row, 1));
        tanod_last_name.setText((String) TanodsTable.getValueAt(row, 2));
        tanod_middle_name.setText((String) TanodsTable.getValueAt(row, 3));
        TanodForm.setTitle("Update Tanod");
        TanodForm.setVisible(true);
        ArchiveTanod.setVisible(true);
    }//GEN-LAST:event_TanodsTableMouseClicked

    private void TanodsTableFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_TanodsTableFocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_TanodsTableFocusGained

    private void TanodSearchField2KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TanodSearchField2KeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_TanodSearchField2KeyPressed

    private void TanodSearchField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TanodSearchField2ActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            Long id = Long.parseLong(TanodSearchField2.getText());
            Tanod tanod = session.find(Tanod.class, id);

            if(tanod != null){
                SMTanod_id.setText("ID: " + tanod.getId());
                SMFirst_name.setText("First Name: " + tanod.getFirstname());
                SMLast_name.setText("Last Name: " + tanod.getLastname());
                SMMiddle_name.setText("Middle Name: " + tanod.getMiddlename());
                SMCreated_at.setText("Created At: " + tanod.getCreatedAt());
                
                if(tanod.is_archived()){
                    UpdateTanod.setEnabled(false);
                    AddShift.setEnabled(false);
                }
                else{
                    UpdateTanod.setEnabled(true);
                    AddShift.setEnabled(true);    
                }
                
                DayOfWeekFilter.setEnabled(true);


                // Fill TanodForm
                tanod_first_name.setText(tanod.getFirstname());
                tanod_last_name.setText(tanod.getLastname());
                tanod_middle_name.setText(tanod.getMiddlename());
                TanodForm.setTitle("Update Tanod");
                ArchiveTanod.setVisible(false);
                appState.put("editingTanodId", tanod.getId());

                fillShiftsTable();
            }

            session.getTransaction().commit();
            session.close();
        }
    }//GEN-LAST:event_TanodSearchField2ActionPerformed

    private void ClockInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockInActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            
            Attendance attendance = session.find(Attendance.class, appState.get("currentEditableAttendanceId"));
            
            attendance.setActualClockIn(LocalTime.now());
            
            session.merge(attendance);
            
            String[] starts_at = TimeHelpers.splitTime(TimeHelpers.toAMPM(attendance.getActualClockIn()));
            clock_in_hr.setSelectedItem(starts_at[0]);
            clock_in_mn.setSelectedItem(starts_at[1]);
            clock_in_md.setSelectedItem(starts_at[2]);
            
            session.getTransaction().commit();
            session.close();
        }

        fillShiftsAttendancingTable();
    }//GEN-LAST:event_ClockInActionPerformed

    private void ShiftsTable2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ShiftsTable2MouseClicked
        // TODO add your handling code here:
        if(ShiftsTable2.isEnabled()){
            try(Session session = sessionFactory.openSession()){
                session.beginTransaction();

                Long id = (Long) ShiftsTable2.getValueAt(ShiftsTable2.getSelectedRow(), 0);

                Attendance attendance = session.find(Attendance.class, id);

                appState.put("currentEditableAttendanceId", id);

                ManualAdjustSave.setEnabled(true);
                ClockIn.setEnabled(true);
                ClockOut.setEnabled(true);

                String[] starts_at = TimeHelpers.splitTime(TimeHelpers.toAMPM(attendance.getActualClockIn()));
                clock_in_hr.setSelectedItem(starts_at[0]);
                clock_in_mn.setSelectedItem(starts_at[1]);
                clock_in_md.setSelectedItem(starts_at[2]);

                String[] ends_at = TimeHelpers.splitTime(TimeHelpers.toAMPM(attendance.getActualClockOut()));
                clock_out_hr.setSelectedItem(ends_at[0]);
                clock_out_mn.setSelectedItem(ends_at[1]);
                clock_out_md.setSelectedItem(ends_at[2]);

                session.getTransaction().commit();
                session.close();
            }
        }
    }//GEN-LAST:event_ShiftsTable2MouseClicked

    private void ClockOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockOutActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            
            Attendance attendance = session.find(Attendance.class, appState.get("currentEditableAttendanceId"));
            
            attendance.setActualClockOut(LocalTime.now());
            
            session.merge(attendance);
            
            String[] ends_at = TimeHelpers.splitTime(TimeHelpers.toAMPM(attendance.getActualClockOut()));
            clock_out_hr.setSelectedItem(ends_at[0]);
            clock_out_mn.setSelectedItem(ends_at[1]);
            clock_out_md.setSelectedItem(ends_at[2]);
            
            session.getTransaction().commit();
            session.close();
        }

        fillShiftsAttendancingTable();
    }//GEN-LAST:event_ClockOutActionPerformed

    private void calendarPanel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_calendarPanel1MouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_calendarPanel1MouseClicked

    private void calendarPanel1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_calendarPanel1PropertyChange
        // TODO add your handling code here:
        if(calendarPanel1.getSelectedDate() != null){
            Boolean is_locked = false;
            try(Session session = sessionFactory.openSession()){
                session.beginTransaction();
                
                Attendance attendance = session.createQuery("FROM Attendance a WHERE a.date = :date", Attendance.class)
                        .setMaxResults(1)
                        .setParameter("date", Date.from(calendarPanel1.getSelectedDate().atStartOfDay().toInstant(ZoneOffset.UTC)))
                        .getSingleResultOrNull();
                
                is_locked = attendance != null && attendance.isLocked();

                session.getTransaction().commit();
                session.close();
            }
            
            LockButton.setEnabled(!is_locked);
            if(is_locked){
                fillReadOnlyAttendances();
            } else {
                fillShiftsAttendancingTable();
            }
        }
    }//GEN-LAST:event_calendarPanel1PropertyChange

    private void ManualAdjustSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ManualAdjustSaveActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            
            Attendance attendance = session.find(Attendance.class, appState.get("currentEditableAttendanceId"));
            
            attendance.setActualClockIn(
                    TimeHelpers.toLocalTime(clock_in_hr, clock_in_mn, clock_in_md)
            );
            
            attendance.setActualClockOut(
                    TimeHelpers.toLocalTime(clock_out_hr, clock_out_mn, clock_out_md)
            );
            
            session.merge(attendance);
            
            session.getTransaction().commit();
            session.close();
        }
        fillShiftsAttendancingTable();
    }//GEN-LAST:event_ManualAdjustSaveActionPerformed

    private void LockButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LockButtonActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            
            List<Attendance> attendances = session.createQuery("FROM Attendance a WHERE a.date = :date", Attendance.class)
                    .setParameter("date", Date.from(calendarPanel1.getSelectedDate().atStartOfDay().toInstant(ZoneOffset.UTC)))
                    .getResultList();
            
            for(Attendance attendance : attendances){
                attendance.removeShift();
                session.merge(attendance);
            }
            
            session.getTransaction().commit();
            
            fillReadOnlyAttendances();
            
            session.close();
        }
    }//GEN-LAST:event_LockButtonActionPerformed

    private void fillSalarySlip(Payroll payroll){
        TanodIdLabel.setText(payroll.getTanod().getId().toString());
        TanodFullName.setText(payroll.getTanodFullName());
        PayrollPeriodLabel.setText(payroll.getPeriod());
        IssuedOnLabel.setText(payroll.getIssuedOn().toString());
        GrossLabel.setText("Php " + SALARY);
        DeductionLabel.setText("Php " + String.format("%.2f", payroll.getDeduction()));
        NetLabel.setText("Php " + String.format("%.2f", payroll.getNetSalary()));
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();


            Tanod tanod = session.find(Tanod.class, (Long) Long.parseLong(SearchTanodField.getText()));

            if(tanod != null){
                Payroll payroll = new Payroll(payrollStart.getDate(), payrollEnd.getDate(), tanod);
                payroll.preview(session);
                fillSalarySlip(payroll);
            }
            session.getTransaction().commit();
            session.close();
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void SearchTanodFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchTanodFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SearchTanodFieldActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();


            Tanod tanod = session.find(Tanod.class, (Long) Long.parseLong(SearchTanodField.getText()));

            if(tanod != null){
                Payroll payroll = new Payroll(payrollStart.getDate(), payrollEnd.getDate(), tanod);
                payroll.preview(session);
                fillSalarySlip(payroll);
                session.persist(payroll);
            }
            session.getTransaction().commit();
            session.close();

            FillPayrollTable();
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void PayrollTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_PayrollTableMouseClicked
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            
            Long id = (Long) PayrollTable.getValueAt(PayrollTable.getSelectedRow(), 0);
            
            Payroll payroll = session.find(Payroll.class, id);
            fillSalarySlip(payroll);
            session.getTransaction().commit();
            session.close();
        }
    }//GEN-LAST:event_PayrollTableMouseClicked

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
        
        String newUsername = username.getText().trim();
        String newPassword = password.getText().trim();

        if (newUsername.isEmpty() || newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username or password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Update admin credentials
        AdminLogin.updateAdminCredentials(newUsername, newPassword);

        // Show success message
        JOptionPane.showMessageDialog(this, "Admin credentials updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "ClockInClockOutCard");
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "TanodManagementCard");
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "ShiftsManagementCard");
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "PayrollCard");
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "SystemCard");
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        String username = userText.getText();
        String password = new String(passwordText.getPassword());

        if (AdminLogin.authenticate(username, password)) {
            // Successfully authenticated, launch the main application
            jPanel6.setVisible(false);
            jPanel4.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(TanodManagement, "Invalid Username or Password.");
        }
    }//GEN-LAST:event_jButton10ActionPerformed

    private void DashboardBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DashboardBtnActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "DashboardCard");
    }//GEN-LAST:event_DashboardBtnActionPerformed

    private void ShiftsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShiftsBtnActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "ShiftsManagementCard");
    }//GEN-LAST:event_ShiftsBtnActionPerformed

    private void ClockInClockOutBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockInClockOutBtnActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "ClockInClockOutCard");
    }//GEN-LAST:event_ClockInClockOutBtnActionPerformed

    private void TanodsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TanodsBtnActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "TanodManagementCard");
    }//GEN-LAST:event_TanodsBtnActionPerformed

    private void PayrollBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PayrollBtnActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "PayrollCard");
    }//GEN-LAST:event_PayrollBtnActionPerformed

    private void SystemBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SystemBtnActionPerformed
        // TODO add your handling code here:
        ((java.awt.CardLayout) CardLayout.getLayout()).show(CardLayout, "SystemCard");
    }//GEN-LAST:event_SystemBtnActionPerformed

    private void SystemBtn1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SystemBtn1ActionPerformed
        // TODO add your handling code here:
        jPanel4.setVisible(false);
        jPanel6.setVisible(true);
    }//GEN-LAST:event_SystemBtn1ActionPerformed

    private void ArchivesTableFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_ArchivesTableFocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_ArchivesTableFocusGained

    private void ArchivesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ArchivesTableMouseClicked
        // TODO add your handling code here:
        int row = ArchivesTable.getSelectedRow();

        appState.put("editingTanodId", ArchivesTable.getValueAt(row, 0));

        jLabel39.setText((String) ArchivesTable.getValueAt(row, 1));
        jLabel40.setText((String) ArchivesTable.getValueAt(row, 2));
        jLabel41.setText((String) ArchivesTable.getValueAt(row, 3));
        RestorationDialog.setTitle("Restore/Delete Tanod");
        RestorationDialog.setVisible(true);
        ArchiveTanod.setVisible(true);
    }//GEN-LAST:event_ArchivesTableMouseClicked

    private void DeleteTanodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteTanodActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            Tanod tanod = session.find(Tanod.class, appState.get("editingTanodId"));
            session.remove(tanod);
            session.getTransaction().commit();
            session.close();
        }
        
        // Reset all dependent components
        SMTanod_id.setText("ID: ");
        SMFirst_name.setText("First Name: ");
        SMLast_name.setText("Last Name: ");
        SMMiddle_name.setText("Middle Name: ");
        SMCreated_at.setText("Created At: ");
        
        UpdateTanod.setEnabled(false);
        DayOfWeekFilter.setEnabled(false);
        AddShift.setEnabled(false);
        
        appState.remove("editingTanodId");
        
        if(TanodSearchField.getText() != null){
            try{
                fillTanodsTable(Long.parseLong(TanodSearchField.getText()));
            }
            catch(NumberFormatException e){
                fillTanodsTable(TanodSearchField.getText());
            }
        }
        else{
            fillTanodsTable();
        }
        
        jLabel39.setText(""); jLabel40.setText(""); jLabel41.setText("");
        RestorationDialog.setVisible(false);
    }//GEN-LAST:event_DeleteTanodActionPerformed

    private void tanod_last_nameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tanod_last_nameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tanod_last_nameActionPerformed

    private void tanod_first_nameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tanod_first_nameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tanod_first_nameActionPerformed

    private void RestoreTanodBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RestoreTanodBtnActionPerformed
        // TODO add your handling code here:
        try(Session session = sessionFactory.openSession()){
            session.beginTransaction();
            Tanod tanod = session.find(Tanod.class, appState.get("editingTanodId"));
            tanod.unarchive();
            session.merge(tanod);
            session.getTransaction().commit();
            session.close();
        }
        
        // Reset all dependent components
        SMTanod_id.setText("ID: ");
        SMFirst_name.setText("First Name: ");
        SMLast_name.setText("Last Name: ");
        SMMiddle_name.setText("Middle Name: ");
        SMCreated_at.setText("Created At: ");
        
        UpdateTanod.setEnabled(false);
        DayOfWeekFilter.setEnabled(false);
        AddShift.setEnabled(false);
        
        appState.remove("editingTanodId");
        
        if(TanodSearchField.getText() != null){
            try{
                fillTanodsTable(Long.parseLong(TanodSearchField.getText()));
            }
            catch(NumberFormatException e){
                fillTanodsTable(TanodSearchField.getText());
            }
        }
        else{
            fillTanodsTable();
        }
        
        jLabel39.setText(""); jLabel40.setText(""); jLabel41.setText("");
        RestorationDialog.setVisible(false);
    }//GEN-LAST:event_RestoreTanodBtnActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TanodManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TanodManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TanodManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TanodManagement.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TanodManagement().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AddShift;
    private javax.swing.JButton ArchiveTanod;
    private javax.swing.JTable ArchivesTable;
    private javax.swing.JPanel CardLayout;
    private javax.swing.JButton ClockIn;
    private javax.swing.JPanel ClockInClockOut;
    private javax.swing.JButton ClockInClockOutBtn;
    private javax.swing.JButton ClockOut;
    private javax.swing.JPanel Dashboard;
    private javax.swing.JButton DashboardBtn;
    private javax.swing.JComboBox<String> DayOfWeekFilter;
    private javax.swing.JLabel DeductionLabel;
    private javax.swing.JButton DeleteShift;
    private javax.swing.JButton DeleteTanod;
    private javax.swing.JLabel GrossLabel;
    private javax.swing.JLabel IssuedOnLabel;
    private javax.swing.JButton LockButton;
    private javax.swing.JPanel Main;
    private javax.swing.JButton ManualAdjustSave;
    private javax.swing.JLabel NetLabel;
    private javax.swing.JPanel Payroll;
    private javax.swing.JButton PayrollBtn;
    private javax.swing.JLabel PayrollPeriodLabel;
    private javax.swing.JTable PayrollTable;
    private javax.swing.JFrame RestorationDialog;
    private javax.swing.JButton RestoreTanodBtn;
    private javax.swing.JPanel RestoreTanodDialog;
    private javax.swing.JLabel SMCreated_at;
    private javax.swing.JLabel SMFirst_name;
    private javax.swing.JLabel SMLast_name;
    private javax.swing.JLabel SMMiddle_name;
    private javax.swing.JLabel SMTanod_id;
    private javax.swing.JButton SaveShift;
    private javax.swing.JButton SaveTanod;
    private javax.swing.JTextField SearchTanodField;
    private javax.swing.JFrame ShiftForm;
    private javax.swing.JPanel ShiftFormPanel;
    private javax.swing.JComboBox<String> Shift_day;
    private javax.swing.JLabel Shift_ends_at;
    private javax.swing.JComboBox<String> Shift_ends_at_hr;
    private javax.swing.JComboBox<String> Shift_ends_at_md;
    private javax.swing.JComboBox<String> Shift_ends_at_mn;
    private javax.swing.JLabel Shift_for;
    private javax.swing.JLabel Shift_starts_at;
    private javax.swing.JComboBox<String> Shift_starts_at_hr;
    private javax.swing.JComboBox<String> Shift_starts_at_md;
    private javax.swing.JComboBox<String> Shift_starts_at_mn;
    private javax.swing.JButton ShiftsBtn;
    private javax.swing.JPanel ShiftsManagement;
    private javax.swing.JPanel ShiftsManagementContent;
    private javax.swing.JPanel ShiftsManagementContent2;
    private javax.swing.JPanel ShiftsManagementHead;
    private javax.swing.JTable ShiftsTable;
    private javax.swing.JTable ShiftsTable2;
    private javax.swing.JPanel SidePanel;
    private javax.swing.JPanel System;
    private javax.swing.JButton SystemBtn;
    private javax.swing.JButton SystemBtn1;
    private javax.swing.JFrame TanodForm;
    private javax.swing.JPanel TanodFormPanel;
    private javax.swing.JLabel TanodFullName;
    private javax.swing.JLabel TanodIdLabel;
    private javax.swing.JPanel TanodManagement;
    private javax.swing.JPanel TanodManagementHead;
    private javax.swing.JTextField TanodSearchField;
    private javax.swing.JTextField TanodSearchField2;
    private javax.swing.JButton TanodsBtn;
    private javax.swing.JTable TanodsTable;
    private javax.swing.JPanel UpdateAdminCredentials;
    private javax.swing.JButton UpdateTanod;
    private eltagonde.utils.BasicBackgroundPanel basicBackgroundPanel1;
    private eltagonde.utils.BasicBackgroundPanel basicBackgroundPanel2;
    private eltagonde.utils.BasicBackgroundPanel basicBackgroundPanel3;
    private com.github.lgooddatepicker.components.CalendarPanel calendarPanel1;
    private javax.swing.JComboBox<String> clock_in_hr;
    private javax.swing.JComboBox<String> clock_in_md;
    private javax.swing.JComboBox<String> clock_in_mn;
    private javax.swing.JComboBox<String> clock_out_hr;
    private javax.swing.JComboBox<String> clock_out_md;
    private javax.swing.JComboBox<String> clock_out_mn;
    private eltagonde.utils.GradientBackgroundPanel gradientBackgroundPanel1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JPasswordField password;
    private javax.swing.JPasswordField passwordText;
    private com.github.lgooddatepicker.components.DatePicker payrollEnd;
    private com.github.lgooddatepicker.components.DatePicker payrollStart;
    private javax.swing.JTextField tanod_first_name;
    private javax.swing.JLabel tanod_first_name_label;
    private javax.swing.JLabel tanod_first_name_label1;
    private javax.swing.JTextField tanod_last_name;
    private javax.swing.JLabel tanod_last_name_label;
    private javax.swing.JLabel tanod_last_name_label1;
    private javax.swing.JTextField tanod_middle_name;
    private javax.swing.JLabel tanod_middle_name_label;
    private javax.swing.JLabel tanod_middle_name_label1;
    private javax.swing.JTextField userText;
    private javax.swing.JTextField username;
    // End of variables declaration//GEN-END:variables
}
