// src/main/java/com/example/ecommerce/pl/config/WebMvcConfig.java
package com.example.ecommerce.pl.config;

import com.example.ecommerce.pl.interceptors.CartCountInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration Web :
 *  - Gère l’intercepteur du badge panier
 *  - Expose le dossier "uploads" pour les images uploadées par l’admin
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CartCountInterceptor cartCountInterceptor;

    // Injection de la propriété définie dans application.yml
    @Value("${app.upload-dir}")
    private String uploadDir;

    public WebMvcConfig(CartCountInterceptor cartCountInterceptor) {
        this.cartCountInterceptor = cartCountInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cartCountInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/uploads/**",
                        "/webjars/**", "/favicon.ico", "/error", "/error/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 🔹 Transforme le chemin défini dans application.yml en chemin absolu
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + uploadPath + "/";

        // 🔹 Expose le dossier local "uploads" à l’URL publique /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600); // Cache navigateur 1h
    }
}
