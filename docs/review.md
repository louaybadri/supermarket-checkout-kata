# Review

A review of the kata as it stood on 24 September 2026, at commit `1ed34ba`, before it was
submitted. All 67 backend and 20 frontend tests were green. The problems below were found by
reading the code and by sending real requests to the running backend. Each one is fixed in its
own commit, with its test, and the commit message names the item, for example
"Look each product up once per checkout (review #1)".

| # | Area | Problem | Status |
|---|---|---|---|
| 1 | Pricing | Prices are read from the database for every basket the search tries | fixed |
| 2 | Pricing | The search goes one call deeper for every offer it applies | open |
| 3 | API | A `null` item crashes the server | open |
| 4 | API | Validation errors are not `ProblemDetail` | open |
| 5 | API | Every `IllegalArgumentException` becomes a 400, so our bugs look like the customer's | open |
| 6 | API + UI | The frontend does not know how many of a product may be bought | open |
| 7 | UI | The receipt is reset by writing signals inside an `effect()` | open |
| 8 | UI | A reply for an old cart can overwrite the receipt | open |
| 9 | UI | Money is formatted two different ways | open |

The evidence below was gathered with the `bundle-week` preset:

```bash
cd backend && ./gradlew bootRun --args='--catalog.active-preset=bundle-week'
```

---

## 1. Prices are read from the database for every basket

**Problem.** `BestPrice` works out the shelf price of every basket it explores, and does it
through the catalog:

```java
// BestPrice.shelfPriceOf
total = total.plus(catalog.require(line.getKey()).unitPrice().times(line.getValue()));
```

In the running application `catalog` is `JpaCatalog`, which is `@Transactional(readOnly = true)`.
Every `require` opens its own transaction and runs its own `SELECT`, and nothing is cached
between calls.

The number of baskets grows quickly because the two groups of offers, apple and banana on one
side and bread and milk on the other, are explored in every combination: about 1,750 states for
the first group times 101 for the second, so roughly 177,000 baskets and up to four lookups each.

**Evidence.**

```bash
curl -s -o /dev/null -w '%{time_total}s\n' -X POST localhost:8080/api/checkout \
  -H 'Content-Type: application/json' \
  -d '{"items":[{"sku":"APPLE","quantity":100},{"sku":"BANANA","quantity":100},
               {"sku":"BREAD","quantity":100},{"sku":"MILK","quantity":100}]}'
```

**28.6 seconds.** 3,000 apples with 3,000 bananas had not answered after 60 seconds.

**Fix.** Each checkout wraps the catalog in a `RememberingCatalog`, which fetches a product the
first time it is asked for and answers from memory after that. The receipt lines, the search and
the discounts all go through it. It lives for one checkout, so the next cart still sees the
catalog as it is.

The first plan was to hand `BestPrice` a map of prices for the skus in the cart. It would not
have been enough: before searching, `BestPrice` checks each offer against the shelf price of the
items it needs, and those can be products the cart does not hold.

**Test.** A catalog stub that counts calls: pricing the cart above calls `require` at most once
per sku. A timing test would prove nothing here, because the unit tests' catalog is already in
memory and fast; the count is what points at the cause.

**After.** At 20 of each product the search used to ask for each sku about 1,700 times; now it
asks once. The cart above went from 28.6 to about 11 seconds. The rest is the search itself,
which is #2: in memory, with no database at all, it takes 0.19 s at 50 of each product and 5.2 s
at 100, because every basket it solves keeps its own copy of the list of offers used.

- [x] Fixed in: "Look each product up once per checkout (review #1)"

---

## 2. The search goes one call deeper for every offer it applies

**Problem.** `chooseFor` applies an offer once, then calls itself on what is left:

```
chooseFor(10 apples) → chooseFor(8) → chooseFor(6) → … → chooseFor(0)
```

The depth of the recursion is the number of times offers are used, so it grows with the cart.

