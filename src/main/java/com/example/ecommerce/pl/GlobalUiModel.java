// src/main/java/com/example/ecommerce/pl/GlobalUiModel.java
package com.example.ecommerce.pl;

import com.example.ecommerce.il.interfaces.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expose des attributs globaux aux vues :
 * - isAuth / username / isAdmin
 * - cartItemCount
 * - toasts (depuis la session, posés par les handlers de sécurité ou les contrôleurs)
 */
@ControllerAdvice(annotations = Controller.class)
public class GlobalUiModel {

    private final CartService cartService;

    public GlobalUiModel(CartService cartService) { this.cartService = cartService; }

    @ModelAttribute
    public void expose(Model model, Authentication auth, HttpSession session) {
        // --- Auth / rôles ---
        boolean isAuth = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        boolean isAdmin = isAuth && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        model.addAttribute("isAuth", isAuth);
        model.addAttribute("username", isAuth ? auth.getName() : null);
        model.addAttribute("isAdmin", isAdmin);

        // --- Compteur panier (DB si connecté, sinon session) ---
        int count = 0;
        try {
            var cart = cartService.getCurrentCart(session);
            if (cart != null) count = cart.totalItems();
        } catch (Exception ignored) {}
        model.addAttribute("cartItemCount", count);

        // --- TOASTS : on récupère les flags mis en session par la sécurité / contrôleurs, puis on les supprime ---
        Object tSucc = session.getAttribute("FLASH_TOAST_SUCCESS");
        Object tErr  = session.getAttribute("FLASH_TOAST_ERROR");
        Object tInfo = session.getAttribute("FLASH_TOAST_INFO");
        if (tSucc != null) { model.addAttribute("toast_success", String.valueOf(tSucc)); session.removeAttribute("FLASH_TOAST_SUCCESS"); }
        if (tErr  != null) { model.addAttribute("toast_error",   String.valueOf(tErr));  session.removeAttribute("FLASH_TOAST_ERROR"); }
        if (tInfo != null) { model.addAttribute("toast_info",    String.valueOf(tInfo)); session.removeAttribute("FLASH_TOAST_INFO"); }
    }
}
