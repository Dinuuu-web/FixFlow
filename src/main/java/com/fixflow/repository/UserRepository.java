package com.fixflow.repository;

import com.fixflow.entity.User;
import com.fixflow.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRollNumber(String rollNumber);

    List<User> findByRole(Role role);

    List<User> findByRoleAndEnabledTrue(Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.enabled = true ORDER BY u.fullName")
    List<User> findActiveStaffByRole(Role role);

    long countByRole(Role role);
}
