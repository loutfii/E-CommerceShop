package com.example.ecommerce.bll.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service Stripe (Checkout Sessions)
 * - Initialise la clé secrète au démarrage
 * - Crée des sessions de paiement hébergées par Stripe
 * - Fabrique des LineItems à partir de tes produits (prix en centimes)
 */
@Service
public class StripeService {

    // Correspond à 'stripe.secret-key' dans application.yml
    @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key manquante (STRIPE_SECRET_KEY).");
        }
        com.stripe.Stripe.apiKey = secretKey;
    }

    /**
     * Crée une session Checkout hébergée par Stripe et renvoie l'objet Session.
     * Tu redirigeras ensuite l'utilisateur vers session.getUrl()
     */
    public Session createCheckoutSession(List<SessionCreateParams.LineItem> lineItems,
                                         String successUrl,
                                         String cancelUrl) throws StripeException {

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addAllLineItem(lineItems)
                .build();

        return Session.create(params);
    }

    /**
     * Construit un LineItem (Stripe attend des centimes)
     */
    public SessionCreateParams.LineItem createLineItem(String productName,
                                                       Long priceInCents,
                                                       Long quantity) {
        return SessionCreateParams.LineItem.builder()
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(priceInCents) // ex: 1299 = 12,99€
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(productName)
                                                .build()
                                )
                                .build()
                )
                .setQuantity(quantity)
                .build();
    }
}
