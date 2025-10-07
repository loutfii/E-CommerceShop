package com.example.ecommerce.il.interfaces;

import com.example.ecommerce.il.dto.CartDto;
import jakarta.servlet.http.HttpSession;

/*Pourquoi retourner un CartDto depuis IL plutôt que des entités ?
Pour protéger la couche PL des détails JPA (lazy loading, etc.) et stabiliser le contrat*/

/**
 * Contrat du panier:
 * - Invité : panier en session
 * - Connecté : panier en DB
 * - Merge : fusion session -> DB lors du login
 *
 * Note: On accepte HttpSession ici pour rester pragmatique côté MVC (PL).
 * Alternative plus "pure": définir un port d'abstraction de session, mais c'est overkill pour cet exercice.
 */
public interface CartService {

    /**
     * Nombre d'articles dans le panier (pour le badge header).
     * - Si connecté: somme des lignes du Cart OPEN en DB
     * - Si invité : somme des quantités dans la session
     */
    int getItemCount(HttpSession session);

    /**
     * Ajoute un produit au panier.
     * - Si ligne déjà présente: incrémente la quantité
     * - Valide quantité >= 1
     */
    void add(Long productId, int quantity, HttpSession session);

    /**
     * Incrémente/décrémente/supprime une ligne.
     * - delta > 0 => augmente
     * - delta < 0 => diminue (si <=0 => supprime)
     */
    void updateQuantity(Long productId, int delta, HttpSession session);

    /**
     * Fusionne le panier de session (invité) dans le panier DB (user connecté).
     * Appelé après succès de login (AuthenticationSuccessHandler).
     */
    void mergeSessionIntoDb(HttpSession session);

    /**
     * Retourne le panier pour affichage (DB si connecté, sinon session).
     */
    CartDto getCurrentCart(HttpSession session);
}