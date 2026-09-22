package com.louaybadri.checkout.pricing;

/**
 * This week's deal on one product: buy {@code quantity} of it and pay {@code bundlePrice}
 * instead of the unit price for each.
 */
public record Offer(String sku, int quantity, Money bundlePrice) {

	public Offer {
		if (sku == null || sku.isBlank()) {
			throw new IllegalArgumentException("An offer needs a sku");
		}
		if (quantity < 1) {
			throw new IllegalArgumentException(
					"An offer needs a quantity of at least 1, but was " + quantity);
		}
	}

	/**
	 * How many times this offer fits into {@code cartQuantity} items.
	 */
	public int timesItFitsIn(int cartQuantity) {
		return cartQuantity / quantity;
	}

	/**
	 * The items left over once the offer has been applied as often as it fits.
	 */
	public int leftOverIn(int cartQuantity) {
		return cartQuantity % quantity;
	}
}
