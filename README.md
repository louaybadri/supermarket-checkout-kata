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

| Preset | What it shows |
|---|---|
| `classic` (default) | The exercise's own example: 2 apples for 0.45 |
| `bundle-week` | Offers that span products, and one that competes with them for the same apples |
| `fruit-war` | Five offers over apples, bananas and oranges, where taking the biggest saving first is wrong |
| `breakfast` | Meal deals across several products, with two deals wanting the same coffee |
| `not-a-deal` | Two "offers" dearer than, or equal to, the shelf price, which are never applied |
| `crowded` | Six deals on apples alone, the slowest kind of week for the search |
| `none` | No deals, only shelf prices |

Each preset in `catalog.yml` has its run command and a worked cart in the comment above it, and
`OfferWeeksTest` checks that every worked cart prices the way the comment says.

## The API

```
GET  /api/products    what the shop sells
GET  /api/offers      the deals running this week, with the items each one needs
POST /api/checkout    a cart in, a receipt out
```

Every amount is in whole cents, named `...Cents`, so no price has to survive a floating point
number on the way to the browser. Each product also carries `maxQuantity`, the most of it one line
of a cart may ask for, so the frontend can stop there without writing the number down itself.

```bash
curl -s -X POST localhost:8080/api/checkout -H 'Content-Type: application/json' \
  -d '{"items":[{"sku":"APPLE","quantity":3}]}'
```
```json
{
  "lines": [
    { "sku": "APPLE", "name": "Apple", "quantity": 3, "unitPriceCents": 30, "lineTotalCents": 90 }
  ],
  "discounts": [{ "name": "2 apples for 0.45", "times": 1, "savingCents": 15 }],
  "shelfTotalCents": 90,
  "totalSavingsCents": 15,
  "totalCents": 75
}
```

A cart the shop cannot price comes back as a 400 `ProblemDetail` saying which part is wrong: an
unknown product, a quantity outside 1 to 99, or the same product on two lines.

```json
{ "status": 400, "title": "Unknown product",
  "detail": "The shop does not sell 'UNICORN'", "sku": "UNICORN" }
```
```json
{ "status": 400, "title": "Invalid cart",
  "detail": "items[0].quantity: must be less than or equal to 99" }
```

A fault on the shop's own side is a 500, never a 400: the customer is not told their cart is wrong
for a bug that is ours.

## How it is laid out

```
backend/src/main/java/com/louaybadri/checkout/
  pricing/    the rules, plain Java with no Spring or JPA in it
              Money, Product, Offer, Cart, Catalog, Checkout, BestPrice, Receipt
  catalog/    the same catalog backed by H2, seeded from catalog.yml
  api/        the three endpoints, their DTOs, and the 400 handler

frontend/src/app/
  catalog/    the shelf: the API service and the product list
  cart/       the cart as a signal, and its view
  receipt/    the checkout call and the printed bill
  connection/ whether the shop can be reached, and the banner that says so
```

`pricing` depends on nothing. `catalog` and `api` depend on `pricing`, never the other way round,
which is why the pricing tests need no database and no Spring context and run in milliseconds.

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
8. Unknown products are rejected, and so is asking for fewer than 1 or more than 99 of a product.
   Nobody puts a hundred of one thing through a till. A cart lists each product once, with how
   many of it are wanted, so a product on two lines is rejected rather than added up.
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
  costs 1.20 greedily and 0.90 when the bundle is used three times. The search takes the offers
  one at a time and tries using each one 0, 1, 2… times, so the recursion is only as deep as the
  list of offers, however large the cart.
- **The search remembers each situation it has solved, under the right label.** Many routes
  through the offers reach the same situation, so each answer is kept and reused. It is filed
  under the next offer to decide and what is left of the products that offer or a later one can
  use; anything no later offer can use is paid at shelf price straight away. Filed under the whole
  basket, as a first attempt was, almost nothing matched and the memory only filled up. With the
  right label, fruit-war with 99 of everything went from 16 s to 0.18 s. The story is in
  `docs/review.md`, items 2 and 12.
- **The quantity limit is written once.** `CheckoutRequest.MAX_QUANTITY` is what the checkout
  validates against and what `GET /api/products` sends as `maxQuantity`, so the cart's buttons and
  quantity field cannot drift from what the backend enforces.
- **Checkout is stateless.** The frontend holds the cart and sends it to `POST /api/checkout`,
  which returns a receipt with lines, discounts and the total. There was no requirement to keep
  carts across sessions.
- **An unreachable shop is said out loud.** When a request gets no answer, a banner says the shop
  cannot be reached and the request is sent again every three seconds, until "Back online". A
  checkout pressed meanwhile prints its bill once the shop answers. Nothing polls in the
  background: the app notices on the next thing the shopper does.
- **Products and offers live in an in-memory H2 database**, seeded at startup from
  `catalog.yml`, behind a `Catalog` interface. It needs no setup from the reviewer, and moving to
  PostgreSQL means changing configuration and migrations, not the pricing code.
- **Three tables.** `product` is the shelf, `offer` is the deal and its price, and `offer_item`
  says which products each deal needs. An offer can name any number of products, so its items
  cannot be columns on the offer row.

## How I worked

- I agreed scope and assumptions before writing code. This README is the first commit.
- I asked whether an offer can combine different products rather than guessing. The answer came
  on 23 September, and the commit that day reshapes `Offer` around it — the history shows the
  requirement arriving and the model changing to meet it.
- The pricing logic is written test-first, in small commits that each build and pass their tests.
- Generated code (Spring Initializr, Angular CLI) sits in its own commits, with the command in
  the commit message, so it is easy to skip.
- I used Claude Code as a pair programmer, working against the spec in `AGENTS.md`, and I
  reviewed every commit before it went in.
- Before sending, I reviewed the whole thing. `docs/review.md` lists twelve findings, each with the
  evidence that showed it and the fix it got, and every fix is its own commit tagged
  "(review #N)", so the history reads build, review, fix.

## Not in scope yet

- **A week with many offers.** Choosing the cheapest set of overlapping bundles is a knapsack
  problem, and the search tries every combination of how many times each offer is used, so it is
  exponential in the number of offers, not in the size of the cart. Remembering solved situations
  keeps a week like fruit-war fast, but many products chained together by offers would still
  multiply the situations. Offers that share no product could be solved as independent groups,
  adding their costs instead of multiplying them.
- **Offers valid only for a given period** (`validFrom` / `validUntil` with an injected `Clock`).
- **A persistent database.** PostgreSQL would replace H2 behind the same `Catalog` interface.
- Continuous integration.
