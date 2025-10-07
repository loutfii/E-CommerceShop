<#-- templates/products/index.ftlh
     Catalogue produits (Bootstrap + theme.css).
     Version avec image produit locale + fallback + lazy-loading.
     - Utilise p.imagePath() OU p.imageUrl() selon dispo.
     - Fallback automatique vers /images/placeholder.svg.
     - Reste 100% compatible avec ton système de filtres/pagination.
-->
<#import "../macros.ftlh" as ui>

<@ui.page
title="Products"
description="Browse our catalog of tech and accessories."
canonical="/products"
>
    <div class="d-flex align-items-center justify-content-between mb-3">
        <h2 class="m-0">Products</h2>
    </div>

    <!-- ================================
         Filtres (inchangés)
    ================================= -->
    <form method="get" action="/products" class="row g-3 align-items-end mb-4" aria-label="Product filters">
        <div class="col-12 col-sm-6 col-md-3">
            <label for="q" class="form-label">Name</label>
            <input id="q" class="form-control" type="text" name="q" value="${(q)!}" placeholder="Search name…"/>
        </div>

        <div class="col-6 col-md-2">
            <label for="min" class="form-label">Min €</label>
            <input id="min" class="form-control"
                   type="number" step="0.01" inputmode="decimal" lang="en"
                   name="min" value="${((min)!)?string}" placeholder="0.00"/>
        </div>

        <div class="col-6 col-md-2">
            <label for="max" class="form-label">Max €</label>
            <input id="max" class="form-control"
                   type="number" step="0.01" inputmode="decimal" lang="en"
                   name="max" value="${((max)!)?string}" placeholder="100.00"/>
        </div>

        <div class="col-12 col-sm-6 col-md-3">
            <label for="categoryId" class="form-label">Category</label>
            <select id="categoryId" class="form-select" name="categoryId">
                <option value="">All categories</option>
                <#list (categories![]) as c>
                    <#assign cid = (c.id())?string>
                    <option value="${cid}" <#if ((categoryId!)?string == cid)>selected</#if>>
                        ${(c.name())?string}
                    </option>
                </#list>
            </select>
        </div>

        <div class="col-12 col-md-2">
            <button class="btn btn-primary w-100" type="submit">Filter</button>
        </div>
    </form>

    <!-- ================================
         Grille produits avec image locale
    ================================= -->
    <div class="row row-cols-1 row-cols-sm-2 row-cols-md-3 g-4">
        <#list (page.content![]) as p>
            <div class="col">
                <div class="card h-100 shadow-sm">

                    <!-- 🔹 Bloc image -->
                    <#assign img = ''>
                    <#attempt><#assign img = (p.imagePath()?string)!''><#recover></#attempt>
                    <#if !img?has_content>
                        <#attempt><#assign img = (p.imageUrl()?string)!''><#recover></#attempt>
                    </#if>

                    <div class="ratio ratio-4x3 bg-light">
                        <img
                                src="${(img?has_content)?then(img, '/images/placeholder.svg')}"
                                alt="${(p.name()!'')?string}"
                                class="w-100 h-100 object-fit-cover rounded-top"
                                loading="lazy"
                                onerror="this.onerror=null;this.src='/images/placeholder.svg';"
                        />
                    </div>

                    <!-- 🔹 Corps de la carte -->
                    <div class="card-body d-flex flex-column">
                        <h5 class="card-title mb-1">${(p.name())?string}</h5>
                        <div class="text-muted fw-semibold mb-3">${(p.price())?string} €</div>

                        <div class="mt-auto d-flex justify-content-between align-items-center gap-2">
                            <a class="btn btn-outline-secondary" href="/products/${(p.id())?string}">Details</a>

                            <form method="post" action="/cart/add" class="d-inline">
                                <#if _csrf??>
                                    <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
                                </#if>
                                <input type="hidden" name="productId" value="${(p.id())?string}"/>
                                <input type="hidden" name="quantity" value="1"/>
                                <button class="btn btn-primary" type="submit">Add to cart</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </#list>
    </div>

    <!-- ================================
         Pagination (inchangée)
    ================================= -->
    <@ui.pagination
    page=page
    url="/products"
    params={
    "q": (q)!,
    "min": ((min)!)?string,
    "max": ((max)!)?string,
    "categoryId": ((categoryId)!)?string
    }
    />
</@ui.page>
