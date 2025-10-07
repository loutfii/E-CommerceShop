<#-- templates/checkout/cancel.ftlh -->
<#import "../macros.ftlh" as ui>

<@ui.page title="Payment canceled">
    <div class="container py-5">
        <div class="row">
            <div class="col-md-6 mx-auto text-center">
                <div class="card shadow">
                    <div class="card-body py-5">
                        <h1 class="mt-4 mb-3">Payment canceled</h1>
                        <p class="lead">Your payment was canceled. Your items are still in the cart.</p>
                        <div class="mt-4">
                            <a href="/checkout" class="btn btn-success me-2">Try again</a>
                            <a href="/cart" class="btn btn-outline-secondary">Back to cart</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</@ui.page>
