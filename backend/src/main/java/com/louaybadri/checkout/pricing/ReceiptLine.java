package com.louaybadri.checkout.pricing;

/**
 * One product on the bill, at shelf price. Any discount it took part in is printed under the
 * lines, because an offer can span several products.
 */
public record ReceiptLine(String sku, String name, int quantity, Money unitPrice) {

	public Money lineTotal() {
		return unitPrice.times(quantity);
	}
}
