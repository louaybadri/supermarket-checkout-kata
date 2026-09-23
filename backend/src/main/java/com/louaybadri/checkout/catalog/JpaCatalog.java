package com.louaybadri.checkout.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.louaybadri.checkout.pricing.Catalog;
import com.louaybadri.checkout.pricing.Offer;
import com.louaybadri.checkout.pricing.Product;

/**
 * The catalog backed by the database. It is the only class that knows the shop's data lives in
 * H2; the pricing code sees nothing but the {@link Catalog} interface.
 */
@Component
@Transactional(readOnly = true)
class JpaCatalog implements Catalog {

	private final ProductRepository products;

	private final OfferRepository offers;

	JpaCatalog(ProductRepository products, OfferRepository offers) {
		this.products = products;
		this.offers = offers;
	}

	@Override
	public Optional<Product> findBySku(String sku) {
		return products.findById(sku).map(ProductEntity::toProduct);
	}

	@Override
	public List<Offer> activeOffers() {
		return offers.findAll().stream().map(OfferEntity::toOffer).toList();
	}
}
