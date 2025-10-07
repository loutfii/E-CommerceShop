package com.example.ecommerce.pl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DevExceptionAdvice
 * ------------------
 * PROFIL: DEV uniquement (ne sera activé que si le profil "dev" est actif).
 *
 * RÔLE:
 *  - Sert d'attrape-tout "global" pour les exceptions dans la chaîne MVC
 *    (contrôleurs, binding, rendu FreeMarker).
 *  - Retourne un diagnostic lisible en "text/plain" directement dans le navigateur
 *    pendant le développement (au lieu d'une Whitelabel page ou d'une réponse tronquée).
 *
 * POINTS CLÉS:
 *  - Pas de propriété dépréciée "spring.mvc.throw-exception-if-no-handler-found".
 *  - Prend en charge explicitement les 404:
 *      * NoResourceFoundException (ressource statique / URL inexistante)
 *      * NoHandlerFoundException   (aucun mapping controller trouvé)
 *  - Sépare les handlers pour les erreurs de binding/validation (MANV vs BindException).
 *  - Construit un diagnostic détaillé: méthode, URI, query, params, utilisateur,
 *    root cause et stacktrace complète.
 */
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class DevExceptionAdvice {

    /* ==========================================================
     *                   404 / CONTRÔLE DE FLUX
     * ========================================================== */

    /**
     * 404 côté "ressources" (ex: Spring pense que c'est un fichier statique).
     * Certaines versions n'exposent pas de getters utiles sur l'exception,
     * donc on remonte l'URI et le message.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        String body = buildDiagnostic("NOT_FOUND_RESOURCE", ex, req, Map.of(
                "path", req.getRequestURI(),
                "query", String.valueOf(req.getQueryString()),
                "message", String.valueOf(ex.getMessage())
        ));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    /**
     * 404 côté "handlers" (aucune méthode @RequestMapping ne correspond).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<String> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest req) {
        String body = buildDiagnostic("NOT_FOUND_HANDLER", ex, req, Map.of(
                "httpMethod", String.valueOf(ex.getHttpMethod()),
                "path", req.getRequestURI(),
                "query", String.valueOf(req.getQueryString()),
                "message", String.valueOf(ex.getMessage())
        ));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    /**
     * Exceptions de type "je force un statut" (ResponseStatusException).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        String body = buildDiagnostic("RESPONSE_STATUS", ex, req, Map.of(
                "status", ex.getStatusCode().toString(),
                "reason", String.valueOf(ex.getReason())
        ));
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    /* ==========================================================
     *                BINDING / VALIDATION REQUÊTE
     * ========================================================== */

    /**
     * Paramètre mal typé (ex: "abc" pour un Long).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String body = buildDiagnostic("TYPE_MISMATCH", ex, req, Map.of(
                "paramName", String.valueOf(ex.getName()),
                "requiredType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "null",
                "value", String.valueOf(ex.getValue())
        ));
        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    /**
     * Échecs de validation @Valid sur @RequestBody / @ModelAttribute.
     * (Sous-type de BindException dans Spring 6+, on le traite séparément pour le libellé.)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> "- " + fe.getField() + " -> " + fe.getDefaultMessage()
                        + " (reçu: " + fe.getRejectedValue() + ")")
                .collect(Collectors.joining("\n"));

        String body = buildDiagnostic("BINDING_METHOD_ARG_NOT_VALID", ex, req, Map.of("fieldErrors", details));
        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    /**
     * Autres erreurs de binding (DataBinder sur des champs de formulaire par ex.).
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<String> handleBind(BindException ex, HttpServletRequest req) {
        String details = ex.getFieldErrors().stream()
                .map(fe -> "- " + fe.getField() + " -> " + fe.getDefaultMessage()
                        + " (reçu: " + fe.getRejectedValue() + ")")
                .collect(Collectors.joining("\n"));

        String body = buildDiagnostic("BINDING", ex, req, Map.of("fieldErrors", details));
        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    /**
     * Violations Jakarta (@Min, @Email, etc.) déclenchées hors binding.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        String details = ex.getConstraintViolations().stream()
                .map(v -> "- " + v.getPropertyPath() + " -> " + v.getMessage()
                        + " (reçu: " + v.getInvalidValue() + ")")
                .collect(Collectors.joining("\n"));

        String body = buildDiagnostic("CONSTRAINT", ex, req, Map.of("violations", details));
        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    /* ==========================================================
     *                         CATCH-ALL
     * ========================================================== */

    /**
     * Attrape tout le reste (équivalent d'un try/catch global MVC).
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<String> handleAny(Throwable ex, HttpServletRequest req) {
        String body = buildDiagnostic("UNCAUGHT", ex, req, Map.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    /* ==========================================================
     *                        UTILITAIRES
     * ========================================================== */

    /**
     * Construit un rapport lisible: contexte requête, params, infos extra,
     * root cause et stacktrace complète.
     */
    private String buildDiagnostic(String tag, Throwable ex, HttpServletRequest req, Map<String, String> extra) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append(">>> DEV TRACE [").append(tag).append("] <<<\n\n")
                .append("time:        ").append(OffsetDateTime.now()).append('\n')
                .append("method:      ").append(req.getMethod()).append('\n')
                .append("uri:         ").append(req.getRequestURI()).append('\n')
                .append("query:       ").append(nullToEmpty(req.getQueryString())).append('\n')
                .append("remoteAddr:  ").append(req.getRemoteAddr()).append('\n')
                .append("user:        ").append(req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "anonymous").append("\n\n");

        // Paramètres HTTP
        sb.append("params:\n");
        req.getParameterMap().forEach((k, v) ->
                sb.append("  - ").append(k).append(" = ").append(String.join(",", v)).append('\n'));

        // Détails additionnels fournis par le handler
        if (extra != null && !extra.isEmpty()) {
            sb.append("\nextra:\n");
            extra.forEach((k, v) -> sb.append("  - ").append(k).append(": ").append(nullToEmpty(v)).append('\n'));
        }

        // Root cause + stacktrace complète
        Throwable root = rootCause(ex);
        sb.append("\nrootCause: ").append(root.getClass().getName())
                .append(": ").append(nullToEmpty(root.getMessage())).append("\n\n")
                .append("stacktrace:\n").append(stackToString(ex));

        return sb.toString();
    }

    private String stackToString(Throwable t) {
        StringWriter sw = new StringWriter(4096);
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur;
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }
}
