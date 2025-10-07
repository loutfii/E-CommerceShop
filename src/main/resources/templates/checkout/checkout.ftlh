<#-- templates/checkout/checkout.ftlh -->
<#import "../macros.ftlh" as ui>

<@ui.page title="Checkout">
    <div class="container py-5">
        <div class="row">
            <div class="col-md-8 mx-auto">
                <h1 class="mb-4">Order summary</h1>

                <#-- État panier vide vs items -->
                <#if cartEmpty?? && cartEmpty>
                    <div class="alert alert-info">Your cart is empty.</div>
                    <a href="/cart" class="btn btn-outline-secondary">Back to cart</a>
                <#else>
                    <div class="card mb-4">
                        <div class="card-body">

                            <#-- Liste des lignes -->
                            <#list items as it>
                                <div class="d-flex justify-content-between align-items-center mb-3 pb-3 border-bottom">
                                    <div>
                                        <h6 class="mb-0">${it.name()?string}</h6>
                                        <small class="text-muted">Quantity: ${it.quantity()?string}</small><br/>
                                        <small class="text-muted">Unit price: ${it.unitPrice()?string["0.00"]} €</small>
                                    </div>
                                    <strong>${it.lineTotal()?string["0.00"]} €</strong>
                                </div>
                            </#list>

                            <div class="d-flex justify-content-between align-items-center mt-4 pt-3 border-top">
                                <h5 class="mb-0">Total</h5>
                                <h4 class="mb-0 text-primary">${total?string["0.00"]} €</h4>
                            </div>

                            <div class="text-muted small mt-2">Taxes and shipping are calculated at the next step.</div>
                        </div>
                    </div>

                    <form action="/create-checkout-session" method="post" class="vstack gap-2">
                        <#-- CSRF -->
                        <#if _csrf??>
                            <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
                        </#if>
                        <button type="submit" class="btn btn-success btn-lg w-100">Pay with Stripe</button>
                    </form>

                    <a href="/cart" class="btn btn-link w-100 mt-3">Back to cart</a>
                </#if>

            </div>
        </div>
    </div>
</@ui.page>
