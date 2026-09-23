package com.louaybadri.checkout.pricing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chooses which offers to apply so that the customer pays as little as possible.
 *
 * <p>Applying the best-looking offer first is not good enough once offers compete for the same
 * items: with "3 apples for 0.60" and "an apple and a banana for 0.30", a basket of 3 apples and
 * 3 bananas costs 1.20 that way and 0.90 if the bundle is used three times. So every option is
 * tried — each offer that fits, and the option of applying nothing more — and the cheapest wins.
 *
 * <p>The answer for a basket already seen is remembered, which is what keeps the search from
 * re-solving the same leftovers down every branch.
 *
 * <p>One instance prices one cart.
 */
class BestPrice {

	/**
	 * A way to buy a basket: what it costs, and the offers it uses.
	 */
	record Choice(Money total, List<Offer> offers) {
	}

	private final Catalog catalog;

	private final List<Offer> worthwhile;

	private final Map<Map<String, Integer>, Choice> solved = new HashMap<>();

	BestPrice(Catalog catalog) {
		this.catalog = catalog;
		this.worthwhile = catalog.activeOffers().stream().filter(offer -> offer.isWorthItFor(catalog)).toList();
	}

	Choice chooseFor(Map<String, Integer> basket) {
		Choice known = solved.get(basket);
		if (known != null) {
			return known;
		}

		Choice best = new Choice(shelfPriceOf(basket), List.of());
		for (Offer offer : worthwhile) {
			if (!offer.fitsIn(basket)) {
				continue;
			}
			Choice rest = chooseFor(offer.removeFrom(basket));
			Money total = offer.price().plus(rest.total());
			if (total.compareTo(best.total()) < 0) {
				best = new Choice(total, with(offer, rest.offers()));
			}
		}

		solved.put(Map.copyOf(basket), best);
		return best;
	}

	private static List<Offer> with(Offer offer, List<Offer> rest) {
		List<Offer> offers = new ArrayList<>(rest.size() + 1);
		offers.add(offer);
		offers.addAll(rest);
		return List.copyOf(offers);
	}

	private Money shelfPriceOf(Map<String, Integer> basket) {
		Money total = Money.ZERO;
		for (Map.Entry<String, Integer> line : basket.entrySet()) {
			total = total.plus(catalog.require(line.getKey()).unitPrice().times(line.getValue()));
		}
		return total;
	}
}
