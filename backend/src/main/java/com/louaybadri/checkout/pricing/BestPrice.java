package com.louaybadri.checkout.pricing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * <p>One instance prices one cart.
 */
class BestPrice {

	/**
	 * A way to buy a basket: what it costs, and how many times each offer is used, in the order
	 * of this week's offers.
	 */
	record OfferCombination(Money total, Map<Offer, Integer> offersUsage) {
	}

	private final Catalog catalog;

	private final List<Offer> offersWorthApplying;

	BestPrice(Catalog catalog) {
		this.catalog = catalog;
		this.offersWorthApplying = catalog.activeOffers()
			.stream()
			.filter(offer -> offer.isWorthItFor(catalog))
			.toList();
	}

	/**
	 * The cheapest way to buy the basket with this week's offers. The search starts at the first
	 * offer, with every offer still to be decided.
	 */
	OfferCombination cheapestCombinationFor(Map<String, Integer> basket) {
		return cheapestUsingOffersFrom(0, basket);
	}

	/**
	 * The cheapest way to buy the basket when only the offers from {@code offerIndex} onwards may
	 * be used. {@code cheapestUsingOffersFrom(0, basket)} is the full answer.
	 *
	 * <p>With "3 apples for 0.60" as offer 0, "apple &amp; banana for 0.30" as offer 1, and a basket
	 * of 3 apples and 3 bananas: offer 0 is tried 0 times, leaving offer 1 to be used three times
	 * for 0.90, and 1 time, leaving 3 bananas that offer 1 cannot use, for 1.20. The first is kept.
	 */
	private OfferCombination cheapestUsingOffersFrom(int offerIndex, Map<String, Integer> basket) {
		// No offers left to decide: whatever remains is paid at shelf price.
		if (offerIndex == offersWorthApplying.size()) {
			return new OfferCombination(shelfPriceOf(basket), Map.of());
		}

		Offer offer = offersWorthApplying.get(offerIndex);
		OfferCombination best = null;
		Map<String, Integer> remaining = basket;

		// Use this offer 0 times, then 1, then 2… for as long as it still fits. Each time, the next
		// offers decide what to do with what remains. Counting in a loop rather than in a recursive
		// call is what keeps the recursion one level per offer, however large the cart.
		for (int times = 0;; times++) {
			OfferCombination bestForRemaining = cheapestUsingOffersFrom(offerIndex + 1, remaining);
			Money total = offer.price().times(times).plus(bestForRemaining.total());
			if (best == null || total.compareTo(best.total()) < 0) {
				best = new OfferCombination(total, offersUsageWith(offer, times, bestForRemaining.offersUsage()));
			}

			// Stop when there is not enough remaining for one more use; otherwise take its items out.
			if (!offer.fitsIn(remaining)) {
				break;
			}
			remaining = offer.removeFrom(remaining);
		}

		return best;
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
