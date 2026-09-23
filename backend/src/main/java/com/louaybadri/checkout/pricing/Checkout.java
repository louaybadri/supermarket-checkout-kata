package com.louaybadri.checkout.pricing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prices a whole cart against the catalog: the items are counted, the cheapest combination of
 * this week's offers is chosen, and whatever no offer covers is charged at shelf price.
 */
public class Checkout {

	private final Catalog catalog;

	public Checkout(Catalog catalog) {
		this.catalog = catalog;
	}

	public Receipt ring(Cart cart) {
		Map<String, Integer> basket = cart.quantityBySku();
		List<ReceiptLine> lines = linesFor(basket);
		List<Offer> chosen = new BestPrice(catalog).chooseFor(basket).offers();
		return new Receipt(lines, discountsFrom(chosen));
	}

	public Money total(Cart cart) {
		return ring(cart).total();
	}

	private List<ReceiptLine> linesFor(Map<String, Integer> basket) {
		return basket.entrySet().stream().map(line -> {
			Product product = catalog.require(line.getKey());
			return new ReceiptLine(product.sku(), product.name(), line.getValue(), product.unitPrice());
		}).toList();
	}

	/**
	 * The same offer can be applied several times, so the chosen offers are counted and each one
	 * becomes a single discount line.
	 */
	private List<AppliedOffer> discountsFrom(List<Offer> chosen) {
		Map<Offer, Integer> times = new LinkedHashMap<>();
		chosen.forEach(offer -> times.merge(offer, 1, Integer::sum));

		return times.entrySet().stream().map(applied -> {
			Offer offer = applied.getKey();
			Money savingEachTime = offer.shelfPrice(catalog).minus(offer.price());
			return new AppliedOffer(offer.name(), applied.getValue(), savingEachTime.times(applied.getValue()));
		}).toList();
	}
}
