package com.louaybadri.checkout.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CheckoutOffersTest {

	private static final Product APPLE = new Product("APPLE", "Apple", Money.ofCents(30));

	private static final Product BANANA = new Product("BANANA", "Banana", Money.ofCents(20));

	/** This week: two apples for 0.45 instead of 0.60. */
	private static final Offer TWO_APPLES = new Offer("2 apples for 0.45", Map.of("APPLE", 2),
			Money.ofCents(45));

	/** And an apple with a banana for 0.40 instead of 0.50. */
	private static final Offer BUNDLE = new Offer("Apple & banana", Map.of("APPLE", 1, "BANANA", 1),
			Money.ofCents(40));

	private final Checkout checkout = new Checkout(
			new StubCatalog().selling(APPLE, BANANA).offering(TWO_APPLES));

	private final Checkout withBundle = new Checkout(
			new StubCatalog().selling(APPLE, BANANA).offering(TWO_APPLES, BUNDLE));

	@Test
	void oneAppleCostsTheUnitPrice() {
		assertThat(checkout.total(cartOf("APPLE"))).isEqualTo(Money.ofCents(30));
	}

	@Test
	void twoApplesCostTheOfferPrice() {
		assertThat(checkout.total(cartOf("APPLE", "APPLE"))).isEqualTo(Money.ofCents(45));
	}

	@Test
	void threeApplesAreTheOfferPlusOneAtTheUnitPrice() {
		assertThat(checkout.total(cartOf("APPLE", "APPLE", "APPLE"))).isEqualTo(Money.ofCents(75));
	}

	@Test
	void fiveApplesAreTheOfferTwicePlusOneAtTheUnitPrice() {
		assertThat(checkout.total(cartOf("APPLE", "APPLE", "APPLE", "APPLE", "APPLE")))
			.isEqualTo(Money.ofCents(120));
	}

	@Test
	void aProductWithoutAnOfferIsChargedAtTheUnitPrice() {
		assertThat(checkout.total(cartOf("BANANA", "BANANA"))).isEqualTo(Money.ofCents(40));
	}

	@Test
	void theOfferAppliesWhateverTheOrderOfTheCart() {
		assertThat(checkout.total(cartOf("APPLE", "BANANA", "APPLE")))
			.isEqualTo(checkout.total(cartOf("BANANA", "APPLE", "APPLE")))
			.isEqualTo(Money.ofCents(65));
	}

	@Test
	void anOfferDearerThanTheShelfPriceIsIgnored() {
		Product cheese = new Product("CHEESE", "Cheese", Money.ofCents(100));
		Checkout misconfigured = new Checkout(new StubCatalog().selling(cheese)
			.offering(new Offer("2 cheese for 2.50", Map.of("CHEESE", 2), Money.ofCents(250))));

		assertThat(misconfigured.total(cartOf("CHEESE", "CHEESE"))).isEqualTo(Money.ofCents(200));
	}

	@Test
	void aBundleAcrossProductsIsApplied() {
		assertThat(withBundle.total(cartOf("APPLE", "BANANA"))).isEqualTo(Money.ofCents(40));
	}

	@Test
	void aBundleLeavesTheSpareItemAtShelfPrice() {
		Checkout bundleOnly = new Checkout(new StubCatalog().selling(APPLE, BANANA).offering(BUNDLE));

		// 0.40 for the bundle, and the spare apple at 0.30.
		assertThat(bundleOnly.total(cartOf("APPLE", "APPLE", "BANANA"))).isEqualTo(Money.ofCents(70));
	}

	@Test
	void theCheaperOfTwoCompetingOffersWins() {
		// Both fit: the bundle leaves an apple at 0.30 for 0.70, the apple deal leaves a banana
		// at 0.20 for 0.65.
		assertThat(withBundle.total(cartOf("APPLE", "APPLE", "BANANA"))).isEqualTo(Money.ofCents(65));
	}

	@Test
	void theCheapestCombinationWinsWhenOffersCompeteForTheSameItems() {
		Offer threeApples = new Offer("3 apples for 0.60", Map.of("APPLE", 3), Money.ofCents(60));
		Offer cheapBundle = new Offer("Apple & banana", Map.of("APPLE", 1, "BANANA", 1), Money.ofCents(30));
		Checkout competing = new Checkout(
				new StubCatalog().selling(APPLE, BANANA).offering(threeApples, cheapBundle));

		// Taking the bigger single saving first would cost 1.20; three bundles cost 0.90.
		assertThat(competing.total(cartOf("APPLE", "APPLE", "APPLE", "BANANA", "BANANA", "BANANA")))
			.isEqualTo(Money.ofCents(90));
	}

	private static Cart cartOf(String... skus) {
		return new Cart(List.of(skus).stream().map(sku -> new Cart.Item(sku, 1)).toList());
	}
}
