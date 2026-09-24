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
- `Offer` — `name`, `requiredItems` (sku to quantity), `price`. May combine different products.
- `Cart` — items of `sku` + `quantity`
- `BestPrice` — picks the combination of offers with the lowest total. Package-private.
- `Receipt` — `lines` + `discounts` + `total` + `totalSavings()`
- `ReceiptLine` — `sku`, `name`, `quantity`, `unitPrice`, `lineTotal`
- `AppliedOffer` — `name`, `times`, `saving`. An offer across products cannot hang off one line,
  so discounts are their own section, the way a till prints them. Savings are computed, not stored.

## Pricing rules

1. Price the whole cart. Order of items is irrelevant.
2. Apply the combination of offers that gives the customer the lowest total. Greedy is wrong once
   offers compete for the same items.
3. Whatever no offer covers is charged at shelf price.
4. Never apply an offer that costs more than the same items at shelf price, or exactly the same.
5. Each item counts towards at most one offer.
6. Reject an unknown sku or a quantity ≤ 0.

## Packages

- `pricing/` — plain Java, no Spring imports: `Money`, `Product`, `Offer`, `Cart`, `Receipt`,
  `ReceiptLine`, `Checkout`, `Catalog` (interface)
- `catalog/` — JPA entities, repository, `Catalog` implementation, seed data
- `api/` — `CheckoutController`, `ProductController`, DTOs, `@ControllerAdvice`

## REST API

- `GET /api/products` → what the shop sells
- `GET /api/offers` → this week's offers with the items each one needs. Separate from products,
  because an offer can span several of them
- `POST /api/checkout` → body `{ "items": [{ "sku": "APPLE", "quantity": 3 }] }` → receipt
- Every amount in the JSON is in whole cents, named `...Cents`. The frontend formats them
- Validation errors and unknown products → 400 with `ProblemDetail`

## Tests to cover

- Empty cart totals 0
- One item, several items, mixed cart
- Same cart in a different order gives the same total
- Offer applies exactly (2 apples = 0.45)
- Offer plus leftovers (3 apples = 0.75; 5 apples = 1.20)
- A product with no offer
- An offer more expensive than unit price is not applied
- A bundle across products (apple + banana)
- Competing offers: "3 apples for 0.60" and "apple+banana for 0.30" with 3 apples and 3 bananas
  costs 0.90, not the 1.20 a greedy pick would give
- Unknown sku rejected; quantity 0 and negative rejected
- Receipt reports the applied offer and the savings per line
- `@WebMvcTest` for both endpoints, including the 400 cases
- Frontend: the cart service adds, removes and changes quantity

## Frontend

Standalone components, signals for cart state, a typed `HttpClient` service, dev proxy to
`:8080`. Three views: product list, cart, receipt. **No pricing logic in the frontend** — the
backend is the source of truth. Every component keeps its template and styles in their own
`.html` and `.css` files, never inline.

## Out of scope

Offer validity dates, authentication, persistence beyond H2, Swagger, CI, and splitting the
search into independent groups of products for very large carts.
