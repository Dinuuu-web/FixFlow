package com.fixflow.scheduler;

import com.fixflow.entity.*;
import com.fixflow.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueDetectionScheduler {

    private final ComplaintRepository complaintRepository;

    @Value("${app.overdue.critical.hours:4}")
    private int criticalHours;

    @Value("${app.overdue.high.hours:24}")
    private int highHours;

    @Value("${app.overdue.medium.hours:72}")
    private int mediumHours;

    @Value("${app.overdue.low.hours:168}")
    private int lowHours;

    /**
     * Runs every 30 minutes to mark overdue complaints.
     */
    @Scheduled(fixedDelayString = "1800000", initialDelay = 60000)
    @Transactional
    public void detectOverdueComplaints() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime criticalCutoff = now.minusHours(criticalHours);
        LocalDateTime highCutoff    = now.minusHours(highHours);
        LocalDateTime mediumCutoff  = now.minusHours(mediumHours);
        LocalDateTime lowCutoff     = now.minusHours(lowHours);

        List<Complaint> overdue = complaintRepository.findAllPotentialOverdue(
            criticalCutoff, highCutoff, mediumCutoff, lowCutoff
        );

        int count = 0;
        for (Complaint c : overdue) {
            if (!c.isOverdue()) {
                c.setOverdue(true);
                complaintRepository.save(c);
                count++;
            }
        }

        if (count > 0) {
            log.info("Overdue detection: marked {} complaint(s) as overdue", count);
        }
    }
}
