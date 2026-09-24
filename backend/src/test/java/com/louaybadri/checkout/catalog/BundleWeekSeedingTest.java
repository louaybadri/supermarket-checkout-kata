package com.louaybadri.checkout.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.louaybadri.checkout.pricing.AppliedOffer;
import com.louaybadri.checkout.pricing.Cart;
import com.louaybadri.checkout.pricing.Catalog;
import com.louaybadri.checkout.pricing.Checkout;
import com.louaybadri.checkout.pricing.Money;
import com.louaybadri.checkout.pricing.Offer;

/**
 * Switching the preset switches the week's deals, with no change to any code.
 */
@SpringBootTest
@TestPropertySource(properties = "catalog.active-preset=bundle-week")
class BundleWeekSeedingTest {

	@Autowired
	private Catalog catalog;

	@Autowired
	private Checkout checkout;

	@Test
	void runsTheOffersOfThatWeekInsteadOfTheDefaultOnes() {
		assertThat(catalog.activeOffers()).extracting(Offer::name)
			.containsExactlyInAnyOrder("Apple & banana for 0.40", "3 apples for 0.60",
					"Bread & milk for 1.80");
	}

	@Test
	void combinesTwoOfThatWeeksOffersWhenThatIsCheapest() {
		Cart cart = new Cart(List.of(new Cart.Item("APPLE", 4), new Cart.Item("BANANA", 1)));

		// Shelf price is 1.40. Three apples for 0.60 with the rest at shelf price is 1.10.
		// Three apples for 0.60 plus the apple and banana bundle for 0.40 is 1.00.
		assertThat(checkout.total(cart)).isEqualTo(Money.ofCents(100));
		assertThat(checkout.receiptFor(cart).discounts()).extracting(AppliedOffer::name)
			.containsExactlyInAnyOrder("3 apples for 0.60", "Apple & banana for 0.40");
	}
}
