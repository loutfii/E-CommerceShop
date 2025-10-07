package com.example.ecommerce.bll.services.impls;

import com.example.ecommerce.bll.mappers.DtoMapper;
import com.example.ecommerce.dal.repositories.CategoryRepository;
import com.example.ecommerce.dal.repositories.ProductRepository;
import com.example.ecommerce.dl.entities.Product;
import com.example.ecommerce.il.dto.ProductDetailDto;
import com.example.ecommerce.il.dto.ProductListItemDto;
import com.example.ecommerce.il.interfaces.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ProductServiceImpl
 * ------------------
 * - Catalogue paginé + filtres via Specifications.
 * - Détail avec fetch-join (repository) pour éviter les LazyInitializationException.
 * - Admin CRUD avec @PreAuthorize.
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductServiceImpl(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductListItemDto> findPage(String q, BigDecimal min, BigDecimal max, Long categoryId, Pageable pageable) {
        List<Specification<Product>> specs = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            specs.add((root, cq, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (min != null) {
            specs.add((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min));
        }
        if (max != null) {
            specs.add((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("price"), max));
        }
        if (categoryId != null) {
            specs.add((root, cq, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }

        Specification<Product> spec = specs.stream().reduce(Specification::and).orElse(null);

        Page<Product> page = (spec == null) ? products.findAll(pageable) : products.findAll(spec, pageable);
        return page.map(DtoMapper::toProductListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDetailDto> findDetail(Long productId) {
        // On récupère le produit en fetch-join → la catégorie est déjà initialisée si elle existe
        return products.findByIdWithCategory(productId)
                .map(DtoMapper::toProductDetail);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Long create(String name, String description, BigDecimal price, Integer stock, Long categoryId, String imageUrl) {
        validateProduct(name, price, stock);

        Product p = new Product();
        p.setName(name.trim());
        p.setDescription(description);
        p.setPrice(price);
        p.setStock(stock != null ? stock : 0);
        p.setImageUrl(imageUrl);

        if (categoryId != null) {
            categories.findById(categoryId).ifPresent(p::setCategory);
        }

        products.save(p);
        return p.getId();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void update(Long id, String name, String description, BigDecimal price, Integer stock, Long categoryId, String imageUrl) {
        validateProduct(name, price, stock);

        Product p = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        p.setName(name.trim());
        p.setDescription(description);
        p.setPrice(price);
        p.setStock(stock != null ? stock : 0);
        p.setImageUrl(imageUrl);

        if (categoryId != null) {
            p.setCategory(categories.findById(categoryId).orElse(null));
        } else {
            p.setCategory(null);
        }
        // dirty checking JPA => pas de save() nécessaire
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        if (!products.existsById(id)) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        products.deleteById(id);
    }

    // ... imports et annotations identiques à ta classe actuelle

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.Optional<Long> findCategoryIdOfProduct(Long productId) {
        // on réutilise le fetch-join pour éviter le lazy
        return products.findByIdWithCategory(productId)
                .map(p -> p.getCategory() != null ? p.getCategory().getId() : null);
    }


    private static void validateProduct(String name, BigDecimal price, Integer stock) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Price must be >= 0");
        if (stock == null || stock < 0) throw new IllegalArgumentException("Stock must be >= 0");
    }
}
