<#-- templates/cart/view.ftlh
     Cart view — Bootstrap + theme.css
     - Records disponibles: l.productName(), l.quantity(), l.unitPrice(), l.lineTotal(), l.productId()
     - Sécurise l'accès quand cart ou lines sont absents
     - CSRF présent pour les POST /cart/update et /cart/remove
-->
<#import "../macros.ftlh" as ui>

<@ui.page title="Cart">
    <div class="d-flex align-items-center justify-content-between mb-3">
        <h2 class="m-0">Your Cart</h2>
    </div>

<#-- 1) Protéger l'accès à cart/lines -->
    <#assign rawLines = (cart??)?then(cart.lines(), [])>
    <#assign lines = (rawLines?has_content)?then(rawLines, [])>

    <#if (lines?size > 0)>
        <div class="card mb-3">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-striped table-hover table-sm align-middle mb-0">
                        <thead>
                        <tr>
                            <th scope="col">Product</th>
                            <th scope="col" class="text-end">Qty</th>
                            <th scope="col" class="text-end">Unit</th>
                            <th scope="col" class="text-end">Total</th>
                            <th scope="col" class="text-center">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list lines as l>
                            <tr>
                                <td>${(l.productName())?string}</td>

                                <td class="text-end">
                                    ${(l.quantity()??)?then(l.quantity()?string, '0')}
                                </td>

                                <td class="text-end">
                                    ${(l.unitPrice()??)?then(l.unitPrice()?string["0.00"], "0.00")} €
                                </td>

                                <td class="text-end fw-semibold">
                                    ${(l.lineTotal()??)?then(l.lineTotal()?string["0.00"], "0.00")} €
                                </td>

                                <td class="text-center">
                                    <div class="d-inline-flex gap-1 align-items-center">

                                        <#-- Incr/Decr : POST /cart/update avec op=incr|decr -->
                                        <form method="post" action="/cart/update" class="d-inline">
                                            <#if _csrf??>
                                                <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
                                            </#if>
                                            <input type="hidden" name="productId" value="${(l.productId())?c}"/>

                                            <button type="submit" name="op" value="decr"
                                                    class="btn btn-outline-secondary btn-sm"
                                                    aria-label="Decrease quantity">−</button>

                                            <button type="submit" name="op" value="incr"
                                                    class="btn btn-outline-secondary btn-sm"
                                                    aria-label="Increase quantity">+</button>
                                        </form>

                                        <#-- Remove : POST /cart/remove (évite l'ambiguïté avec /cart/update?op=remove) -->
                                        <form method="post" action="/cart/remove" class="d-inline ms-1">
                                            <#if _csrf??>
                                                <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
                                            </#if>
                                            <input type="hidden" name="productId" value="${(l.productId())?c}"/>

                                            <button type="submit"
                                                    class="btn btn-danger btn-sm"
                                                    aria-label="Remove item">Remove</button>
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

        <div class="d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-2">
            <div class="text-muted">
                <strong>Total items:</strong>
                ${(cart?? && cart.totalItems()??)?then(cart.totalItems()?string, '0')}
                &nbsp;—&nbsp;
                <strong>Total amount:</strong>
                ${(cart?? && cart.totalAmount()??)?then(cart.totalAmount()?string["0.00"], '0.00')} €
            </div>

            <div class="d-flex gap-2">
                <a class="btn btn-outline-secondary" href="/products">Continue shopping</a>
                <a class="btn btn-primary" href="/checkout">Checkout</a>
            </div>
        </div>

    <#else>
        <div class="alert alert-info" role="alert">
            Cart is empty.
        </div>
        <a class="btn btn-primary" href="/products">← Continue shopping</a>
    </#if>
</@ui.page>
