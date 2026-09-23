package com.louaybadri.checkout.pricing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A catalog held in memory, so the pricing tests need no database and no Spring.
 */
final class StubCatalog implements Catalog {

	private final Map<String, Product> products = new LinkedHashMap<>();

	private final List<Offer> offers = new ArrayList<>();

	StubCatalog selling(Product... sold) {
		List.of(sold).forEach(product -> products.put(product.sku(), product));
		return this;
	}

	StubCatalog offering(Offer... thisWeek) {
		offers.addAll(List.of(thisWeek));
		return this;
	}

	@Override
	public Optional<Product> findBySku(String sku) {
		return Optional.ofNullable(products.get(sku));
	}

	@Override
	public List<Offer> activeOffers() {
		return List.copyOf(offers);
	}
}
