package com.louaybadri.checkout.pricing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A catalog held in a map, so the pricing tests need no database and no Spring.
 */
final class StubCatalog implements Catalog {

	private final Map<String, Product> products = new LinkedHashMap<>();

	private final Map<String, Offer> offers = new LinkedHashMap<>();

	StubCatalog selling(Product... sold) {
		List.of(sold).forEach(product -> products.put(product.sku(), product));
		return this;
	}

	StubCatalog offering(Offer... thisWeek) {
		List.of(thisWeek).forEach(offer -> offers.put(offer.sku(), offer));
		return this;
	}

	@Override
	public Optional<Product> findBySku(String sku) {
		return Optional.ofNullable(products.get(sku));
	}

	@Override
	public Optional<Offer> findOfferFor(String sku) {
		return Optional.ofNullable(offers.get(sku));
	}
}
