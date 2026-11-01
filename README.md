# 🛒 E-Shop — Backend microservices

Backend d'une boutique en ligne construit en **architecture microservices** avec Spring Boot et Spring Cloud : découverte de services avec Eureka, API Gateway, authentification OAuth2/JWT via Keycloak et persistance PostgreSQL.

Le frontend associé se trouve ici : [frant-shop](https://github.com/Wael-BenAbid/frant-shop).

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue)
![Keycloak](https://img.shields.io/badge/Keycloak-OAuth2%20%2F%20JWT-lightgrey)

## Architecture

```
                    ┌──────────────┐
  Frontend ───────▶ │   Gateway    │ :8085   (routage + validation JWT + CORS)
  (React)           └──────┬───────┘
                           │  lb://  (load balancing via Eureka)
       ┌───────────┬───────┼───────────┬─────────────┐
       ▼           ▼       ▼           ▼             │
  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐    │
  │ Product │ │  Order  │ │  User   │ │  Cart   │    │
  │  :8081  │ │  :8082  │ │  :8083  │ │  :8084  │    │
  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘    │
       └───────────┴─────┬─────┴───────────┘         │
                         ▼                           ▼
                  ┌─────────────┐            ┌──────────────┐
                  │ PostgreSQL  │            │ Eureka :8761 │
                  │   :5433     │            └──────────────┘
                  └─────────────┘
                  Keycloak :8080 (realm « ecommerce ») ─ émet les tokens JWT
```

| Service | Port | Rôle |
|---|---|---|
| `eureka-server` | 8761 | Registre de découverte des services |
| `gateway` | 8085 | Point d'entrée unique, routage vers les services, validation des JWT |
| `product-service` | 8081 | Produits, catégories (arborescence), promotions, upload d'images |
| `order-service` | 8082 | Commandes |
| `user-service` | 8083 | Inscription, connexion, gestion des utilisateurs (via Keycloak) |
| `cart-service` | 8084 | Panier |

## Technologies

- **Java 17**, **Spring Boot 3.2**, **Spring Cloud 2023.0**
- **Spring Cloud Gateway** et **Netflix Eureka**
- **Spring Security** + **OAuth2 Resource Server** (JWT) avec **Keycloak**
- **Spring Data JPA** / Hibernate, **PostgreSQL**
- **springdoc-openapi** (documentation Swagger du user-service)
- Maven (projet multi-modules)

## API (via le gateway `http://localhost:8085`)

| Ressource | Endpoints principaux |
|---|---|
| Produits | `GET /api/products`, `GET /api/products/{id}`, `GET /api/products/by-category/{category}`, `POST /api/products`, `POST /api/products/batch`, `PUT /api/products/{id}`, `PUT /api/products/{id}/decrement-stock`, `DELETE /api/products/{id}` |
| Catégories | `GET /api/categories`, `GET /api/categories/tree`, `GET /api/categories/{idOrSlug}`, `POST`, `PUT /{id}`, `PATCH /reorder`, `DELETE /{id}` |
| Utilisateurs | `POST /api/users/register`, `POST /api/users/login`, `GET /api/users`, `GET /api/users/{id}`, `GET /api/users/details/{username}`, `GET /api/users/stats/summary` |
| Commandes | CRUD sur `/api/orders` |
| Panier | CRUD sur `/api/cart`, `POST /api/cart/add` |

Le product-service expose aussi `/api/discounts` (promotions) et `/api/files/upload` (images).

## Lancer le projet

### Prérequis
- JDK 17 et Maven
- PostgreSQL sur le port **5433** avec une base `ecommerce_db`
- Keycloak sur le port **8080** avec un realm `ecommerce` et un client `ecommerce-client`

### Variables d'environnement

| Variable | Description |
|---|---|
| `DB_PASSWORD` | Mot de passe PostgreSQL (par défaut `12345` en local) |
| `KEYCLOAK_CLIENT_SECRET` | Secret du client `ecommerce-client` dans Keycloak |
| `KEYCLOAK_ADMIN_PASSWORD` | Mot de passe de l'administrateur Keycloak |

### Démarrage (dans cet ordre)

```bash
mvn clean install

cd eureka-server   && mvn spring-boot:run   # 1. registre
cd gateway         && mvn spring-boot:run   # 2. gateway
cd product-service && mvn spring-boot:run   # 3. services métier…
cd user-service    && mvn spring-boot:run
cd order-service   && mvn spring-boot:run
cd cart-service    && mvn spring-boot:run
```

Le tableau de bord Eureka est ensuite disponible sur http://localhost:8761.

## Auteur

**Wael Ben Abid** · [GitHub](https://github.com/Wael-BenAbid)
