package com.louaybadri.checkout.pricing;

/**
 * An amount of money in whole cents. Prices never touch floating point, so 0.30 + 0.15 is
 * exactly 0.45 and never 0.44999999999999996.
 */
public record Money(long cents) implements Comparable<Money> {

	public static final Money ZERO = new Money(0);

	public Money {
		if (cents < 0) {
			throw new IllegalArgumentException("An amount of money cannot be negative: " + cents);
		}
	}

	public static Money ofCents(long cents) {
		return new Money(cents);
	}

	public Money plus(Money other) {
		return new Money(this.cents + other.cents);
	}

	/**
	 * @throws IllegalArgumentException if the result would be negative, which in this domain
	 * means a discount larger than the price it applies to.
	 */
	public Money minus(Money other) {
		return new Money(this.cents - other.cents);
	}

	public Money times(int quantity) {
		return new Money(this.cents * quantity);
	}

	@Override
	public int compareTo(Money other) {
		return Long.compare(this.cents, other.cents);
	}

	@Override
	public String toString() {
		return "%d.%02d".formatted(cents / 100, cents % 100);
	}
}