**Evidence.** 200,000 apples with "2 for 0.45" nests 100,000 calls. The JVM stack runs out:

```
java.lang.StackOverflowError
```

and the customer gets a **500**.

**Fix.** Recurse over the offers instead, and decide how many times to use each one in a loop:

```
offer 0 "Apple & banana": use it 0, 1, 2, … times
  └ offer 1 "3 apples": use it 0, 1, 2, … times
      └ offer 2 "Bread & milk": use it 0, 1, 2, … times
          └ no offers left: shelf price for whatever remains
```

The depth is now the number of offers, whatever the size of the cart. Solved baskets are
remembered under `(offer index, basket)`. A choice records each offer with the number of times
it is used, rather than a list with one entry per use, so a large cart does not build and copy
long lists. Taking the offers in a fixed order also stops "A then B" and "B then A" from both
being explored.

**Test.** 200,000 apples cost exactly 45,000.00. The competing-offers test (3 apples and 3
bananas cost 0.90, not the 1.20 a greedy pick gives) stays green, which shows the search still
finds the cheapest combination.

- [ ] Fixed in: _commit_

---

## 3. A `null` item crashes the server

**Problem.** `@Valid` checks the fields of each item, but not that the item exists:

```java
record CheckoutRequest(@NotNull @Valid List<ItemRequest> items)
```

**Evidence.**

```bash
curl -s -X POST localhost:8080/api/checkout -H 'Content-Type: application/json' \
  -d '{"items":[null]}'
```

A `NullPointerException` in `CheckoutRequest.toCart()`, and a **500**.

**Fix.** Put the constraints on the element type:

```java
record CheckoutRequest(@NotNull List<@NotNull @Valid ItemRequest> items)
```

**Test.** `@WebMvcTest`: `{"items":[null]}` is a 400.

- [ ] Fixed in: _commit_

---

## 4. Validation errors are not `ProblemDetail`

**Problem.** `ApiExceptionHandler` handles only our own exceptions. A failed `@Valid` throws
`MethodArgumentNotValidException`, and Spring answers it with its default error body. The spec
asks for a `ProblemDetail`. The existing tests only check the status code, so this went unnoticed.

**Evidence.** A quantity of 0:

```json
{ "timestamp": "2026-09-24T12:28:05.157Z", "status": 400, "error": "Bad Request", "path": "/api/checkout" }
```

No `title`, no `detail`, nothing the frontend can show.

**Fix.** `ApiExceptionHandler` extends `ResponseEntityExceptionHandler`, which already turns the
standard Spring MVC exceptions into `ProblemDetail`, and overrides `handleMethodArgumentNotValid`
so the detail names the field:

```json
{ "status": 400, "title": "Invalid cart",
  "detail": "items[0].quantity: must be greater than or equal to 1" }
```

**Test.** A quantity of 0 and a missing `items` both return a `title` and a `detail` naming the
field.

- [ ] Fixed in: _commit_

---

## 5. Our own bugs look like the customer's fault

**Problem.** The handler turns every `IllegalArgumentException` into a 400 "Invalid cart":

```java
@ExceptionHandler(IllegalArgumentException.class)
```

But the same exception is thrown where the customer has no part in it: `Money` going negative is
a bug in our code, and a malformed `Offer` is a mistake in `catalog.yml`.

**Evidence.** Two lines of 2,147,483,647 and 1 apples overflow the `int` quantity:

```json
{ "status": 400, "title": "Invalid cart",
  "detail": "An amount of money cannot be negative: -64424509440" }
```

The wrong status, and a message that means nothing to the shopper.

**Fix.**

1. Remove the catch-all. A bug on our side is a 500, which is honest.
2. Cap a line at 99 with `@Max(MAX_QUANTITY)`, where `MAX_QUANTITY = 99` is a single constant in
   `CheckoutRequest`. No shopper puts a hundred of one thing through a till, and a request that
   tries is rejected before pricing starts. The cap belongs to the API, not to `pricing`, which
   stays general and is why #2 is still worth fixing.

