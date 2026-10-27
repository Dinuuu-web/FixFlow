package com.fixflow.config;

import com.fixflow.entity.*;
import com.fixflow.repository.*;
import com.fixflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SampleDataInitializer {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.sample-data:true}")
    private boolean initSampleData;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (!initSampleData) return;
            if (userRepository.count() > 0) {
                log.info("Sample data already initialized, skipping.");
                return;
            }

            log.info("Initializing sample data...");

            // Departments
            Department cse = createDept("Computer Science & Engineering", "CSE department");
            Department ece = createDept("Electronics & Communication", "ECE department");
            Department mech = createDept("Mechanical Engineering", "Mechanical department");
            Department civil = createDept("Civil Engineering", "Civil department");
            Department admin = createDept("Administration", "Admin office");
            Department facilities = createDept("Campus Facilities", "Facilities management");

            // Admin user
            User adminUser = new User();
            adminUser.setFullName("Admin User");
            adminUser.setEmail("admin@fixflow.com");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setRole(Role.ADMIN);
            adminUser.setDepartment(admin);
            adminUser.setEnabled(true);
            userRepository.save(adminUser);

            // Maintenance staff
            User staff1 = new User();
            staff1.setFullName("Ravi Kumar");
            staff1.setEmail("ravi@fixflow.com");
            staff1.setPassword(passwordEncoder.encode("staff123"));
            staff1.setRole(Role.MAINTENANCE_STAFF);
            staff1.setDepartment(facilities);
            staff1.setEnabled(true);
            userRepository.save(staff1);

            User staff2 = new User();
            staff2.setFullName("Priya Sharma");
            staff2.setEmail("priya@fixflow.com");
            staff2.setPassword(passwordEncoder.encode("staff123"));
            staff2.setRole(Role.MAINTENANCE_STAFF);
            staff2.setDepartment(facilities);
            staff2.setEnabled(true);
            userRepository.save(staff2);

            // Students
            User student1 = new User();
            student1.setFullName("Arjun Mehta");
            student1.setEmail("arjun@student.fixflow.com");
            student1.setPassword(passwordEncoder.encode("student123"));
            student1.setRole(Role.STUDENT);
            student1.setRollNumber("CSE2021001");
            student1.setPhone("9876543210");
            student1.setDepartment(cse);
            student1.setEnabled(true);
            userRepository.save(student1);

            User student2 = new User();
            student2.setFullName("Sneha Patel");
            student2.setEmail("sneha@student.fixflow.com");
            student2.setPassword(passwordEncoder.encode("student123"));
            student2.setRole(Role.STUDENT);
            student2.setRollNumber("ECE2021045");
            student2.setPhone("9123456789");
            student2.setDepartment(ece);
            student2.setEnabled(true);
            userRepository.save(student2);

            // Sample complaints
            createComplaint("FF-1001", "Projector not working in CSE Lab 3",
                "The projector in CSE Lab 3 is completely non-functional since Monday. The display shows no input signal.",
                "Projector & AV Equipment", "CSE Block, Lab 3", cse, Priority.HIGH,
                ComplaintStatus.IN_PROGRESS, student1, staff1, adminUser,
                LocalDateTime.now().minusDays(2));

            createComplaint("FF-1002", "Wi-Fi not connecting in Library",
                "The Wi-Fi in the central library ground floor has been intermittently dropping for the past week.",
                "Wi-Fi & Networking", "Central Library, Ground Floor", admin, Priority.HIGH,
                ComplaintStatus.ASSIGNED, student2, staff2, adminUser,
                LocalDateTime.now().minusDays(1));

            createComplaint("FF-1003", "Broken bench in Room 204",
                "Three benches in Classroom 204 are broken. Students are sitting on the floor.",
                "Furniture & Fixtures", "Block B, Room 204", civil, Priority.MEDIUM,
                ComplaintStatus.PENDING, student1, null, null,
                LocalDateTime.now().minusHours(5));

            createComplaint("FF-1004", "Water leakage near electrical panel",
                "There is water leaking from the ceiling near the electrical panel in the corridor of Block A, 2nd floor.",
                "Plumbing & Water", "Block A, 2nd Floor Corridor", facilities, Priority.CRITICAL,
                ComplaintStatus.PENDING, student2, null, null,
                LocalDateTime.now().minusHours(2));

            createComplaint("FF-1005", "Computer lab PCs not booting",
                "10 out of 30 computers in ECE Computer Lab are not starting. Hard drive failure suspected.",
                "IT & Computer Equipment", "ECE Block, Computer Lab", ece, Priority.HIGH,
                ComplaintStatus.RESOLVED, student1, staff1, adminUser,
                LocalDateTime.now().minusDays(5));

            log.info("Sample data initialized successfully.");
            log.info("Demo credentials:");
            log.info("  Admin:    admin@fixflow.com / admin123");
            log.info("  Staff:    ravi@fixflow.com / staff123");
            log.info("  Student:  arjun@student.fixflow.com / student123");
        };
    }

    private Department createDept(String name, String desc) {
        Department d = new Department();
        d.setName(name);
        d.setDescription(desc);
        d.setActive(true);
        return departmentRepository.save(d);
    }

    private void createComplaint(String id, String title, String desc, String category,
                                  String location, Department dept, Priority priority,
                                  ComplaintStatus status, User reporter, User assignee,
                                  User admin, LocalDateTime createdAt) {
        Complaint c = new Complaint();
        c.setComplaintId(id);
        c.setTitle(title);
        c.setDescription(desc);
        c.setCategory(category);
        c.setLocation(location);
        c.setDepartment(dept);
        c.setPriority(priority);
        c.setStatus(status);
        c.setReportedBy(reporter);
        c.setAssignedTo(assignee);
        c.setCreatedAt(createdAt);
        c.setUpdatedAt(createdAt);

        if (status == ComplaintStatus.RESOLVED) {
            c.setResolvedAt(createdAt.plusDays(1));
            c.setAdminRemarks("Issue has been resolved. Equipment replaced.");
        }

        complaintRepository.save(c);

        // History
        ComplaintHistory h1 = new ComplaintHistory();
        h1.setComplaint(c);
        h1.setFromStatus(null);
        h1.setToStatus(ComplaintStatus.PENDING);
        h1.setRemarks("Complaint submitted");
        h1.setChangedBy(reporter);
        h1.setChangedAt(createdAt);
        historyRepository.save(h1);

        if (assignee != null && admin != null) {
            ComplaintHistory h2 = new ComplaintHistory();
            h2.setComplaint(c);
            h2.setFromStatus(ComplaintStatus.PENDING);
            h2.setToStatus(ComplaintStatus.ASSIGNED);
            h2.setRemarks("Assigned to " + assignee.getFullName());
            h2.setChangedBy(admin);
            h2.setChangedAt(createdAt.plusHours(1));
            historyRepository.save(h2);
        }

        if (status == ComplaintStatus.IN_PROGRESS && assignee != null) {
            ComplaintHistory h3 = new ComplaintHistory();
            h3.setComplaint(c);
            h3.setFromStatus(ComplaintStatus.ASSIGNED);
            h3.setToStatus(ComplaintStatus.IN_PROGRESS);
            h3.setRemarks("Work started on the issue");
            h3.setChangedBy(assignee);
            h3.setChangedAt(createdAt.plusHours(2));
            historyRepository.save(h3);
        }

        if (status == ComplaintStatus.RESOLVED && assignee != null) {
            ComplaintHistory h4 = new ComplaintHistory();
            h4.setComplaint(c);
            h4.setFromStatus(ComplaintStatus.IN_PROGRESS);
            h4.setToStatus(ComplaintStatus.RESOLVED);
            h4.setRemarks("Issue fully resolved and tested");
            h4.setChangedBy(assignee);
            h4.setChangedAt(createdAt.plusDays(1));
            historyRepository.save(h4);
        }
    }
}
