package com.fixflow.repository;

import com.fixflow.entity.Complaint;
import com.fixflow.entity.ComplaintStatus;
import com.fixflow.entity.Priority;
import com.fixflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    @Query("""
            SELECT c FROM Complaint c
            LEFT JOIN FETCH c.department
            LEFT JOIN FETCH c.assignedTo
            LEFT JOIN FETCH c.reportedBy
            WHERE c.complaintId = :complaintId
            """)
    Optional<Complaint> findByComplaintId(
            @Param("complaintId") String complaintId);

    List<Complaint> findByReportedByOrderByCreatedAtDesc(User user);

    List<Complaint> findByAssignedToOrderByCreatedAtDesc(User user);

    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    List<Complaint> findByPriorityOrderByCreatedAtDesc(Priority priority);

    List<Complaint> findByCategoryOrderByCreatedAtDesc(String category);

    @Query("SELECT c FROM Complaint c LEFT JOIN FETCH c.reportedBy LEFT JOIN FETCH c.assignedTo LEFT JOIN FETCH c.department ORDER BY c.createdAt DESC")
    List<Complaint> findAllByOrderByCreatedAtDesc();

    long countByStatus(ComplaintStatus status);

    long countByPriority(Priority priority);

    long countByOverdueTrue();

    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED','REJECTED','CANCELLED') AND c.overdue = false")
    List<Complaint> findUnresolvedNonOverdue();

    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED','REJECTED','CANCELLED') AND " +
            "(:priority = 'CRITICAL' AND c.createdAt < :criticalCutoff) OR " +
            "(:priority = 'HIGH' AND c.createdAt < :highCutoff) OR " +
            "(:priority = 'MEDIUM' AND c.createdAt < :mediumCutoff) OR " +
            "(:priority = 'LOW' AND c.createdAt < :lowCutoff)")
    List<Complaint> findPotentialOverdue(
            @Param("criticalCutoff") LocalDateTime criticalCutoff,
            @Param("highCutoff") LocalDateTime highCutoff,
            @Param("mediumCutoff") LocalDateTime mediumCutoff,
            @Param("lowCutoff") LocalDateTime lowCutoff);

    @Query("SELECT c FROM Complaint c LEFT JOIN FETCH c.reportedBy LEFT JOIN FETCH c.assignedTo LEFT JOIN FETCH c.department WHERE c.overdue = true ORDER BY c.createdAt ASC")
    List<Complaint> findOverdueComplaints();

    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED','REJECTED','CANCELLED') AND " +
            "((c.priority = 'CRITICAL' AND c.createdAt < :criticalCutoff) OR " +
            "(c.priority = 'HIGH' AND c.createdAt < :highCutoff) OR " +
            "(c.priority = 'MEDIUM' AND c.createdAt < :mediumCutoff) OR " +
            "(c.priority = 'LOW' AND c.createdAt < :lowCutoff))")
    List<Complaint> findAllPotentialOverdue(
            @Param("criticalCutoff") LocalDateTime criticalCutoff,
            @Param("highCutoff") LocalDateTime highCutoff,
            @Param("mediumCutoff") LocalDateTime mediumCutoff,
            @Param("lowCutoff") LocalDateTime lowCutoff);

    // Duplicate detection: same category and location, unresolved
    @Query("SELECT c FROM Complaint c WHERE c.category = :category AND c.location = :location " +
            "AND c.status NOT IN ('RESOLVED','REJECTED','CANCELLED') ORDER BY c.createdAt DESC")
    List<Complaint> findPotentialDuplicates(
            @Param("category") String category,
            @Param("location") String location);

    @Query("SELECT DISTINCT c.category FROM Complaint c ORDER BY c.category")
    List<String> findDistinctCategories();

    @Query("SELECT c.category, COUNT(c) FROM Complaint c GROUP BY c.category ORDER BY COUNT(c) DESC")
    List<Object[]> countByCategory();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, c.createdAt, c.resolvedAt)) FROM Complaint c WHERE c.resolvedAt IS NOT NULL")
    Double avgResolutionTimeHours();

    @Query("SELECT c FROM Complaint c LEFT JOIN FETCH c.reportedBy LEFT JOIN FETCH c.assignedTo LEFT JOIN FETCH c.department ORDER BY c.createdAt DESC")
    List<Complaint> findRecentComplaints(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT MAX(CAST(SUBSTRING(c.complaintId, 4) AS int)) FROM Complaint c WHERE c.complaintId LIKE :prefix%")
    Integer findMaxSequenceForPrefix(@Param("prefix") String prefix);
}
