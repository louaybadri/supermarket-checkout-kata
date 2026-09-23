package com.louaybadri.checkout.api;

import java.util.List;

import com.louaybadri.checkout.pricing.Receipt;

/**
 * The receipt as JSON. Every amount is in whole cents, so nothing has to survive a round trip
 * through a floating point number on the way to the browser.
 */
record ReceiptResponse(List<LineResponse> lines, List<DiscountResponse> discounts, long shelfTotalCents,
		long totalSavingsCents, long totalCents) {

	record LineResponse(String sku, String name, int quantity, long unitPriceCents, long lineTotalCents) {
	}

	record DiscountResponse(String name, int times, long savingCents) {
	}

	static ReceiptResponse from(Receipt receipt) {
		List<LineResponse> lines = receipt.lines()
			.stream()
			.map(line -> new LineResponse(line.sku(), line.name(), line.quantity(),
					line.unitPrice().cents(), line.lineTotal().cents()))
			.toList();

		List<DiscountResponse> discounts = receipt.discounts()
			.stream()
			.map(discount -> new DiscountResponse(discount.name(), discount.times(),
					discount.saving().cents()))
			.toList();

		return new ReceiptResponse(lines, discounts, receipt.shelfTotal().cents(),
				receipt.totalSavings().cents(), receipt.total().cents());
	}
}
