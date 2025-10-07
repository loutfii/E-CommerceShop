package com.example.ecommerce.bll.services.impls;

import com.example.ecommerce.dal.repositories.UserRepository;
import com.example.ecommerce.dl.entities.User;
import com.example.ecommerce.dl.enums.Role;
import com.example.ecommerce.il.interfaces.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*Pourquoi @Transactional ici ? Pour garantir que la création utilisateur est atomique.*/

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthServiceImpl(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void registerUser(String email, String rawPassword) {
        createUser(email, rawPassword, Role.USER);
    }

    @Override
    public void registerAdmin(String email, String rawPassword) {
        createUser(email, rawPassword, Role.ADMIN);
    }

    private void createUser(String email, String rawPassword, Role role) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        var u = User.builder()
                .email(email.trim().toLowerCase())
                .password(encoder.encode(rawPassword))
                .role(role)
                .build();
        users.save(u);
    }
}