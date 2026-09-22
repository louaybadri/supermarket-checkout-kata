package com.louaybadri.checkout.pricing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the customer brings to the till: items in the order they were scanned. The same sku may
 * appear several times, and the order carries no meaning for the price.
 */
public record Cart(List<Item> items) {

	public record Item(String sku, int quantity) {

		public Item {
			if (sku == null || sku.isBlank()) {
				throw new IllegalArgumentException("An item needs a sku");
			}
			if (quantity <= 0) {
				throw new IllegalArgumentException(
						"An item needs a quantity of at least 1, but was " + quantity);
			}
		}
	}

	public Cart {
		items = List.copyOf(items);
	}

	public static Cart empty() {
		return new Cart(List.of());
	}

	/**
	 * The cart seen as a count per sku, which is what pricing works on.
	 */
	public Map<String, Integer> quantityBySku() {
		Map<String, Integer> quantities = new LinkedHashMap<>();
		for (Item item : items) {
			quantities.merge(item.sku(), item.quantity(), Integer::sum);
		}
		return quantities;
	}
}
