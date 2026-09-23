package com.louaybadri.checkout.pricing;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A deal running this week: buy this set of items together and pay {@code price} for them
 * instead of the shelf price. The set may be several of one product ("2 apples for 0.45") or
 * different products together ("an apple and a banana for 0.40").
 */
public record Offer(String name, Map<String, Integer> requiredItems, Money price) {

	public Offer {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("An offer needs a name");
		}
		if (requiredItems == null || requiredItems.isEmpty()) {
			throw new IllegalArgumentException("An offer needs at least one item: " + name);
		}
		requiredItems.forEach((sku, quantity) -> {
			if (quantity < 1) {
				throw new IllegalArgumentException(
						"An offer needs at least one of each item, but " + name + " asks for "
								+ quantity + " of " + sku);
			}
		});
		requiredItems = Map.copyOf(requiredItems);
	}

	/**
	 * Whether the basket still holds everything this offer needs.
	 */
	public boolean fitsIn(Map<String, Integer> basket) {
		return requiredItems.entrySet()
			.stream()
			.allMatch(needed -> basket.getOrDefault(needed.getKey(), 0) >= needed.getValue());
	}

	/**
	 * What is left of the basket once this offer has been used once. The basket itself is not
	 * touched.
	 */
	public Map<String, Integer> removeFrom(Map<String, Integer> basket) {
		Map<String, Integer> left = new LinkedHashMap<>(basket);
		requiredItems.forEach((sku, needed) -> {
			int remaining = left.get(sku) - needed;
			if (remaining == 0) {
				left.remove(sku);
			}
			else {
				left.put(sku, remaining);
			}
		});
		return left;
	}

	/**
	 * What the same items would cost at shelf price.
	 */
	public Money shelfPrice(Catalog catalog) {
		Money total = Money.ZERO;
		for (Map.Entry<String, Integer> needed : requiredItems.entrySet()) {
			Product product = catalog.require(needed.getKey());
			total = total.plus(product.unitPrice().times(needed.getValue()));
		}
		return total;
	}

	/**
	 * Whether the deal is actually a deal. A miskeyed offer that costs the customer more than
	 * the shelf price, or exactly the same, is ignored rather than charged.
	 */
	public boolean isWorthItFor(Catalog catalog) {
		return price.compareTo(shelfPrice(catalog)) < 0;
	}
}
