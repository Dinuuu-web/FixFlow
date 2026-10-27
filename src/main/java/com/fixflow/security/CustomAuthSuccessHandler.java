package com.fixflow.security;

import com.fixflow.entity.User;
import com.fixflow.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String role = authentication.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority())
            .orElse("");

        String redirectUrl;
        switch (role) {
            case "ROLE_ADMIN" -> redirectUrl = "/admin/dashboard";
            case "ROLE_MAINTENANCE_STAFF" -> redirectUrl = "/staff/dashboard";
            default -> redirectUrl = "/student/dashboard";
        }
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
