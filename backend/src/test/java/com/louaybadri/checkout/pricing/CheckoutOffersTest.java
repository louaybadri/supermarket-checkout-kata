package com.louaybadri.checkout.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CheckoutOffersTest {

	private static final Product APPLE = new Product("APPLE", "Apple", Money.ofCents(30));

	private static final Product BANANA = new Product("BANANA", "Banana", Money.ofCents(20));

	/** This week: two apples for 0.45 instead of 0.60. */
	private static final Offer TWO_APPLES = new Offer("APPLE", 2, Money.ofCents(45));

	private final Checkout checkout = new Checkout(
			new StubCatalog().selling(APPLE, BANANA).offering(TWO_APPLES));

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
	void anOfferDearerThanTheShelfPriceIsIgnored() {
		Product cheese = new Product("CHEESE", "Cheese", Money.ofCents(100));
		Checkout misconfigured = new Checkout(new StubCatalog().selling(cheese)
			.offering(new Offer("CHEESE", 2, Money.ofCents(250))));

		assertThat(misconfigured.total(cartOf("CHEESE", "CHEESE"))).isEqualTo(Money.ofCents(200));
	}

	@Test
	void anOfferThatSavesNothingIsIgnored() {
		Product cheese = new Product("CHEESE", "Cheese", Money.ofCents(100));
		Checkout pointless = new Checkout(new StubCatalog().selling(cheese)
			.offering(new Offer("CHEESE", 2, Money.ofCents(200))));

		assertThat(pointless.total(cartOf("CHEESE", "CHEESE"))).isEqualTo(Money.ofCents(200));
	}

	@Test
	void theOfferAppliesWhateverTheOrderOfTheCart() {
		assertThat(checkout.total(cartOf("APPLE", "BANANA", "APPLE")))
			.isEqualTo(checkout.total(cartOf("BANANA", "APPLE", "APPLE")))
			.isEqualTo(Money.ofCents(65));
	}

	private static Cart cartOf(String... skus) {
		return new Cart(List.of(skus).stream().map(sku -> new Cart.Item(sku, 1)).toList());
	}
}
