# AGENTS.md — build spec

Instructions for the coding agent working on this repository.

## Objective

A simplified supermarket checkout: a Spring Boot REST backend that prices a cart and applies
offers, plus a small Angular frontend that holds the cart and shows the receipt.

## Ground rules

- Small steps. One idea per commit. Every commit builds and its tests pass.
- Test first for the pricing logic: write the failing test, then the code, in the same commit.
- No feature that is not in this spec. Ask before adding one.
- Generated code goes in its own commit, with the generator command in the message.
- Never rewrite history; the commit log is part of the deliverable.
- Louay guides the work and reviews every commit before it lands; the agent writes the code.
- Never commit or push without Louay asking for it.

## Stack

Java 25, Spring Boot (current stable from start.spring.io, confirmed before scaffolding),
Gradle with a Java 25 toolchain and the foojay resolver, so `./gradlew` provisions the JDK for a
reviewer who doesn't have it. JUnit 5 + AssertJ, H2 in-memory, Angular (standalone components and signals),
TypeScript. No UI component library. No Swagger. No Lombok.

## Domain model

- `Money` — value type wrapping integer **cents**. No floating point anywhere in pricing.
- `Product` — `sku`, `name`, `unitPrice`
- `Offer` — `sku`, `quantity`, `bundlePrice`. At most one active offer per product.
- `Cart` — items of `sku` + `quantity`
- `Receipt` — lines + `total` + `totalSavings`
- `ReceiptLine` — `sku`, `name`, `quantity`, `unitPrice`, `lineTotal`, `appliedOffer` (nullable),
  `savings`

## Pricing rules

1. Price the whole cart. Order of items is irrelevant.
2. Per product: apply the offer as many times as it fits, charge the remainder at unit price.
3. Never apply an offer that costs more than the same items at unit price.
4. Each item counts towards at most one offer.
5. Reject an unknown sku or a quantity ≤ 0.

## Packages

- `pricing/` — plain Java, no Spring imports: `Money`, `Product`, `Offer`, `Cart`, `Receipt`,
  `ReceiptLine`, `Checkout`, `Catalog` (interface)
- `catalog/` — JPA entities, repository, `Catalog` implementation, seed data
- `api/` — `CheckoutController`, `ProductController`, DTOs, `@ControllerAdvice`

## REST API

- `GET /api/products` → products with their active offer
- `POST /api/checkout` → body `{ "items": [{ "sku": "APPLE", "quantity": 3 }] }` → receipt
- Validation errors → 400 with `ProblemDetail`

## Tests to cover

- Empty cart totals 0
- One item, several items, mixed cart
- Same cart in a different order gives the same total
- Offer applies exactly (2 apples = 0.45)
- Offer plus leftovers (3 apples = 0.75; 5 apples = 1.20)
- A product with no offer
- An offer more expensive than unit price is not applied
- Unknown sku rejected; quantity 0 and negative rejected
- Receipt reports the applied offer and the savings per line
- `@WebMvcTest` for both endpoints, including the 400 cases
- Frontend: the cart service adds, removes and changes quantity

## Frontend

Standalone components, signals for cart state, a typed `HttpClient` service, dev proxy to
`:8080`. Three views: product list, cart, receipt. **No pricing logic in the frontend** — the
backend is the source of truth.

## Out of scope

Several offers per product, offers spanning products, offer validity dates, authentication,
persistence beyond H2, Swagger, CI.
