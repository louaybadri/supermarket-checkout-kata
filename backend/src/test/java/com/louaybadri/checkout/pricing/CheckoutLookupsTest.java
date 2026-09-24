package com.louaybadri.checkout.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * In the running application every product lookup is a database query, so the search must not
 * ask the catalog again for a price it has already been given.
 */
class CheckoutLookupsTest {

	private static final Product APPLE = new Product("APPLE", "Apple", Money.ofCents(30));

	private static final Product BANANA = new Product("BANANA", "Banana", Money.ofCents(20));

	private static final Product BREAD = new Product("BREAD", "Bread", Money.ofCents(110));

	private static final Product MILK = new Product("MILK", "Milk", Money.ofCents(90));

	private final CountingCatalog catalog = new CountingCatalog(new StubCatalog().selling(APPLE, BANANA, BREAD, MILK)
		.offering(new Offer("Apple & banana for 0.40", Map.of("APPLE", 1, "BANANA", 1), Money.ofCents(40)),
				new Offer("3 apples for 0.60", Map.of("APPLE", 3), Money.ofCents(60)),
				new Offer("Bread & milk for 1.80", Map.of("BREAD", 1, "MILK", 1), Money.ofCents(180))));

	@Test
	void looksEachProductUpOnceHoweverLongTheSearch() {
		Cart cart = new Cart(List.of(new Cart.Item("APPLE", 20), new Cart.Item("BANANA", 20),
				new Cart.Item("BREAD", 20), new Cart.Item("MILK", 20)));

		new Checkout(catalog).ring(cart);

		assertThat(catalog.lookups).containsOnly(Map.entry("APPLE", 1), Map.entry("BANANA", 1),
				Map.entry("BREAD", 1), Map.entry("MILK", 1));
	}

	/**
	 * Counts how often each sku is asked for, the way a database would count queries.
	 */
	private static final class CountingCatalog implements Catalog {

		private final Catalog shop;

		private final Map<String, Integer> lookups = new HashMap<>();

		CountingCatalog(Catalog shop) {
			this.shop = shop;
		}

		@Override
		public Optional<Product> findBySku(String sku) {
			lookups.merge(sku, 1, Integer::sum);
			return shop.findBySku(sku);
		}

		@Override
		public List<Product> products() {
			return shop.products();
		}

		@Override
		public List<Offer> activeOffers() {
			return shop.activeOffers();
		}
	}
}
