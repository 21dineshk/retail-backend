# retail-backend

[![Java 21](https://img.shields.io/badge/java-21-007396)](https://adoptium.net/temurin/releases/?version=21)
[![Spring Boot 3.5](https://img.shields.io/badge/spring--boot-3.5-6DB33F)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

A small, batteries-included Spring Boot service that pretends to be a retail
website. It's designed to be a realistic upstream API for testing
[MCP-Forge](https://github.com/anthropics/anthropic-cookbook) and other tools
that need a real, populated backend to call.

On every boot it auto-loads:

- **10,000 fake products** across 15 categories — `Electronics`,
  `Books`, `Clothing`, `Home & Kitchen`, `Sports & Outdoors`,
  `Toys & Games`, `Beauty`, `Grocery`, `Office Supplies`, `Pet Supplies`,
  `Automotive`, `Health`, `Tools & Hardware`, `Garden`, `Music`
- **8 test users** (alice, bob, carol, david, eva, frank, grace, henry —
  all `@example.com`, ids `1` … `8`)
- **~45 historical orders** spread across the test users with realistic
  status mix (DELIVERED / SHIPPED / CONFIRMED / PENDING / CANCELLED)

Database is in-memory **H2** — wiped on every restart and re-seeded from
[`src/main/resources/seed/products.csv`](src/main/resources/seed/products.csv),
so you always start from a known state.

---

## Run it

See **[SETUP.md](SETUP.md)** for the dead-simple guide (one prereq: Java 21,
no Maven install needed thanks to the included `./mvnw` wrapper).

TL;DR for the impatient on macOS / Linux:

```bash
./mvnw -DskipTests package
java -jar target/retail-backend-0.1.0.jar
```

Then open http://localhost:8090/swagger-ui.html.

---

## API surface

All endpoints are unauthenticated — pass the user identity as a path
param (`/api/users/{userId}/...`). Payment is intentionally out of scope.

| Group | Endpoints |
|---|---|
| **Products** | `GET /api/products` (search + paginate + filter), `GET /api/products/{id}`, `GET /api/products/sku/{sku}`, `GET /api/products/{id}/availability`, `GET /api/products/categories` |
| **Users** | `GET /api/users`, `GET /api/users/{userId}`, `GET /api/users/by-email/{email}` |
| **Cart** | `GET /api/users/{userId}/cart`, `POST /api/users/{userId}/cart/items`, `PUT /api/users/{userId}/cart/items/{productId}`, `DELETE /api/users/{userId}/cart/items/{productId}`, `DELETE /api/users/{userId}/cart` |
| **Orders** | `POST /api/users/{userId}/orders/checkout`, `GET /api/users/{userId}/orders`, `GET /api/users/{userId}/orders/{orderId}`, `POST /api/users/{userId}/orders/{orderId}/cancel` |
| **Health** | `GET /actuator/health` |

Total: **15 paths, 17 operations.**

### OpenAPI

Spring Boot serves the spec at:

| URL | Format |
|---|---|
| http://localhost:8090/v3/api-docs | OpenAPI 3.x JSON |
| http://localhost:8090/v3/api-docs.yaml | OpenAPI 3.x YAML |
| http://localhost:8090/swagger-ui.html | Interactive browser |

Both `localhost` and `host.docker.internal` are listed in the spec's
`servers` array, so the doc is usable both from the host browser and
from a Docker-bridged consumer (e.g. MCP-Forge running in a container).

---

## Quick smoke test

After starting the app:

```bash
# Search electronics under $50, page 0, 5 results
curl 'http://localhost:8090/api/products?category=Electronics&maxPrice=50&size=5'

# Add an item to alice's cart
curl -X POST http://localhost:8090/api/users/1/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productId": 1, "quantity": 2}'

# Checkout
curl -X POST http://localhost:8090/api/users/1/orders/checkout \
  -H 'Content-Type: application/json' -d '{}'

# Recent orders
curl http://localhost:8090/api/users/1/orders
```

---

## Tech stack

- **Java 21**
- **Spring Boot 3.5** (web, data-jpa, validation, actuator)
- **H2** (in-memory database)
- **springdoc-openapi 2.7** (OpenAPI 3 generation + Swagger UI)
- **Maven** (build) — `./mvnw` wrapper bundled, no install needed

---

## Project layout

```
retail-backend/
├── mvnw  / mvnw.cmd  / .mvn/                  ← Maven Wrapper (no Maven install needed)
├── pom.xml                                    ← dependencies + build config
├── SETUP.md                                   ← step-by-step setup for non-Java users
├── README.md                                  ← you are here
├── LICENSE                                    ← MIT
└── src/main/
    ├── java/com/example/retail/
    │   ├── RetailBackendApplication.java      ← Spring Boot entry point
    │   ├── config/                            ← OpenAPI / Spring config
    │   ├── domain/                            ← JPA entities (Product, User, Order, Cart, ...)
    │   ├── repository/                        ← Spring Data JPA repos
    │   ├── service/                           ← business logic + DataSeeder
    │   └── web/                               ← REST controllers + DTOs + error handlers
    └── resources/
        ├── application.yml                    ← server.port = 8090, H2 in-memory
        └── seed/products.csv                  ← the 10,000-row product dataset
```

---

## License

[MIT](LICENSE) — do whatever you want with it.
