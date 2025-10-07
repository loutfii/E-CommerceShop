<#-- templates/auth/register.ftlh
     .is-invalid + .invalid-feedback quand "error" est présent (ex: passwords mismatch).
-->
<#import "../macros.ftlh" as ui>

<@ui.page title="Register">
    <div class="row justify-content-center">
        <div class="col-12 col-md-6 col-lg-5">
            <h2 class="mb-3">Create your account</h2>

            <#if error??>
                <div id="form-error" class="alert alert-danger" role="alert" aria-live="polite">
                    ${error}
                </div>
            </#if>

            <#assign hasErr = error??>

            <form method="post" action="/auth/register" novalidate class="vstack gap-3">
                <#if _csrf??>
                    <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
                </#if>

                <div>
                    <label for="email" class="form-label">Email</label>
                    <input id="email"
                           class="form-control"
                           type="email"
                           name="email"
                           required
                           autocomplete="email"
                           aria-required="true"/>
                </div>

                <div>
                    <label for="password" class="form-label">Password</label>
                    <input id="password"
                           class="form-control<#if hasErr> is-invalid</#if>"
                           type="password"
                           name="password"
                           required
                           minlength="8"
                           autocomplete="new-password"
                           aria-required="true"/>
                    <#if hasErr>
                        <div class="invalid-feedback">Passwords must match and be at least 8 characters.</div>
                    </#if>
                    <div class="form-text">Minimum 8 characters.</div>
                </div>

                <div>
                    <label for="confirm" class="form-label">Confirm password</label>
                    <input id="confirm"
                           class="form-control<#if hasErr> is-invalid</#if>"
                           type="password"
                           name="confirm"
                           required
                           minlength="8"
                           autocomplete="new-password"
                           aria-required="true"/>
                    <#if hasErr>
                        <div class="invalid-feedback">Please confirm the same password.</div>
                    </#if>
                </div>

                <button class="btn btn-primary w-100" type="submit">Create account</button>
            </form>

            <p class="mt-3 text-center">
                Already have an account?
                <a href="/auth/login" class="link-light">Login</a>
            </p>
        </div>
    </div>
</@ui.page>
