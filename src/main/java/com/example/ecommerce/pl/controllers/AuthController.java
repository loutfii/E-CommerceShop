package com.example.ecommerce.pl.controllers;

import com.example.ecommerce.il.interfaces.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController
 * --------------
 * Responsibilities:
 *  - Show the login page and surface login messages coming from Spring Security
 *    (e.g., /auth/login?error, /auth/login?logout) without relying on a 'param' object in FTL.
 *  - Show the register page and handle user registration.
 *
 * Notes:
 *  - The login POST is handled by Spring Security's filter chain (not by this controller).
 *    Make sure your SecurityConfig uses .loginPage("/auth/login") AND
 *    .loginProcessingUrl("/auth/login") if your form POSTs to /auth/login.
 *    Otherwise, either change the form action to /login or configure loginProcessingUrl.
 *
 *  - The templates should render ${error} and ${message} if present:
 *      - login.ftlh: show ${error} / ${message}
 *      - register.ftlh: show ${error} if registration fails
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * GET /auth/login
     * Show login page and optionally display messages from query parameters:
     *  - ?error      -> invalid credentials (set by Spring Security on failure)
     *  - ?logout     -> successful logout (set by Spring Security)
     *  - ?registered -> account just created (we add this on successful register)
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid credentials. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out.");
        }
        if (registered != null) {
            model.addAttribute("message", "Account created. You can now sign in.");
        }
        return "auth/login";
    }

    /**
     * GET /auth/register
     * Show the registration form.
     */
    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }

    /**
     * POST /auth/register
     * Register a new user, basic field checks are performed here.
     * On success -> redirect to /auth/login?registered
     * On failure -> return register view with an error message.
     */
    @PostMapping("/register")
    public String register(@RequestParam @Email String email,
                           @RequestParam @NotBlank String password,
                           @RequestParam @NotBlank String confirm,
                           Model model) {

        // Normalize trivial whitespace in email
        String normalizedEmail = email == null ? null : email.trim();

        // Basic checks
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            model.addAttribute("error", "Email is required.");
            return "auth/register";
        }
        if (!password.equals(confirm)) {
            model.addAttribute("error", "Passwords do not match.");
            return "auth/register";
        }

        try {
            authService.registerUser(normalizedEmail, password);
            // Add a flag so the login page can show a friendly message
            return "redirect:/auth/login?registered";
        } catch (Exception e) {
            // Surface the service error (e.g., duplicate email)
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
}
