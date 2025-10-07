package com.example.ecommerce.dl.entities;

import com.example.ecommerce.dl.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * User entity: holds authentication data (email/password) and role.
 * Password must be stored hashed (BCrypt) - handled in BLL at registration time.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt hash (never plain text!)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }
}