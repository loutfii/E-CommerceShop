package com.example.ecommerce.pl.controllers;

import com.example.ecommerce.bll.services.StripeService;
import com.example.ecommerce.il.dto.CartDto;
import com.example.ecommerce.il.interfaces.CartService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * =============================================================================
 * CheckoutController — Orchestration du paiement avec Stripe Checkout (hébergé)
 * =============================================================================
 *
 * Rôle du contrôleur :
 * 1) GET /checkout
 *    - Lit le panier courant (session) via CartService.
 *    - Prépare un ViewModel minimal pour la vue FreeMarker (articles + total).
 *    - Affiche un récapitulatif avec un bouton "Payer avec Stripe".
 *
 * 2) POST /create-checkout-session
 *    - Transforme chaque ligne du panier en LineItem Stripe (nom, quantité,
 *      prix unitaire en CENTIMES).
 *    - Crée une Checkout Session Stripe (page de paiement hébergée) puis
 *      redirige l’utilisateur vers l’URL Stripe.
 *    - Ne modifie PAS la base de données ici : la preuve de paiement viendra
 *      du webhook Stripe (étape 3) pour éviter toute fraude.
 *
 * 3) GET /checkout/success
 *    - Affiche simplement une page de succès après le retour de Stripe.
 *    - ⚠ Ne pas vider le panier ici (URL invocable manuellement). Le vidage
 *      se fera côté serveur via le webhook quand Stripe confirme le paiement.
 *
 * 4) GET /checkout/cancel
 *    - Affiche une page d’annulation et propose de réessayer ou revenir au panier.
 *
 * Sécurité & robustesse :
 * - Les prix proviennent uniquement du serveur (pas du front), donc pas de
 *   confiance dans les montants envoyés par l’utilisateur.
 * - Les URLs success/cancel sont absolues (calculées dynamiquement à partir
 *   de la requête), adaptées au dev et à la prod.
 * - Conversion des euros → centimes via RoundingMode.HALF_UP (norme courante).
 */
@Controller
@RequiredArgsConstructor
public class CheckoutController {

    /** Accès au panier (lecture des lignes, total, etc.). */
    private final CartService cartService;

    /** Service enveloppant le SDK Stripe (clé secrète initialisée au démarrage). */
    private final StripeService stripeService;

