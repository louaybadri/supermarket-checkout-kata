package com.louaybadri.checkout.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

	@Test
	void holdsAnAmountInWholeCents() {
		assertThat(Money.ofCents(30).cents()).isEqualTo(30);
	}

	@Test
	void addsTwoAmounts() {
		assertThat(Money.ofCents(30).plus(Money.ofCents(15))).isEqualTo(Money.ofCents(45));
	}

	@Test
	void subtractsTwoAmounts() {
		assertThat(Money.ofCents(45).minus(Money.ofCents(30))).isEqualTo(Money.ofCents(15));
	}

	@Test
	void multipliesByAQuantity() {
		assertThat(Money.ofCents(30).times(3)).isEqualTo(Money.ofCents(90));
	}

	@Test
	void multipliedByZeroIsNothing() {
		assertThat(Money.ofCents(30).times(0)).isEqualTo(Money.ZERO);
	}

	@Test
	void comparesAmounts() {
		assertThat(Money.ofCents(45)).isGreaterThan(Money.ofCents(30));
		assertThat(Money.ofCents(30)).isEqualByComparingTo(Money.ofCents(30));
	}

	@Test
	void refusesANegativeAmount() {
		assertThatThrownBy(() -> Money.ofCents(-1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("-1");
	}

	@Test
	void refusesToSubtractMoreThanItHolds() {
		assertThatThrownBy(() -> Money.ofCents(30).minus(Money.ofCents(45)))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void readsAsEurosAndCents() {
		assertThat(Money.ofCents(45)).hasToString("0.45");
		assertThat(Money.ofCents(120)).hasToString("1.20");
	}
}
