package com.louaybadri.checkout.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.louaybadri.checkout.pricing.Catalog;
import com.louaybadri.checkout.pricing.Money;
import com.louaybadri.checkout.pricing.Offer;
import com.louaybadri.checkout.pricing.Product;

/**
 * A fault on the shop's side is not the customer's fault, so it must not come back as a 400
 * "Invalid cart". Here the catalog itself is broken, the way a mistyped offer in catalog.yml
 * would be, while the cart sent is perfectly valid.
 */
@WebMvcTest(controllers = CheckoutController.class)
@Import(CheckoutApiFaultTest.BrokenCatalog.class)
class CheckoutApiFaultTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void aFaultOnTheShopsSideIsNotBlamedOnTheCart() {
		// MockMvc hands back an exception no handler turned into a response; in the running
		// application Spring Boot answers it with a 500.
		assertThatThrownBy(() -> mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"items":[{"sku":"APPLE","quantity":1}]}""")))
			.hasRootCauseInstanceOf(IllegalArgumentException.class)
			.hasRootCauseMessage("An offer needs a name");
	}

	@TestConfiguration
	static class BrokenCatalog {

		private static final Product APPLE = new Product("APPLE", "Apple", Money.ofCents(30));

		@Bean
		Catalog catalog() {
			return new Catalog() {

				@Override
				public Optional<Product> findBySku(String sku) {
					return Optional.of(APPLE).filter(product -> product.sku().equals(sku));
				}

				@Override
				public List<Product> products() {
					return List.of(APPLE);
				}

				@Override
				public List<Offer> activeOffers() {
					// An offer with no name, read from a broken catalog.yml.
					return List.of(new Offer("", Map.of("APPLE", 2), Money.ofCents(45)));
				}
			};
		}
	}
}
