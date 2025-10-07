<#-- templates/macros.ftlh
     Layout + utilitaires (Bootstrap + theme.css) avec :
     - SEO/OG/Twitter : title, description, image, canonical
     - Favicon
     - Toasts Bootstrap auto (flash attributes: toast / error)
     - Détection robuste du path + auth/roles
-->
<#-- @ftlvariable name="request" type="jakarta.servlet.http.HttpServletRequest" -->
<#-- @ftlvariable name="springMacroRequestContext" type="org.springframework.web.servlet.support.RequestContext" -->


<#macro page title="Shop" description="" image="" canonical="">
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"/>
        <title>${title}</title>

        <#-- =======================
             SEO de base
        ======================== -->
        <meta name="description" content="${(description!'')?string}"/>

        <#-- Détection d'URL pour canonical/og:url si possible -->
        <#assign _reqUrl = ''>
        <#if request?? && request.requestURL??>
        <#-- StringBuffer -> String via l'API native -->
            <#assign _reqUrl = (request.requestURL?api.toString())!''>
        <#elseif springMacroRequestContext?? && springMacroRequestContext.requestUri??>
            <#assign _reqUrl = ''>
        </#if>

        <#-- Canonical : si canonical fourni, on l'utilise tel quel (chemin absolu recommandé),
            sinon on met l'URL de la requête si on l'a. -->
        <#assign _canonical = (canonical?has_content)?then(canonical, _reqUrl)>
        <#if _canonical?has_content>
            <link rel="canonical" href="${_canonical}"/>
        </#if>

        <#-- =======================
             Open Graph / Twitter
        ======================== -->
        <meta property="og:type" content="website"/>
        <meta property="og:title" content="${title}"/>
        <meta property="og:description" content="${(description!'')?string}"/>
        <meta property="og:image" content="${(image?has_content)?then(image, '/images/og-default.png')}"/>
        <#if _canonical?has_content>
            <meta property="og:url" content="${_canonical}"/>
        </#if>

        <meta name="twitter:card" content="summary_large_image"/>
        <meta name="twitter:title" content="${title}"/>
        <meta name="twitter:description" content="${(description!'')?string}"/>
        <meta name="twitter:image" content="${(image?has_content)?then(image, '/images/og-default.png')}"/>

        <#-- =======================
             Favicon + assets (context-path safe)
        ======================== -->
        <#assign _ctx = (request.contextPath)!((springMacroRequestContext.contextPath)!'')>

        <link rel="icon" href="${_ctx}/favicon.ico" sizes="any">
        <link rel="apple-touch-icon" href="${(_ctx)!''}/images/og-default.png">

        <!-- Bootstrap + thème custom -->
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
        <link href="${_ctx}/css/theme.css" rel="stylesheet">
    </head>
    <body>

    <#-- ========= Détection connexion & rôle ========= -->
    <#assign effIsAuth   = (isAuth??  && isAuth)?then(true,false)>
    <#assign effUsername = (username!)>
    <#assign effIsAdmin  = (isAdmin?? && isAdmin)?then(true,false)>

    <#if !effIsAuth || !effIsAdmin>
        <#assign SEC  = SPRING_SECURITY_CONTEXT!>
        <#assign auth = (SEC.authentication)!>
        <#if !effIsAuth && auth??>
            <#assign nm = (auth.name)!>
            <#assign effIsAuth = nm?has_content && (nm != 'anonymousUser')>
            <#if effIsAuth && !effUsername?has_content>
                <#assign effUsername = nm>
            </#if>
        </#if>
        <#if !effIsAdmin && effIsAuth && auth??>
            <#list (auth.authorities![]) as a>
                <#assign roleStr = (a.authority?string)!>
                <#if roleStr == 'ROLE_ADMIN' || roleStr == 'ADMIN'>
                    <#assign effIsAdmin = true>
                </#if>
            </#list>
        </#if>
    </#if>

    <#-- ========= Lien actif : path sécurisé ========= -->
    <#assign path = ''>
    <#if request?? && (request.requestURI)??>
        <#assign path = (request.requestURI)!''>
    <#elseif springMacroRequestContext?? && (springMacroRequestContext.requestUri)??>
        <#assign path = (springMacroRequestContext.requestUri)!''>
    </#if>

    <!-- ======= NAVBAR ======= -->
    <nav class="navbar navbar-expand-md navbar-dark navbar-glass">
        <div class="container">
            <a class="navbar-brand text-gradient fw-semibold" href="${_ctx}/products">Shop</a>

            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav"
                    aria-controls="mainNav" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>

            <div id="mainNav" class="collapse navbar-collapse">
                <ul class="navbar-nav me-auto mb-2 mb-md-0">
                    <li class="nav-item">
                        <a class="nav-link${(path?starts_with('/products'))?then(' active','')}" href="${_ctx}/products">Products</a>
                    </li>

                    <li class="nav-item">
                        <a class="nav-link${(path?starts_with('/cart'))?then(' active','')}" href="${_ctx}/cart">
                            Cart
                            <span class="badge text-bg-primary rounded-pill ms-1">${cartItemCount!0}</span>
                        </a>
                    </li>

                    <#if effIsAdmin>
                        <li class="nav-item">
                            <a class="nav-link${(path?starts_with('/admin'))?then(' active','')}" href="${_ctx}/admin/products">Admin</a>
                        </li>
                    </#if>
                </ul>

                <div class="d-flex gap-2">
                    <#if effIsAuth>
                        <form method="post" action="${_ctx}/auth/logout" class="d-inline">
                            <#if _csrf??>
                                <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
                            </#if>
                            <button class="btn btn-primary btn-sm" type="submit">
                                Logout<#if effUsername?has_content> (${effUsername})</#if>
                            </button>
                        </form>

                    <#-- Bouton suppression de compte (self-service) -->
                        <form method="post" action="${_ctx}/account/delete" class="d-inline"
                              onsubmit="return confirm('Are you sure you want to permanently delete your account? This cannot be undone.');">
                            <#if _csrf??>
                                <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
                            </#if>
                            <button class="btn btn-outline-danger btn-sm" type="submit">Delete account</button>
                        </form>
                    <#else>
                        <a class="btn btn-primary btn-sm" href="${_ctx}/auth/login">Login</a>
                        <a class="btn btn-primary btn-sm" href="${_ctx}/auth/register">Register</a>
                    </#if>
                </div>
            </div>
        </div>
    </nav>

    <!-- ======= CONTENU ======= -->
    <main class="container py-4">
        <#nested>
    </main>

    <!-- ======= FOOTER ======= -->
    <footer class="border-top mt-5">
        <div class="container py-4">
            <div class="row gy-3 align-items-center">
                <div class="col-12 col-md">
                    <span class="text-muted small">
                        © ${.now?string('yyyy')} Shop. All rights reserved.
                    </span>
                </div>

                <div class="col-12 col-md-auto">
                    <ul class="nav justify-content-start justify-content-md-end small">
                        <li class="nav-item">
                            <a class="nav-link px-2 text-muted" href="${_ctx}/products">Products</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link px-2 text-muted" href="${_ctx}/cart">Cart</a>
                        </li>
                        <#-- Lien Admin visible seulement si rôle ADMIN -->
                        <#if effIsAdmin>
                            <li class="nav-item">
                                <a class="nav-link px-2 text-muted" href="${_ctx}/admin/products">Admin</a>
                            </li>
                        </#if>
                    </ul>
                </div>
            </div>
        </div>
    </footer>


    <!-- ======= TOASTS (flash) ======= -->
    <#-- Utilise flash attr "toast" (success/info) et "error" (erreur).
         Exemple côté contrôleur:
           ra.addFlashAttribute("toast", "✅ Added to cart");
           ra.addFlashAttribute("error", "❌ Oups...");
    -->
    <div class="toast-container position-fixed top-0 end-0 p-3" style="z-index:1080">
        <#if toast??>
            <div id="toastSuccess" class="toast" role="status" aria-live="polite" aria-atomic="true"
                 data-bs-delay="2400">
                <div class="toast-header">
                    <strong class="me-auto">Info</strong>
                    <small>Now</small>
                    <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">${toast?string}</div>
            </div>
        </#if>

        <#if error??>
            <div id="toastError" class="toast text-bg-danger border-0" role="alert" aria-live="assertive" aria-atomic="true"
                 data-bs-delay="3200">
                <div class="toast-header">
                    <strong class="me-auto">Error</strong>
                    <small>Now</small>
                    <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">${error?string}</div>
            </div>
        </#if>
    </div>

    <!-- Bootstrap Bundle JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        // Affiche automatiquement les toasts si présents dans le modèle
        (function () {
            var s = document.getElementById('toastSuccess');
            if (s) new bootstrap.Toast(s).show();
            var e = document.getElementById('toastError');
            if (e) new bootstrap.Toast(e).show();
        })();
    </script>
    </body>
    </html>
