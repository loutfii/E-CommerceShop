package com.example.ecommerce.pl;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * GlobalBinder
 * ------------
 * Convertit "" -> null pour tous les paramètres String.
 * Utile quand un <select> renvoie value="" (ex. All categories).
 */
@ControllerAdvice
public class GlobalBinder {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // true = convert empty String to null
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
