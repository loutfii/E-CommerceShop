<#-- templates/checkout/success.ftlh -->
<#import "../macros.ftlh" as ui>

<@ui.page title="Payment successful">
    <div class="container py-5">
        <div class="row">
            <div class="col-md-6 mx-auto text-center">
                <div class="card shadow">
                    <div class="card-body py-5">
                        <h1 class="mt-4 mb-3">Payment successful!</h1>
                        <p class="lead">Thank you for your purchase. We are finalizing your order.</p>
                        <p class="text-muted">A confirmation email will follow.</p>
                        <a href="/products" class="btn btn-primary mt-4">Continue shopping</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</@ui.page>
