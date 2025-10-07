// src/main/java/com/example/ecommerce/bll/services/impls/CartServiceImpl.java
package com.example.ecommerce.bll.services.impls;

import com.example.ecommerce.bll.mappers.DtoMapper;
import com.example.ecommerce.dal.repositories.*;
import com.example.ecommerce.dl.entities.*;
import com.example.ecommerce.dl.enums.CartStatus;
import com.example.ecommerce.il.dto.CartDto;
import com.example.ecommerce.il.interfaces.CartService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private static final String SESSION_CART = "GUEST_CART"; // Map<Long, Integer>
    private static final int REMOVE_DELTA = Integer.MIN_VALUE;

    private final ProductRepository products;
    private final UserRepository users;
    private final CartRepository carts;
    private final CartLineRepository lines;

    public CartServiceImpl(ProductRepository products, UserRepository users, CartRepository carts, CartLineRepository lines) {
        this.products = products;
        this.users = users;
        this.carts = carts;
        this.lines = lines;
    }

    // -------- Compteur (header) --------
    @Override
    @Transactional(readOnly = true)
    public int getItemCount(HttpSession session) {
        User user = currentUserOrNull();
        if (user == null) {
            Map<Long, Integer> map = getOrCreateSessionCart(session);
            int count = map.values().stream().mapToInt(Integer::intValue).sum();
            log.debug("[CART][COUNT][GUEST] items={} (sessionId={})", count, session.getId());
            return count;
        }
        var cart = getSingleOpenCartOrNull(user);
        if (cart == null) return 0;
        int count = cart.getLines().stream().mapToInt(CartLine::getQuantity).sum();
        log.debug("[CART][COUNT][USER:{}] items={}", user.getEmail(), count);
        return count;
    }

    // -------- Ajout --------
    @Override
    public void add(Long productId, int quantity, HttpSession session) {
        if (quantity < 1) quantity = 1;
        User user = currentUserOrNull();
        if (user == null) {
            addToSession(session, productId, quantity);
            log.debug("[CART][ADD][GUEST] pid={} +{} (sessionId={})", productId, quantity, session.getId());
            return;
        }

        // IMPORTANT: normaliser à un seul panier OPEN (fusion si doublons)
        var cart = getOrCreateSingleOpenCart(user);

        var product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        var line = lines.findByCartAndProduct(cart, product).orElse(null);
        if (line == null) {
            line = new CartLine();
            line.setCart(cart);
            line.setProduct(product);
            line.setQuantity(quantity);
            lines.save(line);
            log.debug("[CART][ADD][USER:{}] pid={} set={}", user.getEmail(), productId, quantity);
        } else {
            line.setQuantity(line.getQuantity() + quantity);
            log.debug("[CART][ADD][USER:{}] pid={} +{} => {}", user.getEmail(), productId, quantity, line.getQuantity());
        }
    }

    // -------- MAJ quantités / Suppression --------
    @Override
    public void updateQuantity(Long productId, int delta, HttpSession session) {
        User user = currentUserOrNull();

        // --- Invité (session)
        if (user == null) {
            if (delta == REMOVE_DELTA) {
                boolean existed = removeFromSession(session, productId);
                log.info("[CART][REMOVE][GUEST] pid={} removed={} (sessionId={})", productId, existed, session.getId());
                return;
            }
            int before = getItemCount(session);
            updateSessionDelta(session, productId, delta);
            int after = getItemCount(session);
            log.info("[CART][UPDATE][GUEST] pid={} delta={} items:{}->{} (sessionId={})",
                    productId, delta, before, after, session.getId());
            return;
        }

        // --- Connecté (DB) : d'abord normaliser vers un seul panier OPEN
        var cart = getOrCreateSingleOpenCart(user);

        var product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        var line = lines.findByCartAndProduct(cart, product).orElse(null);
        if (line == null) {
            log.warn("[CART][UPDATE][USER:{}] pid={} introuvable dans le panier", user.getEmail(), productId);
            return;
        }

        if (delta == REMOVE_DELTA) {
            System.out.println(lines);
            log.info(line.getId().toString());
            lines.deleteById(line.getId());
            log.info("[CART][REMOVE][USER:{}] pid={} OK", user.getEmail(), productId);
            // Relire les lignes restantes du panier puis les logger
            List<CartLine> remainingLines = this.lines.findAll(); // 'lines' = CartLineRepository
            for (CartLine l : remainingLines) {
                log.info("[CART][REMOVE][USER:{}] pid={} qty={}",
                        user.getEmail(),
                        l.getProduct() != null ? l.getProduct().getId() : null,
                        l.getQuantity());
            }

            return;
        }

        int q = line.getQuantity() + delta;
        if (q <= 0) {
            lines.delete(line);
            log.info("[CART][UPDATE->REMOVE][USER:{}] pid={} q<=0 -> deleted", user.getEmail(), productId);
        } else {
            line.setQuantity(q);
            log.info("[CART][UPDATE][USER:{}] pid={} setQty={}", user.getEmail(), productId, q);
        }
    }

    // -------- Merge session -> DB (au login) --------
    @Override
    public void mergeSessionIntoDb(HttpSession session) {
        Map<Long, Integer> map = getSessionCart(session);
        if (map == null || map.isEmpty()) return;

        User user = currentUserOrNull();
        if (user == null) return;

        // On merge aussi côté DB si plusieurs OPEN traînent
        var cart = getOrCreateSingleOpenCart(user);

        map.forEach((pid, qty) -> {
            var product = products.findById(pid).orElse(null);
            if (product == null) return;
            var line = lines.findByCartAndProduct(cart, product).orElse(null);
            if (line == null) {
                line = new CartLine();
                line.setCart(cart);
                line.setProduct(product);
                line.setQuantity(qty);
                lines.save(line);
            } else {
                line.setQuantity(line.getQuantity() + qty);
            }
        });

        session.removeAttribute(SESSION_CART);
        log.info("[CART][MERGE] session->db done (user={})", user.getEmail());
    }

    // -------- Lecture pour la vue --------
    @Override
    @Transactional(readOnly = true)
    public CartDto getCurrentCart(HttpSession session) {
        User user = currentUserOrNull();
        if (user == null) {
            Map<Long, Integer> map = getOrCreateSessionCart(session);
            if (map.isEmpty()) return new CartDto(List.of(), 0, java.math.BigDecimal.ZERO);

            var lineDtos = map.entrySet().stream().map(e -> {
                var p = products.findById(e.getKey()).orElse(null);
                if (p == null) return null;
                var cl = new CartLine(); cl.setProduct(p); cl.setQuantity(e.getValue());
                return DtoMapper.toCartLineDto(cl);
            }).filter(Objects::nonNull).toList();

            return DtoMapper.toCartDto(lineDtos);
        }

        // Lire le panier unifié (le plus récent)
        var cart = getSingleOpenCartOrNull(user);
        if (cart == null || cart.getLines().isEmpty()) {
            return new CartDto(List.of(), 0, java.math.BigDecimal.ZERO);
        }
        var lineDtos = cart.getLines().stream().map(DtoMapper::toCartLineDto).toList();
        return DtoMapper.toCartDto(lineDtos);
    }

    // ==========================
    // Helpers "anti-doublons"
    // ==========================

    /** Renvoie le dernier panier OPEN ; s'il y en a plusieurs, fusionne et purge. */
    private Cart getOrCreateSingleOpenCart(User user) {
        var all = carts.findAllByUserAndStatusOrderByUpdatedAtDesc(user, CartStatus.OPEN);
        if (all.isEmpty()) {
            var c = new Cart();
            c.setUser(user);
            c.setStatus(CartStatus.OPEN);
            return carts.save(c);
        }
        var keeper = all.getFirst();
        if (all.size() > 1) {
            mergeAndPurgeOpenCarts(keeper, all.subList(1, all.size()));
            log.warn("[CART][NORMALIZE][USER:{}] {} duplicate OPEN cart(s) merged+purged", user.getEmail(), all.size() - 1);
        }
        return keeper;
    }

    /** Renvoie le panier OPEN le plus récent, ou null. */
    @Transactional(readOnly = true)
    protected Cart getSingleOpenCartOrNull(User user) {
        var all = carts.findAllByUserAndStatusOrderByUpdatedAtDesc(user, CartStatus.OPEN);
        return all.isEmpty() ? null : all.getFirst();
    }

    /** Fusionne les lignes des doublons dans keeper puis supprime les doublons. */
    private void mergeAndPurgeOpenCarts(Cart keeper, List<Cart> duplicates) {
        for (Cart dup : duplicates) {
            // Fusion des lignes
            for (CartLine l : dup.getLines()) {
                var product = l.getProduct();
                var existing = lines.findByCartAndProduct(keeper, product).orElse(null);
                if (existing == null) {
                    var nl = new CartLine();
                    nl.setCart(keeper);
                    nl.setProduct(product);
                    nl.setQuantity(l.getQuantity());
                    lines.save(nl);
                } else {
                    existing.setQuantity(existing.getQuantity() + l.getQuantity());
                }
            }
            // Supprimer les lignes du doublon puis le doublon
            lines.deleteAll(dup.getLines());
            carts.delete(dup);
        }
    }

    // -------- Helpers session (invité) --------
    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getOrCreateSessionCart(HttpSession session) {
        Map<Long, Integer> map = (Map<Long, Integer>) session.getAttribute(SESSION_CART);
        if (map == null) {
            map = new LinkedHashMap<>();
            session.setAttribute(SESSION_CART, map);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getSessionCart(HttpSession session) {
        return (Map<Long, Integer>) session.getAttribute(SESSION_CART);
    }

    private void addToSession(HttpSession session, Long productId, int qty) {
        var map = getOrCreateSessionCart(session);
        map.merge(productId, qty, Integer::sum);
    }

    private boolean removeFromSession(HttpSession session, Long productId) {
        var map = getOrCreateSessionCart(session);
        return map.remove(productId) != null;
    }

    private void updateSessionDelta(HttpSession session, Long productId, int delta) {
        var map = getOrCreateSessionCart(session);
        map.computeIfPresent(productId, (k, v) -> v + delta);
        map.values().removeIf(q -> q == null || q <= 0);
    }

    // -------- User courant --------
    private User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        return users.findByEmail(auth.getName()).orElse(null);
    }
}
