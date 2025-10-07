// src/main/java/com/example/ecommerce/pl/controllers/AccountController.java
package com.example.ecommerce.pl.controllers;

import com.example.ecommerce.dal.repositories.CartRepository;
import com.example.ecommerce.dal.repositories.UserRepository;
import com.example.ecommerce.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Suppression de compte (self-service) pour l'utilisateur connecté.
 * - Supprime les paniers de l'utilisateur (cascade -> lignes).
 * - Supprime l'utilisateur.
 * - Déconnecte et invalide la session.
 */
@Controller
@RequestMapping("/account")
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private final UserRepository users;
    private final CartRepository carts;

    public AccountController(UserRepository users, CartRepository carts) {
        this.users = users;
        this.carts = carts;
    }

    @PostMapping("/delete")
    public String deleteAccount(HttpServletRequest request,
                                HttpServletResponse response,
                                HttpSession session,
                                RedirectAttributes ra) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            ra.addFlashAttribute("error", "Not authenticated.");
            return "redirect:/products";
        }

        String email = auth.getName();
        User u = users.findByEmail(email).orElse(null);
        if (u == null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
            ra.addFlashAttribute("error", "Account not found.");
            return "redirect:/products";
        }

        // 1) Supprimer les paniers liés (lines supprimées via cascade/orphanRemoval sur Cart)
        List<com.example.ecommerce.dl.entities.Cart> allCarts = carts.findAllByUser(u);
        if (!allCarts.isEmpty()) {
            carts.deleteAll(allCarts);
        }

        // 2) Supprimer l'utilisateur
        users.delete(u);

        // 3) Logout + invalider session
        new SecurityContextLogoutHandler().logout(request, response, auth);
        if (session != null) {
            try { session.invalidate(); } catch (IllegalStateException ignored) {}
        }

        // 4) Feedback (toast côté UI)
        ra.addFlashAttribute("toast", "✅ Your account has been deleted.");
        return "redirect:/products";
    }
}
