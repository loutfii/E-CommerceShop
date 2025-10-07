<#-- templates/admin/products/form.ftlh
    Formulaire Admin (Create / Edit) — version upload local
    - Envoi multipart (enctype="multipart/form-data")
    - Champ fichier "imageFile" (pas d'URL)
    - Édition : aperçu + hidden existingImagePath + checkbox removeImage
    - On conserve ta logique p comme "record" : p.id(), p.name(), ...
    - selectedCategoryId est calculé côté contrôleur
    - AJOUT : création "à la volée" d'une catégorie via modale (+ New)
    - AJOUT : suppression de la catégorie sélectionnée (bouton Delete sous + New)
    - AJUSTEMENT : fallback image sans attribut onerror inline
-->
<#import "../../macros.ftlh" as ui>

<#assign isEdit = p??>
<#assign title = isEdit?then("Edit · " + (p.name())?string, "New product")>

<@ui.page title="${title}">
    <h2 class="mb-3 text-center">${title}</h2>

<#-- URL d'action selon create ou update -->
    <#assign action = isEdit?then("/admin/products/" + (p.id())?string, "/admin/products")>

<#-- IMPORTANT : enctype multipart pour permettre l'upload de fichier -->
    <form method="post" action="${action}" enctype="multipart/form-data"
                    class="card p-3 shadow-hero mx-auto w-100" style="max-width:560px;">

        <#-- CSRF si présent -->
        <#if _csrf??>
            <input type="hidden" name="${_csrf.parameterName!'_csrf'}" value="${_csrf.token!}"/>
        </#if>

        <!-- Name -->
        <div class="mb-3">
            <label for="name" class="form-label">Name</label>
            <input id="name" class="form-control" type="text" name="name" required
                   value="${isEdit?then((p.name())?string,'')}"/>
        </div>

        <!-- Description -->
        <div class="mb-3">
            <label for="description" class="form-label">Description</label>
            <textarea id="description" class="form-control" name="description" rows="4"
                      placeholder="Optional...">${isEdit?then(((p.description())!'')?string,'')}</textarea>
        </div>

        <!-- Price -->
        <div class="mb-3">
            <label for="price" class="form-label">Price (€)</label>
            <input id="price" class="form-control" type="number" step="0.01" inputmode="decimal" lang="en"
                   name="price" required
                   value="${isEdit?then(((p.price())!'')?string,'')}"/>
        </div>

        <!-- Stock -->
        <div class="mb-3">
            <label for="stock" class="form-label">Stock</label>
            <input id="stock" class="form-control" type="number" min="0" name="stock"
                   value="${isEdit?then(((p.stock())!0)?string,'0')}"/>
        </div>

        <!-- Category (+ New au-dessus, Delete en-dessous) -->
        <div class="mb-3">
            <label for="categoryId" class="form-label">Category</label>

            <div class="d-flex gap-3 align-items-start">
                <!-- Sélecteur de catégorie -->
                <div class="flex-grow-1">
                    <select id="categoryId" class="form-select" name="categoryId">
                        <option value="">(none)</option>
                        <#assign selCid = ((selectedCategoryId!)?string)>
                        <#list (categories![]) as c>
                            <#assign cid = (c.id())?string>
                            <option value="${cid}" <#if selCid?has_content && selCid == cid>selected</#if>>
                                ${(c.name())?string}
                            </option>
                        </#list>
                    </select>
                    <div class="form-text">Need a new category? Click “+ New”. To remove the selected category, use “Delete”.</div>
                </div>

                <!-- Colonne boutons verticale : + New (bleu) puis Delete (rouge) -->
                <div class="d-flex flex-column gap-2">
                    <button type="button" class="btn btn-outline-primary" data-bs-toggle="modal" data-bs-target="#newCategoryModal">
                        + New
                    </button>
                    <button type="button" id="deleteCategoryBtn" class="btn btn-outline-danger">
                        Delete
                    </button>
                </div>
            </div>
        </div>

        <#-- ===========================================
             SECTION IMAGE (Upload local)
             - En création : simple champ fichier.
             - En édition : aperçu + hidden existingImagePath + checkbox "remove".
             - Remarque : on réutilise p.imageUrl() comme "chemin existant" si ton DTO n'a pas encore imagePath().
               Le contrôleur saura gérer une valeur vide.
           =========================================== -->

        <#-- Aperçu et options en ÉDITION -->
        <#if isEdit && p.imageUrl?? && (p.imageUrl())?has_content>
            <div class="mb-2">
                <label class="form-label">Current image</label>
                <div class="ratio ratio-4x3 mb-2" style="max-width: 320px;">
                    <img
                            src="${p.imageUrl()}"
                            alt="${(p.name())?string}"
                            class="w-100 h-100 object-fit-cover rounded"
                            data-fallback-src="/images/placeholder.svg"/>
                </div>

                <#-- On transmet l'image actuelle au contrôleur pour la conserver par défaut -->
                <input type="hidden" name="existingImagePath" value="${(p.imageUrl())?string}">
            </div>

            <div class="form-check my-2">
                <input class="form-check-input" type="checkbox" id="removeImage" name="removeImage">
                <label class="form-check-label" for="removeImage">Remove image</label>
            </div>
        <#elseif isEdit>
        <#-- Pas d'image actuelle : on envoie un hidden vide (facultatif) -->
            <input type="hidden" name="existingImagePath" value="">
        </#if>

        <div class="mb-3">
            <label for="imageFile" class="form-label">Product image</label>
            <input id="imageFile" class="form-control" type="file" name="imageFile" accept="image/*"/>
            <div class="form-text">Formats: JPG, PNG, WEBP, GIF — max 5 MB.</div>
        </div>

        <div class="d-flex gap-2">
            <button class="btn btn-primary" type="submit">${isEdit?then('Save','Create')}</button>
            <a class="btn btn-outline-secondary" href="/admin/products">Cancel</a>
        </div>
    </form>

<#-- ===========================
     MODALE "New category"
     - Post en x-www-form-urlencoded vers /admin/categories
     - Mise à jour du <select> + sélection auto
   =========================== -->
    <div class="modal fade" id="newCategoryModal" tabindex="-1" aria-labelledby="newCategoryLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form id="newCategoryForm">
                    <div class="modal-header">
                        <h5 class="modal-title" id="newCategoryLabel">New category</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="newCategoryName" class="form-label">Name</label>
                            <input id="newCategoryName" name="name" type="text" class="form-control" required>
                        </div>

                        <#-- CSRF pour la requête POST AJAX -->
                        <#if _csrf??>
                            <input type="hidden" id="csrfParam" value="${_csrf.parameterName!'_csrf'}">
                            <input type="hidden" id="csrfToken" value="${_csrf.token!}">
                        </#if>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary">Create</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

<#-- ===========================
     JS : gestion soumission modale (Create) + suppression (Delete)
     - Gestion d'erreurs sans lancer d'exception locale (pas de throw)
     - Fallback image sans onerror inline
   =========================== -->
    <script>
        (function () {
            // ----- CREATE (modale) -----
            const createForm = document.getElementById('newCategoryForm');
            if (createForm) {
                createForm.addEventListener('submit', async function (e) {
                    e.preventDefault();

                    const nameInput = document.getElementById('newCategoryName');
                    const categorySelect = document.getElementById('categoryId');
                    const name = (nameInput.value || '').trim();
                    if (!name) { nameInput.focus(); return; }

                    // CSRF
                    const csrfParam = document.getElementById('csrfParam')?.value || '_csrf';
                    const csrfToken = document.getElementById('csrfToken')?.value || '';

                    try {
                        const resp = await fetch('/admin/categories', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                            body: encodeURIComponent('name') + '=' + encodeURIComponent(name)
                                + (csrfToken ? '&' + encodeURIComponent(csrfParam) + '=' + encodeURIComponent(csrfToken) : '')
                        });

                        if (!resp.ok) {
                            let message = 'Request failed.';
                            try {
                                const data = await resp.json();
                                if (data && data.error) message = data.error;
                                else message = 'HTTP ' + resp.status;
                            } catch (_) {
                                const text = await resp.text();
                                if (text) message = text;
                            }
                            alert('❌ ' + message);
                            return;
                        }

                        const data = await resp.json(); // {id, name}

                        // Ajoute l'option dans le select + sélectionne-la
                        const opt = document.createElement('option');
                        opt.value = String(data.id);
                        opt.textContent = data.name;
                        categorySelect.appendChild(opt);
                        categorySelect.value = String(data.id);

                        // Reset champ + fermer modale
                        nameInput.value = '';
                        const modalEl = document.getElementById('newCategoryModal');
                        const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
                        modal.hide();
                    } catch (err) {
                        console.error(err);
                        alert('❌ Network error: ' + err.message);
                    }
                });
            }

            // ----- DELETE (bouton sous + New) -----
            const deleteBtn = document.getElementById('deleteCategoryBtn');
            const selectEl = document.getElementById('categoryId');
            if (deleteBtn && selectEl) {
                const csrfToken = document.getElementById('csrfToken')?.value || '';

                deleteBtn.addEventListener('click', async function () {
                    const val = (selectEl.value || '').trim();
                    if (!val) {
                        alert('Please select a category to delete.');
                        return;
                    }
                    if (!confirm('Are you sure you want to delete this category?')) return;

                    try {
                        const resp = await fetch('/admin/categories/' + encodeURIComponent(val), {
                            method: 'DELETE',
                            headers: csrfToken ? { 'X-CSRF-TOKEN': csrfToken } : {}
                        });

                        if (!(resp.status === 204 || resp.status === 200)) {
                            let message = 'Delete failed.';
                            try {
                                const data = await resp.json();
                                if (data && data.error) message = data.error;
                                else message = 'HTTP ' + resp.status;
                            } catch (_) {
                                const text = await resp.text();
                                if (text) message = text;
                            }
                            alert('❌ ' + message);
                            return;
                        }

                        // Supprime l’option du select et remet "(none)"
                        const opt = selectEl.querySelector('option[value="' + CSS.escape(val) + '"]');
                        if (opt) opt.remove();
                        selectEl.value = '';
                    } catch (err) {
                        console.error(err);
                        alert('❌ Network error: ' + err.message);
                    }
                });
            }

            // ----- Fallback d'image sans onerror inline -----
            document.querySelectorAll('img[data-fallback-src]').forEach(function (img) {
                function onErr() {
                    if (img.dataset.fallbackApplied === '1') return; // évite boucle en cas d'échec du fallback
                    img.dataset.fallbackApplied = '1';
                    img.src = img.getAttribute('data-fallback-src') || '';
                }
                img.addEventListener('error', onErr, { once: true });
            });
        })();
    </script>
</@ui.page>
