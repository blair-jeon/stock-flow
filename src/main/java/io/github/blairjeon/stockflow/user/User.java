package io.github.blairjeon.stockflow.user;

import io.github.blairjeon.stockflow.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_loginId",
                columnNames = "login_id"
        )
)
public class User extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    private User(String loginId, String passwordHash, String displayName){
        this.loginId = Objects.requireNonNull(loginId);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.displayName = Objects.requireNonNull(displayName);
        this.userRole = UserRole.USER;
    }

    public static User create(String loginId, String passwordHash, String displayName){
        return new User(loginId, passwordHash, displayName);
    }

    public void updatePassword(String passwordHash){
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    public void updateDisplayName(String displayName){
        this.displayName = Objects.requireNonNull(displayName);
    }
}