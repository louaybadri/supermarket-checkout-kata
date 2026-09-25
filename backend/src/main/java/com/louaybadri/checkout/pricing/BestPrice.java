package com.louaybadri.checkout.pricing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chooses which offers to apply so that the customer pays as little as possible.
 *
 * <p>Applying the best-looking offer first is not good enough once offers compete for the same
 * items: with "3 apples for 0.60" and "an apple and a banana for 0.30", a basket of 3 apples and
 * 3 bananas costs 1.20 that way and 0.90 if the bundle is used three times. So every option is
 * tried and the cheapest wins.
 *
 * <p>The offers are taken one at a time, in a fixed order. For each one, a loop tries using it
 * 0, 1, 2… times, and whatever is left goes to the next offer; once the offers run out, the rest
 * is paid at shelf price. The recursion is therefore as deep as the list of offers, however large
 * the cart, and each combination is tried once rather than in every order.
 *
 * <p>Many different routes through those loops end up asking the same question, so each answer is
 * written down and reused (see {@link #solved}). The trick is what an answer is filed under: only
 * the products the offers still to come can use. With fruit-war and 99 of everything on the
 * shelf, the loops walk about 60 million routes, but they ask only about 5,000 different
 * questions; answering each once prices the cart in 0.18 s instead of 16.
 *
 * <p>One instance prices one cart.
 */
class BestPrice {

	/**
	 * A way to buy a basket: what it costs, and how many times each offer is used, in the order
	 * of this week's offers.
	 */
	record OfferCombination(Money total, Map<Offer, Integer> offersUsage) {
	}

	/**
	 * Where the search stands: the next offer to decide, and what is left of the products that
	 * offer, or a later one, could still use. {@code (4, {BANANA=37})} reads "37 bananas left, and
	 * only offer 4 still to decide".
	 *
	 * <p>It is the label an answer is filed under in {@link #solved}. Records compare by content,
	 * so two routes that reach the same situation find the same answer.
	 */
	private record Situation(int offerIndex, Map<String, Integer> remaining) {
	}

	private final Catalog catalog;

	private final List<Offer> offersWorthApplying;

	/**
	 * For each offer index, every product that offer or a later one needs. Worked out once, from
	 * the last offer to the first. For fruit-war:
	 *
	 * <pre>
	 * 0 trio            →  apple, banana, orange
	 * 1 apple &amp; banana  →  apple, banana, orange
	 * 2 2 oranges       →  apple, banana, orange
	 * 3 3 apples        →  apple, banana
	 * 4 2 bananas       →  banana
	 * 5 (no offers left) →  nothing
	 * </pre>
	 *
	 * Once offer 2 is decided, no later offer can use an orange: the oranges left are paid at shelf
	 * price there and then, and drop out of the situation.
	 */
	private final List<Set<String>> usableFrom = new ArrayList<>();

	/**
	 * The notebook: every situation already worked out, with its cheapest answer. Part of it,
	 * midway through fruit-war with 3 apples, 3 bananas and 3 oranges:
	 *
	 * <pre>
	 * (4, {BANANA=3})                      →  0.50   2 bananas ×1
	 * (4, {BANANA=1})                      →  0.20   nothing
	 * (3, {APPLE=2, BANANA=2})             →  0.90   2 bananas ×1
	 * (2, {ORANGE=3})                      →  0.95   2 oranges ×1
	 * (1, {APPLE=3, BANANA=3, ORANGE=3})   →  1.85   apple &amp; banana ×3, 2 oranges ×1
	 * </pre>
	 *
	 * The entries for offer 3 carry no oranges, so routes that differ only in how many oranges
	 * went into offer 2 land on the same line and reuse it. That cart writes 23 answers and reuses
	 * 14; with 99 of each fruit, about 5,000 answers are reused millions of times.
	 */
	private final Map<Situation, OfferCombination> solved = new HashMap<>();

	BestPrice(Catalog catalog) {
		this.catalog = catalog;
		this.offersWorthApplying = catalog.activeOffers()
			.stream()
			.filter(offer -> offer.isWorthItFor(catalog))
			.toList();

		// Walk the offers backwards, collecting what each needs, so every step knows what the
		// steps after it can still use.
		Set<String> products = new HashSet<>();
		for (int i = offersWorthApplying.size() - 1; i >= 0; i--) {
			products.addAll(offersWorthApplying.get(i).requiredItems().keySet());
			usableFrom.add(0, Set.copyOf(products));
		}
		usableFrom.add(Set.of());
	}

	/**
	 * The cheapest way to buy the basket with this week's offers. Products no offer needs, such as
	 * the bread in fruit-war, are paid at shelf price straight away; the search only ever sees the
	 * rest.
	 */
	OfferCombination cheapestCombinationFor(Map<String, Integer> basket) {
		OfferCombination best = cheapestUsingOffersFrom(0, only(basket, usableFrom.get(0)));
		Money noOfferNeedsThem = shelfPriceOf(without(basket, usableFrom.get(0)));
		return new OfferCombination(best.total().plus(noOfferNeedsThem), best.offersUsage());
	}

	/**
	 * The cheapest way to buy the basket when only the offers from {@code offerIndex} onwards may
	 * be used. The basket holds only products those offers can use.
	 *
	 * <p>With "3 apples for 0.60" as offer 0, "apple &amp; banana for 0.30" as offer 1, and a basket
	 * of 3 apples and 3 bananas: offer 0 is tried 0 times, leaving offer 1 to be used three times
	 * for 0.90, and 1 time, leaving 3 bananas that offer 1 cannot use, for 1.20. The first is kept.
	 */
	private OfferCombination cheapestUsingOffersFrom(int offerIndex, Map<String, Integer> basket) {
		// No offers left to decide. The basket is empty by now: every product was paid for as soon
		// as no later offer could use it.
		if (offerIndex == offersWorthApplying.size()) {
			return new OfferCombination(shelfPriceOf(basket), Map.of());
		}

		// Already in the notebook: another route got here first.
		Situation situation = new Situation(offerIndex, basket);
		OfferCombination known = solved.get(situation);
		if (known != null) {
			return known;
		}

		Offer offer = offersWorthApplying.get(offerIndex);
		Set<String> usableLater = usableFrom.get(offerIndex + 1);
		OfferCombination best = null;
		Map<String, Integer> remaining = basket;

		// Use this offer 0 times, then 1, then 2… for as long as it still fits. Each time, the next
		// offers decide what to do with what remains. Counting in a loop rather than in a recursive
		// call is what keeps the recursion one level per offer, however large the cart.
		for (int times = 0;; times++) {
			// What no later offer can use is paid at shelf price now, and left out of what is
			// passed on. After "2 oranges", the oranges go here, which is what lets the next
			// situation be the same whatever this loop did with them.
			Money paidNow = shelfPriceOf(without(remaining, usableLater));
			OfferCombination bestForRemaining = cheapestUsingOffersFrom(offerIndex + 1,
					only(remaining, usableLater));

			Money total = offer.price().times(times).plus(paidNow).plus(bestForRemaining.total());
			if (best == null || total.compareTo(best.total()) < 0) {
				best = new OfferCombination(total,
						offersUsageWith(offer, times, bestForRemaining.offersUsage()));
			}

			// Stop when there is not enough remaining for one more use; otherwise take its items out.
			if (!offer.fitsIn(remaining)) {
				break;
			}
			remaining = offer.removeFrom(remaining);
		}

		// Written down so the next route that reaches this situation does not work it out again.
		solved.put(situation, best);
		return best;
	}

	/**
	 * The part of the basket made of these products, e.g. {@code {BANANA=37}} out of
	 * {@code {APPLE=12, BANANA=37}} for bananas only. It cannot be changed afterwards, because it
	 * becomes part of a notebook label, and a label that changed would never be found again.
	 */
	private static Map<String, Integer> only(Map<String, Integer> basket, Set<String> products) {
		Map<String, Integer> kept = new HashMap<>(basket);
		kept.keySet().retainAll(products);
		return Map.copyOf(kept);
	}

	/** The rest of the basket: everything except these products. */
	private static Map<String, Integer> without(Map<String, Integer> basket, Set<String> products) {
		Map<String, Integer> kept = new HashMap<>(basket);
		kept.keySet().removeAll(products);
		return kept;
	}

	/**
	 * The offers used by one option: this offer {@code times} times, followed by whatever the
	 * rest of the search used. For example, the bundle twice followed by {@code {3 apples=1}}
	 * gives {@code {bundle=2, 3 apples=1}}.
	 */
	private static Map<Offer, Integer> offersUsageWith(Offer offer, int times, Map<Offer, Integer> rest) {
		// An offer used 0 times is not a discount, so it is left off rather than listed as 0.
		if (times == 0) {
			return rest;
		}
		// Linked, so the discounts come out in the order of this week's offers and the receipt
		// prints them the same way every time.
		Map<Offer, Integer> offersUsage = new LinkedHashMap<>();
		offersUsage.put(offer, times);
		offersUsage.putAll(rest);
		return Collections.unmodifiableMap(offersUsage);
	}

	private Money shelfPriceOf(Map<String, Integer> basket) {
		Money total = Money.ZERO;
		for (Map.Entry<String, Integer> line : basket.entrySet()) {
			total = total.plus(catalog.require(line.getKey()).unitPrice().times(line.getValue()));
		}
		return total;
	}
}
