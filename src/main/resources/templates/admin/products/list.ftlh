<#-- templates/admin/products/list.ftlh
     Liste admin avec vignettes (thumbnail) + actions.
     - N’utilise que : id(), name(), price()
     - Image : tente p.imagePath() puis p.imageUrl(), sinon fallback.
     - Fallback local : /images/placeholder.svg (crée-le si pas déjà fait).
-->
<#import "../../macros.ftlh" as ui>

<@ui.page
title="Admin · Products"
description="Admin area"
canonical="/admin/products"
>
    <meta name="robots" content="noindex,nofollow">

    <div class="d-flex align-items-center justify-content-between mb-3">
        <h2 class="m-0">Admin · Products</h2>
        <a class="btn btn-primary" href="/admin/products/new">+ New product</a>
    </div>

    <#assign itemsSafe = (items![])>

    <#if itemsSafe?size == 0>
        <div class="alert alert-info" role="alert">No products yet.</div>
    <#else>
        <div class="card">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-striped table-hover align-middle mb-0">
                        <thead>
                        <tr>
                            <th scope="col" style="width:90px">Image</th>
                            <th scope="col">Name</th>
                            <th scope="col" class="text-end">Price</th>
                            <th scope="col" class="text-end">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list itemsSafe as p>
                        <#-- Récupère un chemin d’image si dispo : imagePath() puis imageUrl() -->
                            <#attempt>
                                <#assign imgPath = (p.imagePath()?string)!''>
                                <#recover>
                                    <#assign imgPath = ''>
                            </#attempt>
                            <#if !imgPath?has_content>
                                <#attempt>
                                    <#assign imgPath = (p.imageUrl()?string)!''>
                                    <#recover>
                                        <#assign imgPath = ''>
                                </#attempt>
                            </#if>

                            <tr>
                                <td>
                                    <div class="ratio ratio-1x1" style="width:72px;">
                                        <img
                                                src="${(imgPath?has_content)?then(imgPath, '/images/placeholder.svg')}"
                                                alt="${(p.name()!'')?string}"
                                                class="w-100 h-100 object-fit-cover rounded"
                                                loading="lazy"
                                                onerror="this.onerror=null;this.src='/images/placeholder.svg';"
                                        />
                                    </div>
                                </td>

                                <td class="fw-medium">
                                    ${(p.name()!'')?string}
                                </td>

                                <td class="text-end">
                                    ${(p.price()!'0')?string} €
                                </td>

                                <td class="text-end">
                                    <div class="d-inline-flex gap-2">
                                        <a class="btn btn-outline-secondary btn-sm"
                                           href="/admin/products/${(p.id())?string}/edit">Edit</a>

                                        <form method="post"
                                              action="/admin/products/${(p.id())?string}/delete"
                                              class="d-inline"
                                              onsubmit="return confirm('Delete this product?');">
                                            <#if _csrf??>
                                                <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
                                            </#if>
                                            <button class="btn btn-danger btn-sm" type="submit">Delete</button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                        </#list>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </#if>
</@ui.page>
