<#-- templates/auth/login.ftlh
     .is-invalid + .invalid-feedback quand "error" est présent (fallback générique).
-->
<#import "../macros.ftlh" as ui>

<@ui.page title="Login">
    <div class="row justify-content-center">
        <div class="col-12 col-md-6 col-lg-5">
            <h2 class="mb-3">Login</h2>

            <#if error??>
                <div class="alert alert-danger" role="alert" aria-live="polite">
                    ${error}
                </div>
            </#if>
            <#if message??>
                <div class="alert alert-info" role="status" aria-live="polite">
                    ${message}
                </div>
            </#if>

            <#-- Si "error" est présent, on marque les champs invalides visuellement -->
            <#assign hasErr = error??>

            <form method="post" action="/auth/login" novalidate class="vstack gap-3">
                <#if _csrf??>
                    <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
                </#if>

                <div>
                    <label for="username" class="form-label">Email</label>
                    <input id="username"
                           class="form-control<#if hasErr> is-invalid</#if>"
                           type="email"
                           name="username"
                           required
                           autocomplete="email"
                           inputmode="email"
                           aria-required="true"/>
                    <#if hasErr>
                        <div class="invalid-feedback">Please check your email.</div>
                    </#if>
                </div>

                <div>
                    <label for="password" class="form-label">Password</label>
                    <input id="password"
                           class="form-control<#if hasErr> is-invalid</#if>"
                           type="password"
                           name="password"
                           required
                           autocomplete="current-password"
                           aria-required="true"/>
                    <#if hasErr>
                        <div class="invalid-feedback">Please check your password.</div>
                    </#if>
                </div>

                <button class="btn btn-primary w-100" type="submit">Login</button>
            </form>

            <p class="mt-3 text-center">
                No account yet?
                <a href="/auth/register" class="link-light">Register</a>
            </p>
        </div>
    </div>
</@ui.page>
