package com.fixflow.repository;

import com.fixflow.entity.ComplaintHistory;
import com.fixflow.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {

    @Query("""
            SELECT h FROM ComplaintHistory h
            LEFT JOIN FETCH h.changedBy
            WHERE h.complaint = :complaint
            ORDER BY h.changedAt DESC
            """)
    List<ComplaintHistory> findByComplaintOrderByChangedAtDesc(
            @Param("complaint") Complaint complaint);

    List<ComplaintHistory> findByComplaintIdOrderByChangedAtDesc(Long complaintId);
}
