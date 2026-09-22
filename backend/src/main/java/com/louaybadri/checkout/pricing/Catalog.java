package com.louaybadri.checkout.pricing;

import java.util.Optional;

/**
 * What the supermarket sells today, and this week's offers. The pricing code only reads it, and
 * does not care whether it comes from a database, a file or a test.
 */
public interface Catalog {

	Optional<Product> findBySku(String sku);

	/**
	 * The active offer on a product, if it has one.
	 */
	Optional<Offer> findOfferFor(String sku);
}
