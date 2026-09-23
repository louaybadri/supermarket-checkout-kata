package com.louaybadri.checkout.catalog;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The contents of {@code catalog.yml}: the products the shop sells, and the named sets of offers
 * it can run. Which set is live is {@code catalog.active-preset}, so a different week is a
 * property change rather than a code change.
 */
@ConfigurationProperties("catalog")
public record CatalogProperties(String activePreset, List<ProductConfig> products,
		Map<String, List<OfferConfig>> presets) {

	public record ProductConfig(String sku, String name, long unitPriceCents) {
	}

	public record OfferConfig(String name, long priceCents, Map<String, Integer> items) {
	}

	/**
	 * The offers of the live preset, or a startup failure naming the presets that do exist.
	 */
	public List<OfferConfig> activeOffers() {
		List<OfferConfig> offers = presets.get(activePreset);
		if (offers == null) {
			throw new IllegalStateException("No offer preset called '" + activePreset + "'. Known presets: "
					+ presets.keySet());
		}
		return offers;
	}
}
