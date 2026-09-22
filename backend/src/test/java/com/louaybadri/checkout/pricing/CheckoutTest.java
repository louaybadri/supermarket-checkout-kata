package com.louaybadri.checkout.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class CheckoutTest {

	private static final Product APPLE = new Product("APPLE", "Apple", Money.ofCents(30));
	private static final Product BANANA = new Product("BANANA", "Banana", Money.ofCents(20));

	private final Checkout checkout = new Checkout(catalogOf(APPLE, BANANA));

	@Test
	void anEmptyCartCostsNothing() {
		assertThat(checkout.total(Cart.empty())).isEqualTo(Money.ZERO);
	}

	@Test
	void oneItemCostsItsUnitPrice() {
		assertThat(checkout.total(cartOf("APPLE"))).isEqualTo(Money.ofCents(30));
	}

	@Test
	void severalUnitsOfTheSameItemAddUp() {
		assertThat(checkout.total(cartOf("APPLE", "APPLE", "APPLE"))).isEqualTo(Money.ofCents(90));
	}

	@Test
	void differentItemsAddUp() {
		assertThat(checkout.total(cartOf("APPLE", "BANANA"))).isEqualTo(Money.ofCents(50));
	}

	@Test
	void theOrderOfTheItemsDoesNotMatter() {
		Money scannedOneWay = checkout.total(cartOf("APPLE", "BANANA", "APPLE"));
		Money scannedTheOther = checkout.total(cartOf("BANANA", "APPLE", "APPLE"));

		assertThat(scannedOneWay).isEqualTo(scannedTheOther).isEqualTo(Money.ofCents(80));
	}

	@Test
	void refusesAnItemTheSupermarketDoesNotSell() {
		assertThatThrownBy(() -> checkout.total(cartOf("UNICORN")))
			.isInstanceOf(UnknownProductException.class)
			.hasMessageContaining("UNICORN");
	}

	@Test
	void theSameItemScannedInSeveralGoesIsCountedOnce() {
		Cart inOneGo = new Cart(List.of(new Cart.Item("APPLE", 2)));
		Cart inTwoGoes = new Cart(List.of(new Cart.Item("APPLE", 1), new Cart.Item("APPLE", 1)));

		assertThat(checkout.total(inTwoGoes)).isEqualTo(checkout.total(inOneGo));
	}

	private static Cart cartOf(String... skus) {
		return new Cart(List.of(skus).stream().map(sku -> new Cart.Item(sku, 1)).toList());
	}

	private static Catalog catalogOf(Product... products) {
		return new StubCatalog().selling(products);
	}
}
