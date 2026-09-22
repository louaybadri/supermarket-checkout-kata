package com.louaybadri.checkout.pricing;

import java.util.Map;

/**
 * Prices a whole cart against the catalog.
 */
public class Checkout {

	private final Catalog catalog;

	public Checkout(Catalog catalog) {
		this.catalog = catalog;
	}

	public Money total(Cart cart) {
		Money total = Money.ZERO;
		for (Map.Entry<String, Integer> line : cart.quantityBySku().entrySet()) {
			total = total.plus(priceOf(line.getKey(), line.getValue()));
		}
		return total;
	}

	private Money priceOf(String sku, int quantity) {
		Product product = productFor(sku);
		return catalog.findOfferFor(sku)
			.map(offer -> withOffer(offer, product, quantity))
			.orElseGet(() -> product.unitPrice().times(quantity));
	}

	/**
	 * The offer applies as often as it fits, and whatever is left over is charged at the
	 * unit price.
	 */
	private Money withOffer(Offer offer, Product product, int quantity) {
		Money bundles = offer.bundlePrice().times(offer.timesItFitsIn(quantity));
		Money leftOver = product.unitPrice().times(offer.leftOverIn(quantity));
		return bundles.plus(leftOver);
	}

	private Product productFor(String sku) {
		return catalog.findBySku(sku).orElseThrow(() -> new UnknownProductException(sku));
	}
}
