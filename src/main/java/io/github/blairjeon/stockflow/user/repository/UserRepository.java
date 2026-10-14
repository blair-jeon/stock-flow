package io.github.blairjeon.stockflow.user.repository;

import io.github.blairjeon.stockflow.user.User;
import io.github.blairjeon.stockflow.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginId(String loginId);
    Optional<User> findByLoginId(String loginId);
    boolean existsByIdAndUserRole(Long userId, UserRole userRole);
}