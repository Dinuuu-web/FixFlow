package com.fixflow.controller;

import com.fixflow.ai.GeminiService;
import com.fixflow.dto.AiRequestDto;
import com.fixflow.entity.Complaint;
import com.fixflow.entity.Role;
import com.fixflow.service.ComplaintService;
import com.fixflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final GeminiService geminiService;
    private final ComplaintService complaintService;
    private final UserService userService;

    @GetMapping("/assistant")
    public String assistantPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("aiConfigured", geminiService.isConfigured());
        return "ai/assistant";
    }

    /**
     * REST endpoint for AI operations.
     * Returns JSON so the page can update dynamically.
     */
    @PostMapping("/api/assist")
    @ResponseBody
    public ResponseEntity<Map<String, String>> assist(
            @RequestBody AiRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Content is required"));
        }
        if (request.getContent().length() > 4000) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Input too long (max 4000 chars)"));
        }

        String result;
        try {
            result = switch (request.getAction()) {
                case "improve"       -> geminiService.improveComplaint(request.getContent());
                case "category"      -> geminiService.suggestCategory(request.getContent());
                case "priority"      -> geminiService.suggestPriority(request.getContent());
                case "troubleshoot"  -> geminiService.troubleshootingGuidance(request.getContent());
                case "summary"       -> handleAdminSummary(request, userDetails);
                default              -> "Unknown action: " + request.getAction();
            };
        } catch (Exception e) {
            log.error("AI request failed", e);
            result = "An error occurred processing your request. Please try again.";
        }

        return ResponseEntity.ok(Map.of("result", result));
    }

    private String handleAdminSummary(AiRequestDto request, UserDetails userDetails) {
        // Only allow admins to generate complaint summaries
        boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return "Admin summary is only available to administrators.";
        }
        if (request.getComplaintId() == null || request.getComplaintId().isBlank()) {
            return "Complaint ID is required for summary action.";
        }
        try {
            Complaint complaint = complaintService.getByComplaintId(request.getComplaintId());
            return geminiService.adminSummary(
                complaint.getTitle(),
                complaint.getDescription(),
                complaint.getCategory(),
                complaint.getPriority().name(),
                complaint.getLocation(),
                complaint.getStatus().name()
            );
        } catch (Exception e) {
            return "Complaint not found: " + request.getComplaintId();
        }
    }
}
