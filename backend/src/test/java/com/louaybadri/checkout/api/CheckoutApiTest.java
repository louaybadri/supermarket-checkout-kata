package com.louaybadri.checkout.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.louaybadri.checkout.pricing.Catalog;
import com.louaybadri.checkout.pricing.Money;
import com.louaybadri.checkout.pricing.Offer;
import com.louaybadri.checkout.pricing.Product;

@WebMvcTest(controllers = { CheckoutController.class, CatalogController.class })
@Import(CheckoutApiTest.ShopWithTwoApplesOffer.class)
class CheckoutApiTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void pricesACartAndSaysWhichOfferFired() throws Exception {
		mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"items":[{"sku":"APPLE","quantity":3}]}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalCents").value(75))
			.andExpect(jsonPath("$.shelfTotalCents").value(90))
			.andExpect(jsonPath("$.totalSavingsCents").value(15))
			.andExpect(jsonPath("$.lines[0].sku").value("APPLE"))
			.andExpect(jsonPath("$.lines[0].lineTotalCents").value(90))
			.andExpect(jsonPath("$.discounts[0].name").value("2 apples for 0.45"))
			.andExpect(jsonPath("$.discounts[0].times").value(1));
	}

	@Test
	void anEmptyCartCostsNothing() throws Exception {
		mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content("""
				{"items":[]}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalCents").value(0))
			.andExpect(jsonPath("$.lines").isEmpty());
	}

	@Test
	void rejectsAProductTheShopDoesNotSell() throws Exception {
		mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"items":[{"sku":"UNICORN","quantity":1}]}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Unknown product"))
			.andExpect(jsonPath("$.sku").value("UNICORN"));
	}

	@Test
	void rejectsAQuantityBelowOne() throws Exception {
		mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"items":[{"sku":"APPLE","quantity":0}]}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid cart"))
			.andExpect(jsonPath("$.detail").value("items[0].quantity: must be greater than or equal to 1"));
	}

	@Test
	void acceptsNinetyNineOfAProduct() throws Exception {
		mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"items":[{"sku":"BANANA","quantity":99}]}"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalCents").value(1980));
	}

	@Test
	void rejectsMoreThanNinetyNineOfAProduct() throws Exception {
		mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"items":[{"sku":"APPLE","quantity":100}]}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid cart"))
			.andExpect(jsonPath("$.detail").value("items[0].quantity: must be less than or equal to 99"));
	}

	@Test
	void rejectsAnItemWithoutASku() throws Exception {
		mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"items":[{"sku":"","quantity":1}]}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid cart"))
			.andExpect(jsonPath("$.detail").value("items[0].sku: must not be blank"));
	}

	@Test
	void rejectsAMissingItem() throws Exception {
		mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"items":[null]}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid cart"))
			.andExpect(jsonPath("$.detail").value("items[0]: must not be null"));
	}

	@Test
	void rejectsACartWithoutItems() throws Exception {
		mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid cart"))
			.andExpect(jsonPath("$.detail").value("items: must not be null"));
	}

	@Test
	void rejectsABodyThatIsNotACart() throws Exception {
		mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"items":[{"sku":"APPLE","quantity":"lots"}]}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.detail").exists());
	}

	@Test
	void listsWhatTheShopSells() throws Exception {
		mvc.perform(get("/api/products"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].sku").value("APPLE"))
			.andExpect(jsonPath("$[0].name").value("Apple"))
			.andExpect(jsonPath("$[0].unitPriceCents").value(30));
	}

	@Test
	void listsThisWeeksOffersWithTheItemsTheyNeed() throws Exception {
		mvc.perform(get("/api/offers"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("2 apples for 0.45"))
			.andExpect(jsonPath("$[0].priceCents").value(45))
			.andExpect(jsonPath("$[0].items.APPLE").value(2));
	}

	@TestConfiguration
	static class ShopWithTwoApplesOffer {

		private static final Product APPLE = new Product("APPLE", "Apple", Money.ofCents(30));

		private static final Product BANANA = new Product("BANANA", "Banana", Money.ofCents(20));

		private static final Offer TWO_APPLES = new Offer("2 apples for 0.45", Map.of("APPLE", 2),
				Money.ofCents(45));

		@Bean
		Catalog catalog() {
			return new Catalog() {

				@Override
				public Optional<Product> findBySku(String sku) {
					return products().stream().filter(product -> product.sku().equals(sku)).findFirst();
				}

				@Override
				public List<Product> products() {
					return List.of(APPLE, BANANA);
				}

				@Override
				public List<Offer> activeOffers() {
					return List.of(TWO_APPLES);
				}
			};
		}

	}

}
