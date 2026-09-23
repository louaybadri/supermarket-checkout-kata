package com.louaybadri.checkout.pricing;

import java.util.List;
import java.util.Optional;

/**
 * What the supermarket sells today, and the offers running this week. The pricing code only
 * reads it, and does not care whether it comes from a database, a file or a test.
 */
public interface Catalog {

	Optional<Product> findBySku(String sku);

	/**
	 * Everything on the shelf.
	 */
	List<Product> products();

	/**
	 * Every offer running this week. An offer can span several products, so it cannot be filed
	 * under a single sku.
	 */
	List<Offer> activeOffers();

	/**
	 * The product, or a rejection naming the sku that is not sold.
	 */
	default Product require(String sku) {
		return findBySku(sku).orElseThrow(() -> new UnknownProductException(sku));
	}
}
