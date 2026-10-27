package com.fixflow.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsDto {
    private long totalComplaints;
    private long pendingComplaints;
    private long inProgressComplaints;
    private long resolvedComplaints;
    private long overdueComplaints;
    private long rejectedComplaints;
    private Double avgResolutionHours;
    private List<Object[]> categoryStats;
    private List<Object[]> statusStats;
}
