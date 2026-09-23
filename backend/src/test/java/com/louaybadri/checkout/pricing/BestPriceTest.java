package com.louaybadri.checkout.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class BestPriceTest {

	private static final Product APPLE = new Product("APPLE", "Apple", Money.ofCents(30));

	private static final Product BANANA = new Product("BANANA", "Banana", Money.ofCents(20));

	private static final Offer THREE_APPLES = new Offer("3 apples for 0.60", Map.of("APPLE", 3),
			Money.ofCents(60));

	private static final Offer BUNDLE = new Offer("Apple & banana", Map.of("APPLE", 1, "BANANA", 1),
			Money.ofCents(30));

	private final Catalog shop = new StubCatalog().selling(APPLE, BANANA).offering(THREE_APPLES, BUNDLE);

	@Test
	void usesNoOfferWhenNoneFits() {
		BestPrice.Choice choice = choose(Map.of("BANANA", 2));

		assertThat(choice.offers()).isEmpty();
		assertThat(choice.total()).isEqualTo(Money.ofCents(40));
	}

	@Test
	void prefersThreeBundlesOverTheOfferWithTheBiggerSingleSaving() {
		BestPrice.Choice choice = choose(Map.of("APPLE", 3, "BANANA", 3));

		assertThat(choice.offers()).containsExactly(BUNDLE, BUNDLE, BUNDLE);
		assertThat(choice.total()).isEqualTo(Money.ofCents(90));
	}

	@Test
	void combinesTwoDifferentOffers() {
		BestPrice.Choice choice = choose(Map.of("APPLE", 4, "BANANA", 1));

		assertThat(choice.offers()).containsExactlyInAnyOrder(THREE_APPLES, BUNDLE);
		assertThat(choice.total()).isEqualTo(Money.ofCents(90));
	}

	@Test
	void leavesUncoveredItemsAtShelfPrice() {
		BestPrice.Choice choice = choose(Map.of("APPLE", 2, "BANANA", 1));

		assertThat(choice.offers()).containsExactly(BUNDLE);
		assertThat(choice.total()).isEqualTo(Money.ofCents(60));
	}

	@Test
	void ignoresAnOfferThatIsDearerThanTheShelfPrice() {
		Catalog badDeals = new StubCatalog().selling(APPLE)
			.offering(new Offer("2 apples for 0.70", Map.of("APPLE", 2), Money.ofCents(70)));

		BestPrice.Choice choice = new BestPrice(badDeals).chooseFor(Map.of("APPLE", 2));

		assertThat(choice.offers()).isEmpty();
		assertThat(choice.total()).isEqualTo(Money.ofCents(60));
	}

	private BestPrice.Choice choose(Map<String, Integer> basket) {
		return new BestPrice(shop).chooseFor(basket);
	}
}