**Test.** A quantity of 100 is a 400 whose detail mentions 99. A checkout that throws
`IllegalArgumentException` inside is a 500.

- [ ] Fixed in: _commit_

---

## 6. The frontend does not know the limit

**Problem.** Once #5 is in, "+" still lets the shopper reach 100, and they only find out from a
400 at checkout. Writing 99 into the frontend as well would leave two numbers to keep in step.

**Fix.** The backend stays the single source of truth. `GET /api/products` sends the same
constant with every product:

```java
record ProductResponse(String sku, String name, long unitPriceCents, int maxQuantity) {}
```

The frontend's `Product` gains `maxQuantity`, `Cart.add` stops there, and "+" is disabled at the
limit. A limit per product could come later without changing the shape of the API.

**Test.** Backend: `GET /api/products` returns `maxQuantity: 99`. Frontend: the cart does not go
past `maxQuantity`, and the button is disabled when it is reached.

Two commits, one per side:

- [ ] Backend fixed in: _commit_
- [ ] Frontend fixed in: _commit_

---

## 7. The receipt is reset by writing signals inside an `effect()`

**Problem.**

```ts
effect(() => {
  this.cart.lines();          // read only so the effect re-runs
  this.receipt.set(null);
  this.error.set(null);
});
```

Angular allows this, but effects are meant for side effects outside the signal graph (logging,
storage, the DOM), not for keeping one piece of state in step with another. The effect runs
during change detection rather than when the cart changes, so for a moment the new cart sits next
to the old bill, and the dependency on the cart is hidden in a call whose value is thrown away.

**Fix.** `linkedSignal`, which is made for state that resets when its source changes and can
still be set by hand:

```ts
protected readonly receipt = linkedSignal({ source: this.cart.lines, computation: (): Receipt | null => null });
protected readonly error = linkedSignal({ source: this.cart.lines, computation: (): string | null => null });
```

The effect goes away.

**Test.** The existing "takes the bill down as soon as the cart changes" is tightened to read the
receipt straight after `cart.add()`, with no change detection in between.

- [ ] Fixed in: _commit_

---

## 8. A reply for an old cart can overwrite the receipt

**Problem.** `checkout()` subscribes and forgets:

```ts
this.api.ring(this.cart.toRequestItems()).subscribe({ next: (receipt) => this.receipt.set(receipt), … });
```

The Checkout button is disabled while a request is out, but the cart buttons are not. Add an
apple while the backend is pricing, and the reply for the old cart is printed as the bill for the
new one.

**Evidence.** Found by reading the code, not reproduced in the browser.

**Fix.** Checkout becomes a stream. `switchMap` keeps only the latest request, and
`takeUntil` drops a reply the cart has moved past:

```ts
this.clicks.pipe(
  tap(() => this.ringing.set(true)),
  switchMap(() => this.api.ring(this.cart.toRequestItems()).pipe(
    takeUntil(this.cartChanges),
    finalize(() => this.ringing.set(false)),
  )),
  takeUntilDestroyed(),
).subscribe(…);
```

This is the other half of #7: `linkedSignal` clears the bill already on screen, `takeUntil`
stops the one still on its way from replacing it.

**Test.** With `HttpTestingController`: start a checkout, add an item, then answer the request.
No receipt is shown, and the button no longer says "Ringing up…".

- [ ] Fixed in: _commit_

---

## 9. Money is formatted two different ways

**Problem.** The shelf and the cart use `CurrencyPipe`; the receipt has its own:

```ts
protected euros(cents: number): string {
  return (cents / 100).toFixed(2) + ' €';
}
```

**Fix.** `{{ cents / 100 | currency: 'EUR' }}` everywhere, and `euros()` goes.

**Test.** The receipt view test expects the pipe's format.

- [ ] Fixed in: _commit_
