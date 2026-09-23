package com.louaybadri.checkout.catalog;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills the database from {@code catalog.yml} at startup. H2 is in memory, so the shop is rebuilt
 * on every run and a different week is one property away.
 */
@Component
class CatalogSeeder implements ApplicationRunner {

	private final CatalogProperties catalog;

	private final ProductRepository products;

	private final OfferRepository offers;

	CatalogSeeder(CatalogProperties catalog, ProductRepository products, OfferRepository offers) {
		this.catalog = catalog;
		this.products = products;
		this.offers = offers;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		products.saveAll(catalog.products()
			.stream()
			.map(product -> new ProductEntity(product.sku(), product.name(), product.unitPriceCents()))
			.toList());

		List<OfferEntity> thisWeek = catalog.activeOffers()
			.stream()
			.map(offer -> new OfferEntity(offer.name(), offer.priceCents(), offer.items()))
			.toList();
		offers.saveAll(thisWeek);
	}
}
