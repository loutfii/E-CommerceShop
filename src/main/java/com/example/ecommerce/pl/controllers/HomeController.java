package com.example.ecommerce.pl.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Redirect root to /products to avoid ambiguity on "/" mapping. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/products";
    }
}
