package com.louaybadri.checkout.pricing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CartTest {

	@Test
	void refusesAQuantityOfZero() {
		assertThatThrownBy(() -> new Cart.Item("APPLE", 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("at least 1");
	}

	@Test
	void refusesANegativeQuantity() {
		assertThatThrownBy(() -> new Cart.Item("APPLE", -2))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("-2");
	}

	@Test
	void refusesAnItemWithoutASku() {
		assertThatThrownBy(() -> new Cart.Item("  ", 1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("sku");
	}
}
