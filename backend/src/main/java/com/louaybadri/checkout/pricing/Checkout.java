package com.louaybadri.checkout.pricing;

import java.util.Map;

/**
 * Prices a whole cart against the catalog: the items are counted, the cheapest combination of
 * this week's offers is chosen, and whatever no offer covers is charged at shelf price.
 */
public class Checkout {

	private final Catalog catalog;

	public Checkout(Catalog catalog) {
		this.catalog = catalog;
	}

	public Money total(Cart cart) {
		Map<String, Integer> basket = cart.quantityBySku();
		basket.keySet().forEach(catalog::require);
		return new BestPrice(catalog).chooseFor(basket).total();
	}
}