</#macro>

<#-- Flash messages "inline" (toujours dispo si besoin) -->
<#macro flashMessage type="info" msg="">
    <#local bsClass =
    (type == "success")?then("alert-success",
    (type == "error")?then("alert-danger", "alert-info"))>
    <div class="alert ${bsClass} mb-3" role="alert">${msg}</div>
</#macro>

<#-- Pagination Bootstrap -->
<#macro pagination page url params={}>
    <#assign pairs = []>
    <#list params?keys as k>
        <#assign v = params[k]!>
        <#if v?has_content>
            <#assign pairs += [k?url + "=" + (v?string?url)]>
        </#if>
    </#list>
    <#assign base = url + (pairs?size > 0)?then("?" + pairs?join("&"), "")>
    <#assign sep  = (pairs?size > 0)?then("&","?")>

    <#assign totalPages = (page?? && (page.totalPages)??)?then(page.totalPages, 0)>
    <#assign currNumber = (page?? && (page.number)??)?then(page.number, 0)>
    <#assign pageSize   = (page?? && (page.size)??)?then(page.size, 12)>

    <#if (totalPages > 1)>
        <nav class="mt-4 d-flex justify-content-center" aria-label="Catalogue pagination">
            <ul class="pagination mb-0">
                <#list 0..(totalPages-1) as i>
                    <li class="page-item <#if i==currNumber>active</#if>">
                        <a class="page-link" href="${base}${sep}page=${i}&size=${pageSize?string}">${i+1}</a>
                    </li>
                </#list>
            </ul>
        </nav>
    </#if>
</#macro>
