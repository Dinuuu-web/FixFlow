package com.fixflow.controller;

import com.fixflow.entity.*;
import com.fixflow.service.ComplaintService;
import com.fixflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
@PreAuthorize("hasAnyRole('ADMIN','MAINTENANCE_STAFF')")
@RequiredArgsConstructor
public class StaffController {

    private final UserService userService;
    private final ComplaintService complaintService;

    private User getCurrentUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getCurrentUser(userDetails);
        var complaints = complaintService.getComplaintsForStaff(user);
        long pending   = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.ASSIGNED).count();
        long inProg    = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS).count();
        long resolved  = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();

        model.addAttribute("user", user);
        model.addAttribute("complaints", complaints);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("inProgressCount", inProg);
        model.addAttribute("resolvedCount", resolved);
        return "staff/dashboard";
    }

    @GetMapping("/complaint/{complaintId}")
    public String viewComplaint(@PathVariable String complaintId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = getCurrentUser(userDetails);
        Complaint complaint = complaintService.getByComplaintId(complaintId);

        // Only see complaints assigned to them (or admin can see all)
        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && (complaint.getAssignedTo() == null
                || !complaint.getAssignedTo().getId().equals(user.getId()))) {
            return "redirect:/error/403";
        }

        model.addAttribute("complaint", complaint);
        model.addAttribute("history", complaintService.getHistory(complaint));
        model.addAttribute("user", user);
        return "staff/complaint-detail";
    }

    @PostMapping("/complaint/{complaintId}/update")
    public String updateStatus(@PathVariable String complaintId,
                               @RequestParam String status,
                               @RequestParam(required = false) String remarks,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(userDetails);
        try {
            // Staff can only set IN_PROGRESS or RESOLVED
            ComplaintStatus newStatus = ComplaintStatus.valueOf(status.toUpperCase());
            if (newStatus != ComplaintStatus.IN_PROGRESS && newStatus != ComplaintStatus.RESOLVED) {
                throw new IllegalArgumentException("Staff can only set IN_PROGRESS or RESOLVED");
            }
            complaintService.updateStatus(complaintId, newStatus, remarks, user);
            redirectAttributes.addFlashAttribute("successMsg", "Status updated to " + newStatus);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/staff/complaint/" + complaintId;
    }
}
