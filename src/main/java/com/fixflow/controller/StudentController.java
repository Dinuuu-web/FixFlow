package com.fixflow.controller;

import com.fixflow.dto.ComplaintDto;
import com.fixflow.entity.*;
import com.fixflow.repository.DepartmentRepository;
import com.fixflow.service.ComplaintService;
import com.fixflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentController {

    private final UserService userService;
    private final ComplaintService complaintService;
    private final DepartmentRepository departmentRepository;

    private User getCurrentUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getCurrentUser(userDetails);
        List<Complaint> complaints = complaintService.getComplaintsForStudent(user);
        long pending   = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.PENDING).count();
        long inProg    = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS
                                                      || c.getStatus() == ComplaintStatus.ASSIGNED).count();
        long resolved  = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();

        model.addAttribute("user", user);
        model.addAttribute("recentComplaints", complaints.stream().limit(5).toList());
        model.addAttribute("totalComplaints", complaints.size());
        model.addAttribute("pendingComplaints", pending);
        model.addAttribute("inProgressComplaints", inProg);
        model.addAttribute("resolvedComplaints", resolved);
        return "student/dashboard";
    }

    @GetMapping("/complaints")
    public String myComplaints(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getCurrentUser(userDetails);
        model.addAttribute("user", user);
        model.addAttribute("complaints", complaintService.getComplaintsForStudent(user));
        return "student/my-complaints";
    }

    @GetMapping("/complaint/new")
    public String newComplaintForm(Model model) {
        model.addAttribute("complaintDto", new ComplaintDto());
        model.addAttribute("departments", departmentRepository.findByActiveTrue());
        model.addAttribute("categories", getCategories());
        return "student/submit-complaint";
    }

    @PostMapping("/complaint/new")
    public String submitComplaint(@Valid @ModelAttribute("complaintDto") ComplaintDto dto,
                                  BindingResult result,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findByActiveTrue());
            model.addAttribute("categories", getCategories());
            return "student/submit-complaint";
        }

        User user = getCurrentUser(userDetails);

        // Duplicate detection
        List<Complaint> duplicates = complaintService.findPotentialDuplicates(
            dto.getCategory(), dto.getLocation(), dto.getDescription()
        );
        if (!duplicates.isEmpty() && !"confirmed".equals(model.getAttribute("confirm"))) {
            model.addAttribute("duplicates", duplicates);
            model.addAttribute("complaintDto", dto);
            model.addAttribute("departments", departmentRepository.findByActiveTrue());
            model.addAttribute("categories", getCategories());
            model.addAttribute("showDuplicateWarning", true);
            return "student/submit-complaint";
        }

        try {
            Complaint saved = complaintService.submitComplaint(dto, user);
            redirectAttributes.addFlashAttribute("successMsg",
                "Complaint " + saved.getComplaintId() + " submitted successfully!");
            return "redirect:/student/complaints";
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Submission failed: " + e.getMessage());
            model.addAttribute("departments", departmentRepository.findByActiveTrue());
            model.addAttribute("categories", getCategories());
            return "student/submit-complaint";
        }
    }

    @GetMapping("/complaint/{complaintId}")
    public String viewComplaint(@PathVariable String complaintId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = getCurrentUser(userDetails);
        Complaint complaint = complaintService.getByComplaintId(complaintId);

        // Students can only view their own complaints
        if (!complaint.getReportedBy().getId().equals(user.getId())) {
            return "redirect:/error/403";
        }

        model.addAttribute("complaint", complaint);
        model.addAttribute("history", complaintService.getHistory(complaint));
        model.addAttribute("user", user);
        return "student/complaint-detail";
    }

    @PostMapping("/complaint/{complaintId}/cancel")
    public String cancelComplaint(@PathVariable String complaintId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(userDetails);
        try {
            complaintService.cancelComplaint(complaintId, user);
            redirectAttributes.addFlashAttribute("successMsg", "Complaint cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/student/complaints";
    }

    private List<String> getCategories() {
        return List.of(
            "Furniture & Fixtures",
            "Electrical Issues",
            "Plumbing & Water",
            "IT & Computer Equipment",
            "Projector & AV Equipment",
            "Wi-Fi & Networking",
            "Cleanliness & Sanitation",
            "Structural & Civil",
            "Safety & Security",
            "Other"
        );
    }
}
