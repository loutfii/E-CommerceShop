// src/main/java/com/example/ecommerce/pl/security/SecurityConfig.java
package com.example.ecommerce.pl.security;

import com.example.ecommerce.il.interfaces.CartService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Encoder utilisé par AuthServiceImpl (injection requise). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /** Fournit explicitement le success handler (pas de @Component sur la classe). */
    @Bean
    public CartMergeOnLoginSuccessHandler cartMergeOnLoginSuccessHandler(CartService cartService) {
        return new CartMergeOnLoginSuccessHandler(cartService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CartMergeOnLoginSuccessHandler loginSuccessHandler
    ) throws Exception {

        http
                // CSRF laissé activé (formulaires HTML -> OK, tu as déjà les champs CSRF dans tes vues)
                .authorizeHttpRequests(auth -> auth
                        // public
                        .requestMatchers(
                                "/", "/products", "/products/**",
                                "/cart", "/cart/**",      // invité: panier en session
                                "/auth/**",
                                "/css/**", "/images/**", "/js/**", "/webjars/**",
                                "/uploads/**"
                        ).permitAll()
                        // admin
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // le reste
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .successHandler(loginSuccessHandler)   // fusion panier invité -> DB
                        .permitAll()
                )
                .logout(log -> log
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/products")
                );

        return http.build();
    }
}
