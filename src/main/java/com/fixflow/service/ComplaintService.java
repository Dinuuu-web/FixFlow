package com.fixflow.service;

import com.fixflow.dto.ComplaintDto;
import com.fixflow.dto.DashboardStatsDto;
import com.fixflow.entity.*;
import com.fixflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.complaint.id.prefix:FF}")
    private String complaintIdPrefix;

    // ============================================
    // COMPLAINT ID GENERATION (thread-safe)
    // ============================================
    private static final Object ID_LOCK = new Object();

    private synchronized String generateComplaintId() {
        String prefix = complaintIdPrefix + "-";
        Integer max = complaintRepository.findMaxSequenceForPrefix(complaintIdPrefix + "-");
        int next = (max == null) ? 1001 : max + 1;
        return prefix + next;
    }

    // ============================================
    // SUBMIT COMPLAINT
    // ============================================
    @Transactional
    public Complaint submitComplaint(ComplaintDto dto, User reportedBy) {
        Complaint complaint = new Complaint();
        complaint.setComplaintId(generateComplaintId());
        complaint.setTitle(dto.getTitle().trim());
        complaint.setDescription(dto.getDescription().trim());
        complaint.setCategory(dto.getCategory());
        complaint.setLocation(dto.getLocation().trim());
        complaint.setPriority(Priority.valueOf(dto.getPriority()));
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setReportedBy(reportedBy);

        if (dto.getDepartmentId() != null) {
            departmentRepository.findById(dto.getDepartmentId())
                .ifPresent(complaint::setDepartment);
        }

        // Handle image upload
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String imagePath = saveImage(dto.getImage(), complaint.getComplaintId());
            complaint.setImagePath(imagePath);
        }

        Complaint saved = complaintRepository.save(complaint);

        // Record initial history
        recordHistory(saved, null, ComplaintStatus.PENDING, "Complaint submitted", reportedBy);

        log.info("Complaint {} submitted by {}", saved.getComplaintId(), reportedBy.getEmail());
        return saved;
    }

    // ============================================
    // DUPLICATE DETECTION
    // ============================================
    public List<Complaint> findPotentialDuplicates(String category, String location, String description) {
        List<Complaint> candidates = complaintRepository.findPotentialDuplicates(category, location);
        if (candidates.isEmpty()) return Collections.emptyList();

        // Normalize keywords from description
        Set<String> keywords = extractKeywords(description);
        List<Complaint> duplicates = new ArrayList<>();

        for (Complaint c : candidates) {
            Set<String> existingKeywords = extractKeywords(c.getTitle() + " " + c.getDescription());
            long matchCount = keywords.stream().filter(existingKeywords::contains).count();
            double similarity = keywords.isEmpty() ? 0
                : (double) matchCount / Math.max(keywords.size(), existingKeywords.size());
            if (similarity >= 0.3) {
                duplicates.add(c);
            }
        }
        return duplicates;
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String[] stopWords = {"the", "a", "an", "is", "it", "in", "on", "at", "to", "of", "and",
                              "or", "for", "not", "with", "has", "are", "was", "be", "this", "that"};
        Set<String> stops = new HashSet<>(Arrays.asList(stopWords));
        Set<String> words = new HashSet<>();
        for (String word : text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (word.length() > 2 && !stops.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }

    // ============================================
    // ASSIGN COMPLAINT
    // ============================================
    @Transactional
    public Complaint assignComplaint(String complaintId, Long staffId, User admin) {
        Complaint complaint = getByComplaintId(complaintId);
        User staff = userRepository.findById(staffId)
            .orElseThrow(() -> new IllegalArgumentException("Staff member not found"));

        ComplaintStatus prevStatus = complaint.getStatus();
        complaint.setAssignedTo(staff);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        Complaint saved = complaintRepository.save(complaint);

        recordHistory(saved, prevStatus, ComplaintStatus.ASSIGNED,
            "Assigned to " + staff.getFullName(), admin);
        log.info("Complaint {} assigned to {}", complaintId, staff.getEmail());
        return saved;
    }

    // ============================================
    // UPDATE STATUS
    // ============================================
    @Transactional
    public Complaint updateStatus(String complaintId, ComplaintStatus newStatus,
                                  String remarks, User changedBy) {
        Complaint complaint = getByComplaintId(complaintId);
        ComplaintStatus prevStatus = complaint.getStatus();
        complaint.setStatus(newStatus);
        if (remarks != null && !remarks.isBlank()) {
            complaint.setAdminRemarks(remarks.trim());
        }
        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
            complaint.setOverdue(false);
        }
        if (newStatus == ComplaintStatus.REJECTED || newStatus == ComplaintStatus.CANCELLED) {
            complaint.setOverdue(false);
        }
        Complaint saved = complaintRepository.save(complaint);
        recordHistory(saved, prevStatus, newStatus, remarks, changedBy);
        log.info("Complaint {} status changed: {} -> {} by {}", complaintId, prevStatus, newStatus, changedBy.getEmail());
        return saved;
    }

    // ============================================
    // CANCEL COMPLAINT (student)
    // ============================================
    @Transactional
    public Complaint cancelComplaint(String complaintId, User student) {
        Complaint complaint = getByComplaintId(complaintId);
        if (!complaint.getReportedBy().getId().equals(student.getId())) {
            throw new SecurityException("Not authorized to cancel this complaint");
        }
        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new IllegalStateException("Only PENDING complaints can be cancelled");
        }
        return updateStatus(complaintId, ComplaintStatus.CANCELLED, "Cancelled by student", student);
    }

    // ============================================
    // QUERIES
    // ============================================
    public Complaint getByComplaintId(String complaintId) {
        return complaintRepository.findByComplaintId(complaintId)
            .orElseThrow(() -> new NoSuchElementException("Complaint not found: " + complaintId));
    }

    public List<Complaint> getComplaintsForStudent(User user) {
        return complaintRepository.findByReportedByOrderByCreatedAtDesc(user);
    }

    public List<Complaint> getComplaintsForStaff(User staff) {
        return complaintRepository.findByAssignedToOrderByCreatedAtDesc(staff);
    }

    public List<Complaint> getAllComplaintsDesc() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Complaint> getRecentComplaints(int count) {
        return complaintRepository.findRecentComplaints(PageRequest.of(0, count));
    }

    public List<Complaint> getOverdueComplaints() {
        return complaintRepository.findOverdueComplaints();
    }

    public List<String> getDistinctCategories() {
        return complaintRepository.findDistinctCategories();
    }

    // ============================================
    // DASHBOARD STATS
    // ============================================
    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setTotalComplaints(complaintRepository.count());
        stats.setPendingComplaints(complaintRepository.countByStatus(ComplaintStatus.PENDING));
        stats.setInProgressComplaints(
            complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS)
            + complaintRepository.countByStatus(ComplaintStatus.ASSIGNED)
        );
        stats.setResolvedComplaints(complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        stats.setRejectedComplaints(complaintRepository.countByStatus(ComplaintStatus.REJECTED));
        stats.setOverdueComplaints(complaintRepository.countByOverdueTrue());
        stats.setAvgResolutionHours(complaintRepository.avgResolutionTimeHours());
        stats.setCategoryStats(complaintRepository.countByCategory());
        stats.setStatusStats(complaintRepository.countByStatusGrouped());
        return stats;
    }

    // ============================================
    // HISTORY
    // ============================================
    public List<ComplaintHistory> getHistory(Complaint complaint) {
        return historyRepository.findByComplaintOrderByChangedAtDesc(complaint);
    }

    private void recordHistory(Complaint complaint, ComplaintStatus from, ComplaintStatus to,
                               String remarks, User changedBy) {
        ComplaintHistory history = new ComplaintHistory();
        history.setComplaint(complaint);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setRemarks(remarks);
        history.setChangedBy(changedBy);
        historyRepository.save(history);
    }

    // ============================================
    // IMAGE UPLOAD
    // ============================================
    private String saveImage(MultipartFile file, String complaintId) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
                : ".jpg";

            // Allow only image types
            if (!List.of(".jpg", ".jpeg", ".png", ".gif", ".webp").contains(ext)) {
                throw new IllegalArgumentException("Unsupported image format: " + ext);
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("File size exceeds 5MB limit");
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(uploadPath);

            String fileName = complaintId + "_" + System.currentTimeMillis() + ext;
            Path targetPath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + fileName;
        } catch (IOException e) {
            log.error("Image upload failed for complaint {}: {}", complaintId, e.getMessage());
            throw new RuntimeException("Failed to save image: " + e.getMessage());
        }
    }
}
