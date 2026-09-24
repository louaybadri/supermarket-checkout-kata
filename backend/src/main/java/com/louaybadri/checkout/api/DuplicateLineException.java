package com.louaybadri.checkout.api;

/**
 * The cart names the same product on more than one line. The API takes one line per product,
 * holding how many of it the shopper wants, so a second line is bad input rather than something
 * to add up: added up, two lines of 99 would get past the limit each line is held to.
 */
class DuplicateLineException extends RuntimeException {

	private final String sku;

	DuplicateLineException(String sku) {
		super(sku + " appears on more than one line");
		this.sku = sku;
	}

	String sku() {
		return sku;
	}
}
