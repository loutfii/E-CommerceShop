package com.example.ecommerce.bll.services.impls;

import com.example.ecommerce.dal.repositories.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * DbUserDetailsService
 * - Charge l'utilisateur par email.
 * - Normalise le rôle DB (user / ADMIN / ROLE_ADMIN / enum...) en "USER" / "ADMIN".
 * - PRÉFIXE en mémoire => "ROLE_USER" / "ROLE_ADMIN" (compatible hasRole("ADMIN")).
 */
@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public DbUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String lookup = (email == null) ? null : email.trim();
        var u = users.findByEmail(lookup)
                .orElseThrow(() -> new UsernameNotFoundException("No user " + email));

        String norm = normalizeRole(u.getRole()); // "USER" ou "ADMIN"

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + norm));

        return User.withUsername(u.getEmail())
                .password(u.getPassword())   // hashé (BCrypt) en DB
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * Normalise le rôle DB (enum ou String) en "USER" / "ADMIN".
     * - accepte "user", "USER", "ROLE_USER", enum USER, etc. -> "USER"
     * - accepte "ADMIN", "role_admin", "ROLE_ADMIN", enum ADMIN, etc. -> "ADMIN"
     * - tout le reste -> "USER"
     */
    private String normalizeRole(Object rawRole) {
        if (rawRole == null) return "USER";

        String r = (rawRole instanceof Enum<?> e) ? e.name() : rawRole.toString();
        r = r.trim().toUpperCase(Locale.ROOT);
        if (r.startsWith("ROLE_")) r = r.substring(5); // "ROLE_ADMIN" -> "ADMIN"

        // if/else (évite le warning "switch avec trop peu de cases")
        if ("ADMIN".equals(r)) {
            return "ADMIN";
        } else {
            return "USER";
        }
    }
}
