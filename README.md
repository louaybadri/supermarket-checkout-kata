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

Prerequisites: JDK 25 and Node 24. With [mise](https://mise.jdx.dev) installed, `mise install`
picks both up from `mise.toml` for this directory only, leaving the versions you use elsewhere
untouched. Otherwise the Gradle wrapper downloads the JDK itself, and `frontend/.nvmrc` carries
the Node version for nvm, fnm and asdf.

```bash
# backend, http://localhost:8080
cd backend && ./gradlew bootRun

# frontend, http://localhost:4200
cd frontend && npm install && npm start

# tests
cd backend && ./gradlew test
cd frontend && npm test
```

The shop and its deals live in `backend/src/main/resources/catalog.yml`. The products stay put,
the offers come in named sets, and you pick which week is running without touching any code:

```bash
cd backend && ./gradlew bootRun --args='--catalog.active-preset=bundle-week'
```

`classic` is the exercise's own example, `bundle-week` has offers that span products and compete
for the same apples, and `none` turns every deal off.

## Assumptions

1. Prices are in euros and handled as whole cents, never as floating point numbers.
2. The order in which items are added does not matter. The whole cart is priced at checkout.
3. An offer such as "2 for 0.45€" applies as many times as it fits. Leftover items are charged
   at the normal price, so 3 apples cost 0.45€ + 0.30€ = 0.75€.
4. An offer may combine different products, for example an apple and a banana together. Haiilo
   confirmed this on 23 September. A single-product deal is the same thing with one entry in the
   set, so there is one kind of offer rather than two.
5. Several offers may want the same items. The checkout picks the combination that gives the
   customer the lowest total, not the first or the biggest-looking discount.
6. Each item in the cart counts towards at most one offer. The same apple is never discounted twice.
7. An offer that costs more than the same items at unit price, or exactly the same, is never
   applied.
8. Unknown products, and quantities of zero or less, are rejected.
9. "Weekly" offers are modelled as data, replaced by changing the catalog. Validity dates are
   not modelled; see "Not in scope".

## Decisions

- **Stack:** Java 25 and Spring Boot for the backend, Angular and TypeScript for the frontend,
  Gradle for the build.
- **The pricing logic is plain Java**, with no framework code in it, so it can be read and
  tested on its own.
- **Money is a value type over integer cents.** No `double` anywhere in pricing.
- **One shape of offer:** a name, the set of items it needs, and the price for that set.
- **The cheapest combination is found by searching**, not by applying offers greedily. With
  "3 apples for 0.60" and "an apple and a banana for 0.30", a basket of 3 apples and 3 bananas
  costs 1.20 greedily and 0.90 when the bundle is used three times. The search tries every
  offer that fits plus the option of stopping, and remembers each basket it has already solved,
  which keeps a supermarket-sized cart instant.
- **Checkout is stateless.** The frontend holds the cart and sends it to `POST /api/checkout`,
  which returns a receipt with lines, discounts and the total. There was no requirement to keep
  carts across sessions.
- **Products and offers live in an in-memory H2 database**, seeded at startup from
  `catalog.yml`, behind a `Catalog` interface. It needs no setup from the reviewer, and moving to
  PostgreSQL means changing configuration and migrations, not the pricing code.
- **Three tables.** `product` is the shelf, `offer` is the deal and its price, and `offer_item`
  says which products each deal needs. An offer can name any number of products, so its items
  cannot be columns on the offer row.

## How I worked

- I agreed scope and assumptions before writing code. This README is the first commit.
- The pricing logic is written test-first, in small commits that each build and pass their tests.
- Generated code (Spring Initializr, Angular CLI) sits in its own commits, with the command in
  the commit message.
- I used Claude Code as a pair programmer, working against the spec in `AGENTS.md`, and I
  reviewed every commit before it went in.

## Not in scope yet

- **Very large carts where many offers overlap.** Choosing the cheapest set of overlapping
  bundles is multi-dimensional knapsack, so the search is exponential in the number of products
  that offers touch. Products linked by an offer could be solved as independent groups, which
  keeps the exponent at the size of the largest group rather than the whole catalog.
- **Offers valid only for a given period** (`validFrom` / `validUntil` with an injected `Clock`).
- **A persistent database.** PostgreSQL would replace H2 behind the same `Catalog` interface.
- Continuous integration.
