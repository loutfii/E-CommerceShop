package com.example.ecommerce.pl;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;

/**
 * DevExceptionFilter (profil DEV)
 * -------------------------------
 * Attrape toute exception au niveau Servlet/Filter.
 * Si la réponse n'est pas encore commit, écrit une trace text/plain détaillée.
 */
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class DevExceptionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (Throwable ex) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;

            if (!resp.isCommitted()) {
                resp.reset();
                resp.setStatus(500);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write(buildDiagnostic(ex, req));
                resp.flushBuffer();
            } else {
                // Trop tard pour écrire une réponse propre : on relance pour logging container
                throw ex instanceof ServletException se ? se : new ServletException(ex);
            }
        }
    }

    private String buildDiagnostic(Throwable ex, HttpServletRequest req) {
        StringWriter sw = new StringWriter(4096);
        ex.printStackTrace(new PrintWriter(sw));

        // Chaîne simple : plus de warning "StringBuilder peut être remplacé"
        return ">>> DEV FILTER TRACE <<<\n\n" +
                "time:   " + OffsetDateTime.now() + '\n' +
                "method: " + req.getMethod() + '\n' +
                "uri:    " + req.getRequestURI() + '\n' +
                "query:  " + (req.getQueryString() == null ? "" : req.getQueryString()) + "\n\n" +
                sw;
    }
}
