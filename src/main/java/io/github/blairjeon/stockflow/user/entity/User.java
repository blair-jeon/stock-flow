package io.github.blairjeon.stockflow.user.entity;

import io.github.blairjeon.stockflow.common.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Getter
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_login_id",
                columnNames = "login_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id @GeneratedValue
    private long id;

    @NotBlank
    @Column(name = "login_id", nullable = false, updatable = false, length = 30)
    private String loginId;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private User(String loginId, String passwordHash){
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
    }

    public static User create(String loginId, String passwordHash){
        return new User(loginId, passwordHash);
    }

    public void updatePassword(String encodedPassword){
        this.passwordHash = Objects.requireNonNull(encodedPassword);
    }
}