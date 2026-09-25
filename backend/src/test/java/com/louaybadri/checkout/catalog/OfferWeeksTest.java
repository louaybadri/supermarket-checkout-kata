package com.louaybadri.checkout.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.louaybadri.checkout.pricing.AppliedOffer;
import com.louaybadri.checkout.pricing.Cart;
import com.louaybadri.checkout.pricing.Checkout;
import com.louaybadri.checkout.pricing.Money;
import com.louaybadri.checkout.pricing.Receipt;

/**
 * The sample weeks in catalog.yml load, and each prices its worked example the way the comment
 * above it says. The expected totals were found by trying every combination by hand, outside this
 * code, so the tests do not just repeat what the search itself works out.
 */
class OfferWeeksTest {

	/** A cart from "SKU", quantity pairs, e.g. cart("APPLE", 3, "BANANA", 1). */
	static Cart cart(Object... skuThenQuantity) {
		List<Object> values = Arrays.asList(skuThenQuantity);
		List<Cart.Item> items = new ArrayList<>();
		for (int i = 0; i < values.size(); i += 2) {
			items.add(new Cart.Item((String) values.get(i), (Integer) values.get(i + 1)));
		}
		return new Cart(items);
	}

	@Nested
	@SpringBootTest
	@TestPropertySource(properties = "catalog.active-preset=fruit-war")
	class FruitWar {

		@Autowired
		private Checkout checkout;

		@Test
		void beatsTakingTheBiggestSavingFirst() {
			// The trio saves the most on its own, 0.30, but leaves an orange at 0.40: 1.00.
			// Apple & banana plus 2 oranges save 0.45 together: 0.85.
			Receipt receipt = checkout.receiptFor(cart("APPLE", 1, "BANANA", 1, "ORANGE", 2));

			assertThat(receipt.total()).isEqualTo(Money.ofCents(85));
			assertThat(receipt.discounts()).extracting(AppliedOffer::name, AppliedOffer::times)
				.containsExactlyInAnyOrder(tuple("Apple & banana for 0.30", 1), tuple("2 oranges for 0.55", 1));
		}

		@Test
		void usesThreeDifferentOffersInOneCart() {
			// Shelf 2.70. Best: the trio once, apple & banana twice, 2 oranges once: 1.75.
			Receipt receipt = checkout.receiptFor(cart("APPLE", 3, "BANANA", 3, "ORANGE", 3));

			assertThat(receipt.total()).isEqualTo(Money.ofCents(175));
			assertThat(receipt.discounts()).extracting(AppliedOffer::name, AppliedOffer::times)
				.containsExactlyInAnyOrder(tuple("Apple, banana & orange for 0.60", 1),
						tuple("Apple & banana for 0.30", 2), tuple("2 oranges for 0.55", 1));
		}

		@Test
		void pricesTheMostOfEachFruitACartMayHold() {
			// The cart that took 16 seconds before the search reused its answers (review #12).
			// The same total either way; now in a fraction of a second.
			assertThat(checkout.total(cart("APPLE", 99, "BANANA", 99, "ORANGE", 99)))
				.isEqualTo(Money.ofCents(5695));
		}
	}

	@Nested
	@SpringBootTest
	@TestPropertySource(properties = "catalog.active-preset=breakfast")
	class Breakfast {

		@Autowired
		private Checkout checkout;

		@Test
		void splitsTheCoffeesBetweenTwoDeals() {
			// Shelf 11.10. Best: the breakfast box, coffee & croissant, and 2 coffees: 8.80.
			Receipt receipt = checkout.receiptFor(
					cart("COFFEE", 3, "CROISSANT", 1, "EGG", 6, "BUTTER", 1, "BREAD", 1));

			assertThat(receipt.total()).isEqualTo(Money.ofCents(880));
			assertThat(receipt.discounts()).extracting(AppliedOffer::name)
				.containsExactlyInAnyOrder("Breakfast box for 3.40", "Coffee & croissant for 2.40",
						"2 coffees for 3.00");
		}
	}

	@Nested
	@SpringBootTest
	@TestPropertySource(properties = "catalog.active-preset=not-a-deal")
	class NotADeal {

		@Autowired
		private Checkout checkout;

		@Test
		void appliesOnlyTheOfferThatIsCheaperThanTheShelf() {
			// The pear deals cost more than, or exactly, the shelf price, so only the yoghurts
			// get a discount: 2 pears at 0.70 plus 3 yoghurts for 1.80 is 2.50.
			Receipt receipt = checkout.receiptFor(cart("PEAR", 2, "YOGHURT", 3));

			assertThat(receipt.total()).isEqualTo(Money.ofCents(250));
			assertThat(receipt.discounts()).extracting(AppliedOffer::name)
				.containsExactly("3 yoghurts for 1.80");
		}
	}

	@Nested
	@SpringBootTest
	@TestPropertySource(properties = "catalog.active-preset=crowded")
	class Crowded {

		@Autowired
		private Checkout checkout;

		@Test
		void doesNotStartWithTheBiggestDeal() {
			// "10 apples for 2.60" saves the most at once, but leaves 2 apples: 3.15.
			// Four lots of 3 for 0.75, or two lots of 6 for 1.50, both come to 3.00; which of
			// the two is printed does not matter, only that the big deal is left alone.
			Receipt receipt = checkout.receiptFor(cart("APPLE", 12));

			assertThat(receipt.total()).isEqualTo(Money.ofCents(300));
			assertThat(receipt.discounts()).extracting(AppliedOffer::name)
				.doesNotContain("10 apples for 2.60");
		}

		@Test
		void pricesTheMostApplesOneLineMayHold() {
			// Six offers on one product is the slowest case for the search; 99 apples still
			// come to 33 lots of 3 for 0.75.
			assertThat(checkout.total(cart("APPLE", 99))).isEqualTo(Money.ofCents(2475));
		}
	}
}
