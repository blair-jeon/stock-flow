package io.github.blairjeon.stockflow.user.entity;

import io.github.blairjeon.stockflow.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Getter
@Table(name = "user_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseTimeEntity {
    @Id
    @Column(nullable = false, updatable = false, unique = true)
    private long userId;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String address;

    @Version
    private Long version;

    private UserProfile(
            Long userId,
            String lastName,
            String firstName,
            String phoneNumber,
            String email,
            String address
    ){
        this.userId = Objects.requireNonNull(userId);
        this.lastName = lastName;
        this.firstName = firstName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
    }

    public static UserProfile create(
            Long userId,
            String lastName,
            String firstName,
            String phoneNumber,
            String email,
            String address
    ){
        return new UserProfile(userId, lastName, firstName, phoneNumber, email, address);
    }

    public void updateProfile(
            String lastName,
            String firstName,
            String phoneNumber,
            String email,
            String address
    ) {
        if (lastName != null) { this.lastName = lastName; }
        if (firstName != null) { this.firstName = firstName; }
        if (phoneNumber != null) { this.phoneNumber = phoneNumber; }
        if (email != null) { this.email = email; }
        if (address != null) { this.address = address; }
    }
}