package com.fixflow.service;

import com.fixflow.dto.RegisterDto;
import com.fixflow.entity.*;
import com.fixflow.repository.DepartmentRepository;
import com.fixflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerStudent(RegisterDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (dto.getRollNumber() != null && !dto.getRollNumber().isBlank()
                && userRepository.existsByRollNumber(dto.getRollNumber())) {
            throw new IllegalArgumentException("Roll number already in use");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = new User();
        user.setFullName(dto.getFullName().trim());
        user.setEmail(dto.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.STUDENT);
        user.setRollNumber(dto.getRollNumber() != null ? dto.getRollNumber().trim() : null);
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        user.setEnabled(true);

        if (dto.getDepartmentId() != null) {
            departmentRepository.findById(dto.getDepartmentId()).ifPresent(user::setDepartment);
        }

        User saved = userRepository.save(user);
        log.info("New student registered: {}", saved.getEmail());
        return saved;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAllMaintenanceStaff() {
        return userRepository.findActiveStaffByRole(Role.MAINTENANCE_STAFF);
    }

    public List<User> findAllAdmins() {
        return userRepository.findByRole(Role.ADMIN);
    }

    @Transactional
    public User createStaffUser(String fullName, String email, String password,
                                Role role, Long departmentId) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email.toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        if (departmentId != null) {
            departmentRepository.findById(departmentId).ifPresent(user::setDepartment);
        }
        return userRepository.save(user);
    }

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
