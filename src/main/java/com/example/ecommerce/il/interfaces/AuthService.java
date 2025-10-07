package com.example.ecommerce.il.interfaces;

/**
 * Contrat d'authentification/inscription.
 * Remarque: la logique "login" est gérée par Spring Security;
 * ici on expose seulement "register" (création compte).
 */
public interface AuthService {

    /**
     * Enregistre un nouvel utilisateur avec rôle USER (par défaut).
     * - Valide l'unicité de l'email.
     * - Hash le mot de passe (BCrypt).
     * @throws IllegalArgumentException si email déjà utilisé ou mot de passe invalide.
     */
    void registerUser(String email, String rawPassword);

    /**
     * Optionnel: création d'un admin (utilisé par DataInitializer).
     */
    void registerAdmin(String email, String rawPassword);
}
