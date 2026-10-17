package io.github.blairjeon.stockflow.user.repository;

import io.github.blairjeon.stockflow.user.entity.User;
import io.github.blairjeon.stockflow.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByIdAndStatus(Long id, UserStatus status);
}