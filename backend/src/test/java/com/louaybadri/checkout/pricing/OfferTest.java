package com.louaybadri.checkout.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.junit.jupiter.api.Test;

class OfferTest {

	private static final Product APPLE = new Product("APPLE", "Apple", Money.ofCents(30));

	private static final Product BANANA = new Product("BANANA", "Banana", Money.ofCents(20));

	private static final Catalog SHOP = new StubCatalog().selling(APPLE, BANANA);

	private static final Offer BUNDLE = new Offer("Apple & banana", Map.of("APPLE", 1, "BANANA", 1),
			Money.ofCents(40));

	@Test
	void fitsABasketThatHoldsEverythingItNeeds() {
		assertThat(BUNDLE.fitsIn(Map.of("APPLE", 2, "BANANA", 1))).isTrue();
	}

	@Test
	void doesNotFitWhenOneOfItsItemsIsMissing() {
		assertThat(BUNDLE.fitsIn(Map.of("APPLE", 2))).isFalse();
	}

	@Test
	void doesNotFitWhenThereAreNotEnoughOfOneItem() {
		Offer threeApples = new Offer("3 apples", Map.of("APPLE", 3), Money.ofCents(60));

		assertThat(threeApples.fitsIn(Map.of("APPLE", 2))).isFalse();
	}

	@Test
	void takesItsItemsOutOfTheBasket() {
		assertThat(BUNDLE.removeFrom(Map.of("APPLE", 2, "BANANA", 1))).containsExactly(entry("APPLE", 1));
	}

	@Test
	void leavesTheBasketItWasGivenAlone() {
		Map<String, Integer> basket = Map.of("APPLE", 2, "BANANA", 1);

		BUNDLE.removeFrom(basket);

		assertThat(basket).containsEntry("APPLE", 2).containsEntry("BANANA", 1);
	}

	@Test
	void knowsWhatItsItemsCostOnTheShelf() {
		assertThat(BUNDLE.shelfPrice(SHOP)).isEqualTo(Money.ofCents(50));
	}

	@Test
	void isWorthItWhenItBeatsTheShelfPrice() {
		assertThat(BUNDLE.isWorthItFor(SHOP)).isTrue();
	}

	@Test
	void isNotWorthItWhenItCostsMoreThanTheShelfPrice() {
		Offer badDeal = new Offer("Apple & banana", Map.of("APPLE", 1, "BANANA", 1), Money.ofCents(60));

		assertThat(badDeal.isWorthItFor(SHOP)).isFalse();
	}

	@Test
	void isNotWorthItWhenItSavesNothing() {
		Offer pointless = new Offer("Apple & banana", Map.of("APPLE", 1, "BANANA", 1), Money.ofCents(50));

		assertThat(pointless.isWorthItFor(SHOP)).isFalse();
	}

	@Test
	void refusesAnOfferWithoutItems() {
		assertThatThrownBy(() -> new Offer("Empty", Map.of(), Money.ofCents(10)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("at least one item");
	}

	@Test
	void refusesAnOfferWithoutAName() {
		assertThatThrownBy(() -> new Offer(" ", Map.of("APPLE", 2), Money.ofCents(45)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("name");
	}
}
