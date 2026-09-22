package com.louaybadri.checkout.pricing;

/**
 * The cart names something the supermarket does not sell. This is bad input rather than a
 * failure, so the API turns it into a 400.
 */
public class UnknownProductException extends IllegalArgumentException {

	private final String sku;

	public UnknownProductException(String sku) {
		super("Unknown sku: " + sku);
		this.sku = sku;
	}

	public String sku() {
		return sku;
	}
}
