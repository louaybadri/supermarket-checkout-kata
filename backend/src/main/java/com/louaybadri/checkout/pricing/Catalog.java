package com.louaybadri.checkout.pricing;

import java.util.Optional;

/**
 * What the supermarket sells today. The pricing code only reads it, and does not care whether
 * it comes from a database, a file or a test.
 */
@FunctionalInterface
public interface Catalog {

	Optional<Product> findBySku(String sku);
}
