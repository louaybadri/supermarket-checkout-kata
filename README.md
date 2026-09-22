# Supermarket Checkout Kata

A simplified supermarket checkout with offers, built with a Java / Spring Boot backend
and an Angular / TypeScript frontend.

## The task

> Implement a simplified supermarket checkout system. The cart can contain any number and
> combination of available items, in any order. Additionally the checkout system should support
> weekly offers, where an offer defines a number of items that are discounted when bought
> together. For example: one apple costs 0.30€, but 2 apples are offered at 0.45€. These offers
> should be applied automatically during checkout.

## How to run

Prerequisites: JDK 25, Node 22.22 or newer. The Gradle build downloads the JDK itself if you
don't have it.

```bash
# backend, http://localhost:8080
cd backend && ./gradlew bootRun

# frontend, http://localhost:4200
cd frontend && npm install && npm start

# tests
cd backend && ./gradlew test
cd frontend && npm test
```

## Assumptions

1. Prices are in euros and handled as whole cents, never as floating point numbers.
2. The order in which items are added does not matter. The whole cart is priced at checkout.
3. An offer such as "2 for 0.45€" applies as many times as it fits. Leftover items are charged
   at the normal price, so 3 apples cost 0.45€ + 0.30€ = 0.75€.
4. Each product has at most one active offer. The offer is applied as many times as it fits and
   the leftovers are charged at unit price, which gives the lowest price for that product.
5. An offer that costs more than the same items at unit price is never applied.
6. Each item in the cart counts towards at most one offer. The same apple is never discounted twice.
7. I asked Haiilo whether an offer can combine different products, for example an apple and a
   banana. Until they answer, offers apply to a single product. The offer model is designed so
   that a multi-product offer would be a new implementation rather than a redesign.
8. Unknown products, and quantities of zero or less, are rejected.
9. "Weekly" offers are modelled as data, replaced by changing the catalog. Validity dates are
   not modelled; see "Not in scope".

## Decisions

- **Stack:** Java 25 and Spring Boot for the backend, Angular and TypeScript for the frontend,
  Gradle for the build.
- **The pricing logic is plain Java**, with no framework code in it, so it can be read and
  tested on its own.
- **Money is a value type over integer cents.** No `double` anywhere in pricing.
- **Checkout is stateless.** The frontend holds the cart and sends it to `POST /api/checkout`,
  which returns a receipt with lines, discounts and the total. There was no requirement to keep
  carts across sessions.
- **Products and offers live in an in-memory H2 database**, seeded at startup, behind a
  `Catalog` interface. It needs no setup from the reviewer, and moving to PostgreSQL means
  changing configuration and migrations, not the pricing code.

## How I worked

- I agreed scope and assumptions before writing code. This README is the first commit.
- The pricing logic is written test-first, in small commits that each build and pass their tests.
- Generated code (Spring Initializr, Angular CLI) sits in its own commits, with the command in
  the commit message.
- I used Claude Code as a pair programmer, working against the spec in `AGENTS.md`, and I
  reviewed every commit before it went in.

## Not in scope yet

- **Several offers on the same product.** Greedy is no longer optimal then, and it needs a small
  dynamic-programming pass over the quantity:
  `cheapest[n] = min(cheapest[n-1] + unitPrice, cheapest[n-k] + offerPrice)`.
- **Offers combining several products.** A harder optimisation problem, which I would want the
  real business rules for before designing.
- **Offers valid only for a given period** (`validFrom` / `validUntil` with an injected `Clock`).
- **A persistent database.** PostgreSQL would replace H2 behind the same `Catalog` interface.
- Continuous integration.
