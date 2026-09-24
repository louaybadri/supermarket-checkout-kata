package com.louaybadri.checkout.pricing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The catalog as one checkout sees it: each product is fetched the first time it is asked for
 * and remembered after that. The search asks for the same prices thousands of times, and behind
 * the real catalog every ask is a database query.
 *
 * <p>It lives for one checkout only, so a price changed in the catalog is picked up by the next
 * cart.
 */
final class RememberingCatalog implements Catalog {

	private final Catalog source;

	private final Map<String, Optional<Product>> products = new HashMap<>();

	RememberingCatalog(Catalog source) {
		this.source = source;
	}

	@Override
	public Optional<Product> findBySku(String sku) {
		return products.computeIfAbsent(sku, source::findBySku);
	}

	@Override
	public List<Product> products() {
		return source.products();
	}

	@Override
	public List<Offer> activeOffers() {
		return source.activeOffers();
	}
}
