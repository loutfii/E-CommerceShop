// src/main/java/com/example/ecommerce/pl/controllers/AdminProductController.java
package com.example.ecommerce.pl.controllers;

import com.example.ecommerce.il.interfaces.CategoryService;
import com.example.ecommerce.il.interfaces.ProductService;
import com.example.ecommerce.bll.services.ImageStorageService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ImageStorageService imageStorageService;

    public AdminProductController(ProductService productService,
                                  CategoryService categoryService,
                                  ImageStorageService imageStorageService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items",
                productService.findPage(null, null, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 500)).getContent());
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/products/form";
    }

    /**
     * Création d’un produit avec upload d’image local (optionnel).
     * - Le formulaire doit avoir enctype="multipart/form-data"
     * - Champ fichier : name="imageFile"
     *
     * NOTE: 'description' est optionnel → required=false + defaultValue=""
     * pour éviter le 400 quand le champ est laissé vide (surtout si un StringTrimmerEditor convertit "" → null).
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public String create(@RequestParam @NotBlank String name,
                         @RequestParam(required = false, defaultValue = "") String description, // <-- rendu optionnel
                         @RequestParam @DecimalMin("0.00") BigDecimal price,
                         @RequestParam @Min(0) Integer stock,
                         @RequestParam(required = false) Long categoryId,
                         @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                         RedirectAttributes ra) {
        try {
            // 1) Upload de l’image si fournie → chemin public /uploads/xxx.ext
            String imagePath = null;
            if (imageFile != null && !imageFile.isEmpty()) {
                imagePath = imageStorageService.saveImage(imageFile, null);
            }

            // 2) Création du produit (on passe le chemin public au paramètre imageUrl)
            productService.create(name, description, price, stock, categoryId, imagePath);

            ra.addFlashAttribute("toast", "✅ Produit créé avec succès");
            return "redirect:/admin/products";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Erreur lors de la création : " + e.getMessage());
            return "redirect:/admin/products/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        var detail = productService.findDetail(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        model.addAttribute("p", detail);
        model.addAttribute("categories", categoryService.findAll());

        Long selectedCategoryId = productService.findCategoryIdOfProduct(id).orElse(null);
        model.addAttribute("selectedCategoryId", selectedCategoryId);

        return "admin/products/form";
    }

    /**
     * Mise à jour d’un produit avec gestion complète de l’image :
     * - Nouveau fichier => upload + suppression de l’ancienne
     * - Aucun nouveau fichier => on garde l’existante (champ hidden)
     * - Case “supprimer l’image” => on supprime l’ancienne et on met à null
     *
     * Le formulaire d’édition envoie :
     * - name="imageFile" (MultipartFile, optionnel)
     * - name="existingImagePath" (hidden, chemin actuel si présent)
     * - name="removeImage" (checkbox, optionnelle)
     *
     * NOTE: 'description' est optionnel → required=false + defaultValue=""
     * pour éviter le 400 si l’admin laisse la description vide.
     */
    @PostMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false, defaultValue = "") String description, // <-- rendu optionnel
                         @RequestParam BigDecimal price,
                         @RequestParam Integer stock,
                         @RequestParam(required = false) Long categoryId,
                         @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                         @RequestParam(name = "existingImagePath", required = false) String existingImagePath,
                         @RequestParam(name = "removeImage", required = false) Boolean removeImage,
                         RedirectAttributes ra) {

        try {
            String imagePathToSave = existingImagePath; // par défaut on conserve

            if (Boolean.TRUE.equals(removeImage)) {
                if (existingImagePath != null && !existingImagePath.isBlank()) {
                    imageStorageService.deleteIfOwned(existingImagePath);
                }
                imagePathToSave = null;
            } else if (imageFile != null && !imageFile.isEmpty()) {
                imagePathToSave = imageStorageService.saveImage(imageFile, existingImagePath);
            }

            productService.update(id, name, description, price, stock, categoryId, imagePathToSave);

            ra.addFlashAttribute("toast", "✅ Produit mis à jour");
            return "redirect:/admin/products";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Erreur lors de la mise à jour : " + e.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            // Supprime physiquement l'image si elle est sous /uploads/
            var detail = productService.findDetail(id);
            detail.ifPresent(d -> {
                String img = d.imageUrl();  // ✅ ton DTO expose imageUrl()
                if (img != null && !img.isBlank()) {
                    imageStorageService.deleteIfOwned(img);
                }
            });

            productService.delete(id);
            ra.addFlashAttribute("toast", "🗑️ Produit supprimé");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Erreur lors de la suppression : " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
}
