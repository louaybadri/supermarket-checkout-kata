package com.louaybadri.checkout.pricing;

import java.util.List;

/**
 * What the till prints: the products at shelf price, then the discounts that were applied, then
 * what is actually due.
 *
 * <p>The totals are worked out from the lines rather than stored, so they cannot drift away from
 * what is printed above them.
 */
public record Receipt(List<ReceiptLine> lines, List<AppliedOffer> discounts) {

	public Receipt {
		lines = List.copyOf(lines);
		discounts = List.copyOf(discounts);
	}

	/**
	 * What the cart would cost with no offers at all.
	 */
	public Money shelfTotal() {
		return lines.stream().map(ReceiptLine::lineTotal).reduce(Money.ZERO, Money::plus);
	}

	public Money totalSavings() {
		return discounts.stream().map(AppliedOffer::saving).reduce(Money.ZERO, Money::plus);
	}

	public Money total() {
		return shelfTotal().minus(totalSavings());
	}
}
