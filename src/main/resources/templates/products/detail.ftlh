<#-- templates/products/detail.ftlh
     Product detail (Bootstrap + theme.css) — image + fallback + SEO/OG
     - p : record -> p.id(), p.name(), p.price(), p.description(), p.imageUrl()? (ou p.imagePath()? plus tard)
-->
<#import "../macros.ftlh" as ui>

<#-- SEO fields -->
<#assign _title = (p.name())?string>
<#assign _desc  = ((p.description())!'')?string>

<#-- Image for OG: prefer local upload (imagePath or imageUrl), else default -->
<#assign _ogimg = ''>
<#attempt><#assign _ogimg = (p.imagePath()?string)!''><#recover></#attempt>
<#if !_ogimg?has_content>
    <#attempt><#assign _ogimg = (p.imageUrl()?string)!''><#recover></#attempt>
</#if>
<#if !_ogimg?has_content>
    <#assign _ogimg = "/images/og-default.png">
</#if>

<@ui.page
title="${_title}"
description="${_desc}"
image="${_ogimg}"
canonical="/products/${(p.id())?string}"
>
    <div class="row g-4">

        <!-- Colonne description + image -->
        <div class="col-12 col-md-7">
            <#-- Détermine le chemin d'image : imagePath() -> imageUrl() -> placeholder -->
            <#assign img = ''>
            <#attempt><#assign img = (p.imagePath()?string)!''><#recover></#attempt>
            <#if !img?has_content>
                <#attempt><#assign img = (p.imageUrl()?string)!''><#recover></#attempt>
            </#if>

            <div class="ratio ratio-4x3 mb-3">
                <img
                        src="${(img?has_content)?then(img, '/images/placeholder.svg')}"
                        alt="${(p.name()!'')?string}"
                        class="w-100 h-100 object-fit-cover rounded"
                        loading="lazy"
                        onerror="this.onerror=null;this.src='/images/placeholder.svg';"
                />
            </div>

            <h2 class="mb-2">${(p.name())?string}</h2>
            <p class="text-muted">${(p.description()!'')?string}</p>
        </div>

        <!-- Colonne achat -->
        <div class="col-12 col-md-5">
            <div class="card h-100">
                <div class="card-body">
                    <div class="h4 mb-3">${(p.price())?string} €</div>

                    <form method="post" action="/cart/add" class="d-flex align-items-end gap-2">
                        <#-- CSRF si présent -->
                        <#if _csrf??>
                            <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
                        </#if>

                        <input type="hidden" name="productId" value="${(p.id())?string}"/>

                        <div class="flex-grow-1">
                            <label class="form-label" for="qty">Qty</label>
                            <input id="qty"
                                   class="form-control"
                                   type="number"
                                   name="quantity"
                                   value="1"
                                   min="1"
                                   step="1"
                                   inputmode="numeric"/>
                        </div>

                        <button class="btn btn-primary" type="submit">Add to cart</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</@ui.page>