    // -------------------------------------------------------------------------
    // ViewModel (interne à la vue) : on expose uniquement ce qui est utile
    // à l’affichage du récapitulatif, sans fuiter les détails du domaine.
    // -------------------------------------------------------------------------
    public record CheckoutItemVM(
            String name,          // Nom affiché du produit
            long quantity,        // Quantité achetée
            BigDecimal unitPrice, // Prix unitaire en euros (grande unité)
            BigDecimal lineTotal  // Total ligne (unitPrice * quantity), en euros
    ) {}

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Convertit un montant en euros (grande unité) vers les centimes (petite unité)
     * exigés par Stripe (entier).
     *
     * Exemple :
     *  - 12,34 €  →  1234 (centimes)
     *
     * Pourquoi RoundingMode.HALF_UP ?
     *  - Comportement attendu pour l’arrondi monétaire (pas de constantes dépréciées).
     */
    private long toCents(BigDecimal amount) {
        // movePointRight(2) ≈ amount * 100 ; setScale(0, RoundingMode.HALF_UP) évite les fractions
        return amount
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP) // ✅ API moderne (int, int) dépréciée évitée
                .longValueExact();                  // jette ArithmeticException si dépassement (improbable ici)
    }

    /**
     * Construit l’URL de base absolue à partir de la requête (protocole + host [+ port]).
     * Exemples : http://localhost:8080  |  https://shop.example.com
     */
    private String baseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null) // supprime le chemin courant (/checkout, /create-checkout-session, etc.)
                .build()
                .toUriString();
    }

    // =========================================================================
    // 1) GET /checkout — Afficher le récapitulatif
    // =========================================================================

    /**
     * Affiche la page récapitulatif du panier.
     *
     * - Si l’utilisateur n’est pas authentifié, on le redirige vers la page de
     *   login avec un paramètre de retour (?next=/checkout).
     * - On lit le panier de la session, on le mappe vers un petit ViewModel,
     *   on calcule le total et on envoie le tout à la vue FreeMarker.
     */
    @GetMapping("/checkout")
    public String checkout(Authentication auth, HttpSession session, HttpServletRequest req, Model model) {
        // Garde : authentification requise (au cas où la config sécurité ne le force pas déjà)
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/auth/login?next=/checkout";
        }

        // Lecture du panier depuis la session via CartService
        CartDto cart = cartService.getCurrentCart(session);

        // Préparation des données pour la vue
        var items = new ArrayList<CheckoutItemVM>();
        BigDecimal total = BigDecimal.ZERO;

        if (cart != null && cart.lines() != null) {
            cart.lines().forEach(l -> {
                // Map minimal : nom, quantité, prix unitaire, total ligne
                var vm = new CheckoutItemVM(
                        l.productName(),
                        l.quantity() == null ? 0L : l.quantity().longValue(),
                        l.unitPrice(),
                        l.lineTotal()
                );
                items.add(vm);
            });
            // Le total global est fourni par le CartDto ; fallback sur 0 si null
            total = (cart.totalAmount() != null) ? cart.totalAmount() : BigDecimal.ZERO;
        }

        // Variables exposées à la vue
        model.addAttribute("cartEmpty", items.isEmpty());
        model.addAttribute("items", items);
        model.addAttribute("total", total);

        // Rend la template: templates/checkout/checkout.ftlh
        return "checkout/checkout";
    }

    // =========================================================================
    // 2) POST /create-checkout-session — Créer la session Stripe Checkout
    // =========================================================================

    /**
     * Crée une session Stripe Checkout depuis le panier courant puis redirige
     * l’utilisateur vers la page de paiement hébergée par Stripe.
     *
     * Sécurité :
     * - On relit le panier côté serveur et on calcule nous-mêmes les montants.
     * - Aucune écriture en base ici : la confirmation se fera via le webhook Stripe.
     *
     * Gestion d’erreurs :
     * - Panier vide → redirection vers /checkout?empty
     * - Exception Stripe → redirection vers /checkout?error
     */
    @PostMapping("/create-checkout-session")
    public String createCheckoutSession(Authentication auth, HttpSession session, HttpServletRequest req) {
        // Authentification requise
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/auth/login?next=/checkout";
        }

        // Relecture du panier
        CartDto cart = cartService.getCurrentCart(session);
        if (cart == null || cart.lines() == null || cart.lines().isEmpty()) {
            return "redirect:/checkout?empty";
        }

        try {
            // Construction des LineItems Stripe à partir des lignes du panier
            List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
            cart.lines().forEach(l -> {
                long qty = (l.quantity() == null) ? 0L : l.quantity().longValue();
                long unitInCents = toCents(l.unitPrice()); // € → centimes
                var li = stripeService.createLineItem(l.productName(), unitInCents, qty);
                lineItems.add(li);
            });

            // URLs absolues pour success/cancel
            String base = baseUrl(req);
            String success = base + "/checkout/success";
            String cancel  = base + "/checkout/cancel";

            // Création de la session hébergée Stripe + redirection
            Session sessionObj = stripeService.createCheckoutSession(lineItems, success, cancel);
            return "redirect:" + sessionObj.getUrl();

        } catch (StripeException e) {
            // Log technique possible ici (logger.warn/err)
            return "redirect:/checkout?error";
        }
    }

    // =========================================================================
    // 3) GET /checkout/success — Page de retour (ne pas muter le panier ici)
    // =========================================================================

    /**
     * Affiche la page de succès après le retour de Stripe.
     * ⚠ On ne touche PAS au panier ni aux commandes ici. La mise à jour
     * (vider le panier, marquer payé) sera effectuée par le webhook Stripe.
     */
    @GetMapping("/checkout/success")
    public String checkoutSuccess() {
        return "checkout/success";
    }

    // =========================================================================
    // 4) GET /checkout/cancel — Page d’annulation
    // =========================================================================

    /**
     * Affiche la page d’annulation du paiement (l’utilisateur peut réessayer).
     */
    @GetMapping("/checkout/cancel")
    public String checkoutCancel() {
        return "checkout/cancel";
    }
}
