package com.louaybadri.checkout.pricing;

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
		Catalog remembered = new RememberingCatalog(catalog);
		Map<String, Integer> basket = cart.quantityBySku();
		List<ReceiptLine> lines = linesFor(basket, remembered);
		BestPrice.OfferCombination cheapest = new BestPrice(remembered).cheapestCombinationFor(basket);
		return new Receipt(lines, discountsFrom(cheapest.offersUsage(), remembered));
	}

	public Money total(Cart cart) {
		return ring(cart).total();
	}

	private static List<ReceiptLine> linesFor(Map<String, Integer> basket, Catalog remembered) {
		return basket.entrySet().stream().map(line -> {
			Product product = remembered.require(line.getKey());
			return new ReceiptLine(product.sku(), product.name(), line.getValue(), product.unitPrice());
		}).toList();
	}

	/**
	 * One discount line per offer used. The search already says how many times each offer was
	 * used, so {@code {bundle=3}} becomes a single line "bundle × 3" whose saving is what one use
	 * saves against shelf price, times three.
	 */
	private static List<AppliedOffer> discountsFrom(Map<Offer, Integer> offersUsage, Catalog remembered) {
		return offersUsage.entrySet().stream().map(offerUsed -> {
			Offer offer = offerUsed.getKey();
			int times = offerUsed.getValue();
			Money savingEachTime = offer.shelfPrice(remembered).minus(offer.price());
			return new AppliedOffer(offer.name(), times, savingEachTime.times(times));
		}).toList();
	}
}
