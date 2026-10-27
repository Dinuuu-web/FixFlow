package com.fixflow.controller;

import com.fixflow.entity.*;
import com.fixflow.repository.DepartmentRepository;
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

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ComplaintService complaintService;
    private final DepartmentRepository departmentRepository;

    private User getCurrentUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", complaintService.getDashboardStats());
        model.addAttribute("overdueComplaints", complaintService.getOverdueComplaints());
        model.addAttribute("recentComplaints", complaintService.getRecentComplaints(10));
        return "admin/dashboard";
    }

    @GetMapping("/complaints")
    public String allComplaints(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String priority,
                                @RequestParam(required = false) String category,
                                Model model) {
        List<Complaint> complaints;

        if (status != null && !status.isBlank()) {
            try {
                complaints = complaintService.getAllComplaintsDesc().stream()
                    .filter(c -> c.getStatus().name().equalsIgnoreCase(status))
                    .toList();
            } catch (Exception e) {
                complaints = complaintService.getAllComplaintsDesc();
            }
        } else if (priority != null && !priority.isBlank()) {
            try {
                complaints = complaintService.getAllComplaintsDesc().stream()
                    .filter(c -> c.getPriority().name().equalsIgnoreCase(priority))
                    .toList();
            } catch (Exception e) {
                complaints = complaintService.getAllComplaintsDesc();
            }
        } else if (category != null && !category.isBlank()) {
            complaints = complaintService.getAllComplaintsDesc().stream()
                .filter(c -> c.getCategory().equalsIgnoreCase(category))
                .toList();
        } else {
            complaints = complaintService.getAllComplaintsDesc();
        }

        model.addAttribute("complaints", complaints);
        model.addAttribute("categories", complaintService.getDistinctCategories());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("selectedCategory", category);
        return "admin/complaints";
    }

    @GetMapping("/complaint/{complaintId}")
    public String viewComplaint(@PathVariable String complaintId, Model model) {
        Complaint complaint = complaintService.getByComplaintId(complaintId);
        model.addAttribute("complaint", complaint);
        model.addAttribute("history", complaintService.getHistory(complaint));
        model.addAttribute("staffList", userService.findAllMaintenanceStaff());
        return "admin/complaint-detail";
    }

    @PostMapping("/complaint/{complaintId}/assign")
    public String assignComplaint(@PathVariable String complaintId,
                                  @RequestParam Long staffId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser(userDetails);
        try {
            complaintService.assignComplaint(complaintId, staffId, admin);
            redirectAttributes.addFlashAttribute("successMsg", "Complaint assigned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Assignment failed: " + e.getMessage());
        }
        return "redirect:/admin/complaint/" + complaintId;
    }

    @PostMapping("/complaint/{complaintId}/status")
    public String updateStatus(@PathVariable String complaintId,
                               @RequestParam String status,
                               @RequestParam(required = false) String remarks,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser(userDetails);
        try {
            ComplaintStatus newStatus = ComplaintStatus.valueOf(status.toUpperCase());
            complaintService.updateStatus(complaintId, newStatus, remarks, admin);
            redirectAttributes.addFlashAttribute("successMsg", "Status updated to " + newStatus);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Update failed: " + e.getMessage());
        }
        return "redirect:/admin/complaint/" + complaintId;
    }

    @GetMapping("/overdue")
    public String overdueComplaints(Model model) {
        model.addAttribute("complaints", complaintService.getOverdueComplaints());
        return "admin/overdue";
    }

    @GetMapping("/departments")
    public String departments(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        return "admin/departments";
    }

    @PostMapping("/departments")
    public String addDepartment(@RequestParam String name,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            if (departmentRepository.existsByName(name)) {
                redirectAttributes.addFlashAttribute("errorMsg", "Department already exists.");
                return "redirect:/admin/departments";
            }
            Department dept = new Department();
            dept.setName(name.trim());
            dept.setDescription(description != null ? description.trim() : "");
            dept.setActive(true);
            departmentRepository.save(dept);
            redirectAttributes.addFlashAttribute("successMsg", "Department added.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/departments";
    }
}
