package com.louaybadri.checkout.api;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.louaybadri.checkout.pricing.Catalog;

/**
 * What the shop sells and what is on offer. The frontend needs both to draw the shelf, and an
 * offer can span products, so the two are separate resources rather than one nested in the other.
 */
@RestController
@RequestMapping("/api")
class CatalogController {

	private final Catalog catalog;

	CatalogController(Catalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/products")
	List<ProductResponse> products() {
		return catalog.products()
			.stream()
			.map(product -> new ProductResponse(product.sku(), product.name(), product.unitPrice().cents(),
					CheckoutRequest.MAX_QUANTITY))
			.toList();
	}

	@GetMapping("/offers")
	List<OfferResponse> offers() {
		return catalog.activeOffers()
			.stream()
			.map(offer -> new OfferResponse(offer.name(), offer.requiredItems(), offer.price().cents()))
			.toList();
	}

	/**
	 * A product as the frontend sees it. {@code maxQuantity} is the same limit the checkout
	 * enforces, sent along so the cart can stop at it without the frontend writing the number
	 * down a second time.
	 */
	record ProductResponse(String sku, String name, long unitPriceCents, int maxQuantity) {
	}

	record OfferResponse(String name, Map<String, Integer> items, long priceCents) {
	}
}
