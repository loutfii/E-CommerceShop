// src/main/java/com/example/ecommerce/pl/controllers/CartController.java
package com.example.ecommerce.pl.controllers;

import com.example.ecommerce.il.interfaces.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

/**
 * Contrôleur Panier
 * -----------------
 * - Ajout : toast + redirection (Referer si présent).
 * - Vue /cart : affiche le contenu du panier.
 * - Update : incr/decr (redirige sur /cart).
 * - Remove : suppression dédiée via POST /cart/remove (redirige sur /cart).
 *
 * Les toasts sont affichés par macros.ftlh (flash "toast" / "error").
 */
@Controller
@RequestMapping("/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    /** Sentinel interne (doit rester cohérent avec l'implémentation du service). */
    private static final int REMOVE_DELTA = Integer.MIN_VALUE;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** Ajoute un produit au panier puis redirige (Referer ou /products). */
    @PostMapping("/add")
    public String add(@RequestParam(required = false) Long productId,
                      @RequestParam(defaultValue = "1") int quantity,
                      HttpSession session,
                      HttpServletRequest request,
                      RedirectAttributes ra) {

        if (productId == null || productId <= 0L) {
            ra.addFlashAttribute("error", "❌ ProductId manquant/illégal.");
            log.warn("[CART][ADD] productId invalide: {}", productId);
            return "redirect:/products";
        }

        int safeQty = Math.max(1, quantity);
        log.info("[CART][ADD] pid={} qty={} (sessionId={})", productId, safeQty, session.getId());

        try {
            cartService.add(productId, safeQty, session);
            ra.addFlashAttribute("toast", "✅ " + safeQty + " item(s) added to cart");
        } catch (Exception e) {
            log.error("[CART][ADD] échec pid={} : {}", productId, e.getMessage(), e);
            ra.addFlashAttribute("error", "❌ Impossible d'ajouter cet article.");
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/products";
    }

    /** Affiche le panier. */
    @GetMapping
    public String view(HttpSession session, Model model) {
        model.addAttribute("cart", cartService.getCurrentCart(session));
        return "cart/view";
    }

    /**
     * Met à jour la quantité (incr/decr) et reste sur /cart.
     * (La suppression passe par /cart/remove pour éviter l'ambiguïté.)
     */
    @PostMapping("/update")
    public String update(@RequestParam(required = false) Long productId,
                         @RequestParam String op,
                         HttpSession session,
                         RedirectAttributes ra) {

        if (productId == null || productId <= 0L) {
            ra.addFlashAttribute("error", "❌ ProductId manquant/illégal.");
            log.warn("[CART][UPDATE] productId invalide: {}", productId);
            return "redirect:/cart";
        }

        String opNorm = (op == null) ? "" : op.trim().toLowerCase(Locale.ROOT);
        int delta = switch (opNorm) {
            case "incr" -> 1;
            case "decr" -> -1;
            default -> 0;
        };

        if (delta == 0) {
            ra.addFlashAttribute("error", "❌ Operation inconnue.");
            log.warn("[CART][UPDATE] op inconnue='{}' pid={}", op, productId);
            return "redirect:/cart";
        }

        log.info("[CART][UPDATE] pid={} op={} delta={} (sessionId={})", productId, opNorm, delta, session.getId());
        try {
            cartService.updateQuantity(productId, delta, session);
            ra.addFlashAttribute("toast", "🛒 Cart updated");
        } catch (Exception e) {
            log.error("[CART][UPDATE] échec pid={} : {}", productId, e.getMessage(), e);
            ra.addFlashAttribute("error", "❌ Impossible de mettre à jour l'article.");
        }
        return "redirect:/cart";
    }

    /** Suppression dédiée d'un article par productId. */
    @PostMapping("/remove")
    public String remove(@RequestParam(required = false) Long productId,
                         HttpSession session,
                         RedirectAttributes ra) {

        if (productId == null || productId <= 0L) {
            ra.addFlashAttribute("error", "❌ ProductId manquant/illégal.");
            log.warn("[CART][REMOVE] productId invalide: {}", productId);
            return "redirect:/cart";
        }

        log.info("[CART][REMOVE] pid={} (sessionId={})", productId, session.getId());
        try {
            cartService.updateQuantity(productId, REMOVE_DELTA, session);
            ra.addFlashAttribute("toast", "🗑️ Item removed");
        } catch (Exception e) {
            log.error("[CART][REMOVE] échec pid={} : {}", productId, e.getMessage(), e);
            ra.addFlashAttribute("error", "❌ Impossible de supprimer l'article.");
        }
        return "redirect:/cart";
    }
}
