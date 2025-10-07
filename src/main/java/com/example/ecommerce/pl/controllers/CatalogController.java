package com.example.ecommerce.pl.controllers;

import com.example.ecommerce.il.interfaces.CategoryService;
import com.example.ecommerce.il.interfaces.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Product catalog controller.
 * Class-level @RequestMapping("/products") ensures that:
 *  - GET /products        -> index()
 *  - GET /products/{id}   -> detail()
 * This avoids any ambiguity with static resource handler for "/**".
 */
@Controller
@RequestMapping("/products")
public class CatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public CatalogController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    /** GET /products : list + filters + pagination */
    @GetMapping
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String min,
                        @RequestParam(required = false) String max,
                        @RequestParam(required = false) String categoryId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "12") int size,
                        Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        BigDecimal minVal = parseDecimal(min);
        BigDecimal maxVal = parseDecimal(max);
        Long catId = parseLong(categoryId);

        var pageData = productService.findPage(q, minVal, maxVal, catId, pageable);

        model.addAttribute("page", pageData);
        model.addAttribute("q", q);
        model.addAttribute("min", minVal);
        model.addAttribute("max", maxVal);
        model.addAttribute("categoryId", catId);
        model.addAttribute("categories", categoryService.findAll());
        return "products/index";
    }

    /** GET /products/{id} : product detail */
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        var dto = productService.findDetail(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        model.addAttribute("p", dto);
        return "products/detail";
    }

    /* -------- helpers -------- */

    private BigDecimal parseDecimal(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        s = s.replace(',', '.');
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }

    private Long parseLong(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        try { return Long.valueOf(s); } catch (NumberFormatException e) { return null; }
    }
}
