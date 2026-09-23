package com.louaybadri.checkout.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.louaybadri.checkout.pricing.Cart;
import com.louaybadri.checkout.pricing.Catalog;
import com.louaybadri.checkout.pricing.Checkout;
import com.louaybadri.checkout.pricing.Money;
import com.louaybadri.checkout.pricing.Offer;
import com.louaybadri.checkout.pricing.Product;

import java.util.List;

/**
 * The catalog file reaches the database, and the checkout prices against it.
 */
@SpringBootTest
@TestPropertySource(properties = "catalog.active-preset=classic")
class CatalogSeedingTest {

	@Autowired
	private Catalog catalog;

	@Autowired
	private Checkout checkout;

	@Test
	void sellsTheProductsListedInTheFile() {
		assertThat(catalog.findBySku("APPLE"))
			.contains(new Product("APPLE", "Apple", Money.ofCents(30)));
		assertThat(catalog.findBySku("CHEESE"))
			.contains(new Product("CHEESE", "Cheese", Money.ofCents(250)));
	}

	@Test
	void doesNotSellSomethingThatIsNotInTheFile() {
		assertThat(catalog.findBySku("UNICORN")).isEmpty();
	}

	@Test
	void runsTheOffersOfTheActivePreset() {
		assertThat(catalog.activeOffers())
			.containsExactly(new Offer("2 apples for 0.45", Map.of("APPLE", 2), Money.ofCents(45)));
	}

	@Test
	void pricesACartAgainstTheSeededCatalog() {
		Cart twoApplesAndAMilk = new Cart(
				List.of(new Cart.Item("APPLE", 2), new Cart.Item("MILK", 1)));

		assertThat(checkout.total(twoApplesAndAMilk)).isEqualTo(Money.ofCents(135));
	}
}
