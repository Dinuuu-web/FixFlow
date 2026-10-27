package com.fixflow.controller;

import com.fixflow.dto.RegisterDto;
import com.fixflow.repository.DepartmentRepository;
import com.fixflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final DepartmentRepository departmentRepository;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("errorMsg", "Invalid email or password.");
        if (logout != null) model.addAttribute("logoutMsg", "You have been logged out.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        model.addAttribute("departments", departmentRepository.findByActiveTrue());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDto") RegisterDto dto,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        if (result.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findByActiveTrue());
            return "auth/register";
        }

        try {
            userService.registerStudent(dto);
            redirectAttributes.addFlashAttribute("successMsg",
                "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("departments", departmentRepository.findByActiveTrue());
            return "auth/register";
        }
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
