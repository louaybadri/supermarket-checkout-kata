package com.louaybadri.checkout.pricing;

import java.util.Map;

/**
 * Prices a whole cart against the catalog.
 */
public class Checkout {

	private final Catalog catalog;

	public Checkout(Catalog catalog) {
		this.catalog = catalog;
	}

	public Money total(Cart cart) {
		Money total = Money.ZERO;
		for (Map.Entry<String, Integer> line : cart.quantityBySku().entrySet()) {
			Product product = productFor(line.getKey());
			total = total.plus(product.unitPrice().times(line.getValue()));
		}
		return total;
	}

	private Product productFor(String sku) {
		return catalog.findBySku(sku)
			.orElseThrow(() -> new IllegalArgumentException("Unknown sku: " + sku));
	}
}
