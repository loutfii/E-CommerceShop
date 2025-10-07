# E-commerce (Spring Boot + Freemarker) — README

Ce projet est une mini-boutique en Spring Boot avec rendu Freemarker, gestion de panier (invité + connecté), authentification, administration des produits (CRUD + upload d'images), et catégories (création à la volée côté admin).

## Fonctionnalités

**Catalogue produits** : listing paginé, recherche simple (name), filtres (min/max/category).

**Détail produit** : ajout au panier depuis la fiche.

**Panier** :
- Invité : en session.
- Authentifié : en base (Cart + CartLine).
- Fusion automatique du panier invité → DB au login.
- Incr/Decr/Remove avec toasts (Bootstrap).

**Auth** :
- Login/Register sous /auth/*.
- Sécurité Spring Security (CSRF activé).
- Rôle ADMIN pour l'accès back-office.

**Admin** :
- CRUD produits, upload image locale → /uploads/**.
- Édition produit : création "à la volée" d'une catégorie (modale "+ New").
- Suppression d'image existante gérée proprement.

**Paiement Stripe** :
- Bouton “Pay with Stripe” sur la page Checkout.
- Création de la Checkout Session côté serveur (montant recalculé à partir des prix DB).
- Redirection automatique vers les pages Success et Cancel.

**UI/Theme** :
- Layout unique Freemarker (macros.ftlh), navbar sombre, footer, toasts.
- Pagination centrée.

**Compte** :
- Bouton Delete account pour supprimer définitivement son compte + paniers.

**Robustesse** :
- Redirection post-login durcie pour éviter les assets (images/css/js) comme cibles.
- Spring Security est activé sur toute l'application. Le login/logout/register se font sous /auth/* avec CSRF activé et logout en POST avec token CSRF.
- Les opérations sensibles (CRUD produits, etc.) sont protégées par @PreAuthorize("hasRole('ADMIN')") au niveau des méthodes.
- Les mots de passe sont hachés avec BCrypt (force = 10) via PasswordEncoder Spring. Les mots de passe ne sont jamais stockés en clair en base de données.
- Un handler de succès personnalisé gère la fusion du panier invité vers la DB, puis effectue une redirection filtrée.
- La redirection n'est autorisée une fois connecté que si :
c'est une requête GET, et la cible n'est pas un asset (/uploads/**, /images/**, /css/**, /js/**, /webjars/**, /favicon.ico).
- Au niveau des entités, Cart utilise @OneToMany(..., cascade = REMOVE, orphanRemoval = true) pour éviter les orphelins et garantir la suppression des lignes du panier.
- Pour l'upload d'image (admin) :
Vérification stricte du MIME : image/jpeg, image/png, image/webp, image/gif.
Nom de fichier aléatoire (UUID + extension) → pas de réutilisation du nom fourni par l'utilisateur.
Écriture dans un dossier dédié (app.upload-dir) et exposition publique via /uploads/....
Suppression sûre : on ne supprime que si le chemin commence par /uploads/

##  Stack technique

- Java 25, Maven
- Spring Boot (Web, Security, Data JPA, Validation)
- Freemarker (templates *.ftlh)
- Base de données : JPA/Hibernate (H2 en dev ou autre DB au choix)
- Bootstrap 5.3 + static/css/theme.css
- Stripe
```

## Lancer l'application

```bash
# 1) Compiler
mvn clean package

# 2) Démarrer
mvn spring-boot:run
```

Application : http://localhost:8080

## Comptes & rôles

**Inscription** : /auth/register

**Login** : /auth/login

**Pour créer un ADMIN**, deux options :
- Via un script `data.sql` (à ajouter) qui insère un user avec rôle ADMIN.
- Élever manuellement un user existant en DB (champ role = ADMIN).

Le service `DbUserDetailsService` charge l'utilisateur par email et normalise les rôles en `ROLE_USER` / `ROLE_ADMIN`.

## Structure (extraits)

```
src/main/java/com/example/ecommerce
├─ bll/services/impls
│   ├─ CartServiceImpl.java         # Panier session/DB + merge au login
│   ├─ CategoryServiceImpl.java     # findAll + create
│   └─ ImageStorageServiceImpl.java # Upload local, deleteIfOwned
│
├─ dal/repositories
│   ├─ ProductRepository.java       # findByIdWithCategory (fetch join), specs
│   └─ ...                          # UserRepository, CartRepository, CartLineRepository, CategoryRepository
│
├─ dl/entities
│   ├─ Cart.java                    # @OneToMany(cascade=REMOVE, orphanRemoval=true)
│   └─ CartLine.java, Product.java, Category.java, User.java ...
│
├─ il/interfaces
│   ├─ CartService.java, ProductService.java, CategoryService.java, AuthService.java
│   └─ dto/...
│
├─ pl/config
│   └─ WebMvcConfig.java            # /uploads/** → file:... + Interceptor cart badge
│
├─ pl/security
│   ├─ SecurityConfig.java          # FilterChain + password encoder
│   └─ CartMergeOnLoginSuccessHandler.java # Merge + redirect filtrée
│
├─ pl/controllers
│   ├─ HomeController.java          # "/" → redirect:/products
│   ├─ ProductController.java       # (si présent) /products
│   ├─ CartController.java          # /cart (add/update/remove/view)
│   ├─ AdminProductController.java  # CRUD + upload + edit modal category
│   ├─ AdminCategoryController.java # POST /admin/categories (JSON), DELETE /admin/categories/{id}
│   ├─ AuthController.java          # login/register
│   └─ AccountController.java       # delete account (optionnel si ajouté)
│
src/main/resources/
├─ templates/
│   ├─ macros.ftlh                  # Layout global (navbar, footer, toasts, pagination centrée)
│   ├─ products/...
│   ├─ admin/products/form.ftlh     # Modale "+ New category"
│   ├─ cart/view.ftlh
│   └─ checkout/(checkout|success|cancel).ftlh
└─ static/css/theme.css
```

## Upload d'images

**Service** : ImageStorageServiceImpl

**Types autorisés** : image/jpeg, image/png, image/webp, image/gif

**Enregistrement** → /uploads/<uuid>.<ext>

**Suppression** :
- côté admin si "remove image" coché
- au delete produit → deleteIfOwned(...) appelé

**Exposition web** : WebMvcConfig.addResourceHandlers("/uploads/**" → file:...)

## Panier — détails

**Invité** : GUEST_CART (Map<Long, Integer>) en session.

**Connecté** : entités Cart (status OPEN) + CartLine.

**Au login** : CartMergeOnLoginSuccessHandler appelle cartService.mergeSessionIntoDb(session).

**Suppression lignes** : sur entité Cart, relation @OneToMany(mappedBy="cart", cascade = REMOVE, orphanRemoval = true).

## Post-login redirect "durci"

Pour éviter un redirect vers une image ou un asset, le handler :
- n'accepte que les GET,
- exclut /uploads/**, /images/**, /css/**, /js/**, /webjars/**, /favicon.ico,
- sinon fallback → /products.
```
## Auteur 

Ghazal Loutfi Adonis



