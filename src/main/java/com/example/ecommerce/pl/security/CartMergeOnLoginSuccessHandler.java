// src/main/java/com/example/ecommerce/pl/security/CartMergeOnLoginSuccessHandler.java
package com.example.ecommerce.pl.security;

import com.example.ecommerce.il.interfaces.CartService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import java.io.IOException;
import java.net.URI;

/**
 * Au login :
 *  1) Fusionne le panier invité (session) vers le panier DB de l'utilisateur.
 *  2) Redirige uniquement vers une SavedRequest "valide" (GET + pas un asset).
 *     Sinon, fallback propre vers /products.
 *
 * NB : pas d'annotation @Component — ce handler est fourni par un @Bean dans SecurityConfig.
 */
public class CartMergeOnLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final CartService cartService;

    // Cache des requêtes sauvegardées (SavedRequest). On utilise explicitement HttpSessionRequestCache.
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public CartMergeOnLoginSuccessHandler(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws ServletException, IOException {

        // 1) Fusion panier invité -> DB (si session existe)
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                cartService.mergeSessionIntoDb(session);
            } catch (Exception ignored) {
                // Ici on ne bloque pas le login si la fusion échoue, on pourrait logger un warn si besoin.
                // log.warn("Cart merge failed", ignored);
            }
        }

        // 2) Récupère la SavedRequest (dernière ressource protégée)
        SavedRequest saved = requestCache.getRequest(request, response);

        if (saved != null) {
            String method = saved.getMethod();
            String redirectUrl = saved.getRedirectUrl();

            // On n'autorise la redirection que si :
            // - la méthode est GET
            // - l'URL n'est pas un asset (/uploads, /images, /css, /js, /webjars, /favicon.ico)
            boolean isGet = "GET".equalsIgnoreCase(method);
            boolean okTarget = isGet && isPageUrlNotAsset(redirectUrl, request.getContextPath());

            if (okTarget) {
                // Laisse le comportement standard rediriger vers la page sauvegardée
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            } else {
                // SavedRequest "non valide" -> on la purge pour éviter un redirect bizarre
                requestCache.removeRequest(request, response);
            }
        }

        // 3) Fallback propre vers le catalogue
        String ctx = request.getContextPath();
        response.sendRedirect(ctx + "/products");
    }

    /**
     * Vérifie que l'URL de redirection ressemble à une "page" de l'app et non à un asset.
     * - Exclut /uploads/**, /images/**, /css/**, /js/**, /webjars/**, /favicon.ico
     */
    private boolean isPageUrlNotAsset(String absoluteUrl, String contextPath) {
        try {
            URI uri = URI.create(absoluteUrl);
            String path = uri.getPath(); // on ne garde que le chemin, pas le host
            if (path == null) return false;

            // Normalise le context-path (peut être vide)
            String ctx = (contextPath == null) ? "" : contextPath;

            // Détection d'assets à exclure
            return !(path.startsWith(ctx + "/uploads/") ||
                    path.startsWith(ctx + "/images/")  ||
                    path.startsWith(ctx + "/css/")     ||
                    path.startsWith(ctx + "/js/")      ||
                    path.startsWith(ctx + "/webjars/") ||
                    path.equals(ctx + "/favicon.ico"));
        } catch (Exception e) {
            // Si l'URL est invalide, par prudence on considère que ce n'est PAS une page valide.
            return false;
        }
    }
}
