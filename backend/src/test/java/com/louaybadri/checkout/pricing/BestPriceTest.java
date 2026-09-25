package com.louaybadri.checkout.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
		BestPrice.OfferCombination combination = choose(Map.of("BANANA", 2));

		assertThat(combination.offersUsage()).isEmpty();
		assertThat(combination.total()).isEqualTo(Money.ofCents(40));
	}

	@Test
	void prefersThreeBundlesOverTheOfferWithTheBiggerSingleSaving() {
		BestPrice.OfferCombination combination = choose(Map.of("APPLE", 3, "BANANA", 3));

		assertThat(combination.offersUsage()).containsExactly(entry(BUNDLE, 3));
		assertThat(combination.total()).isEqualTo(Money.ofCents(90));
	}

	@Test
	void combinesTwoDifferentOffers() {
		BestPrice.OfferCombination combination = choose(Map.of("APPLE", 4, "BANANA", 1));

		assertThat(combination.offersUsage()).containsOnly(entry(THREE_APPLES, 1), entry(BUNDLE, 1));
		assertThat(combination.total()).isEqualTo(Money.ofCents(90));
	}

	@Test
	void leavesUncoveredItemsAtShelfPrice() {
		BestPrice.OfferCombination combination = choose(Map.of("APPLE", 2, "BANANA", 1));

		assertThat(combination.offersUsage()).containsExactly(entry(BUNDLE, 1));
		assertThat(combination.total()).isEqualTo(Money.ofCents(60));
	}

	@Test
	void ignoresAnOfferThatIsDearerThanTheShelfPrice() {
		Catalog badDeals = new StubCatalog().selling(APPLE)
			.offering(new Offer("2 apples for 0.70", Map.of("APPLE", 2), Money.ofCents(70)));

		BestPrice.OfferCombination combination = new BestPrice(badDeals)
			.cheapestCombinationFor(Map.of("APPLE", 2));

		assertThat(combination.offersUsage()).isEmpty();
		assertThat(combination.total()).isEqualTo(Money.ofCents(60));
	}

	@Test
	void answersEachSituationOnceRatherThanOncePerRoute() {
		// fruit-war's five offers, all fighting over apples, bananas and oranges.
		Product orange = new Product("ORANGE", "Orange", Money.ofCents(40));
		CountingCatalog counting = new CountingCatalog(new StubCatalog().selling(APPLE, BANANA, orange)
			.offering(
					new Offer("Apple, banana & orange for 0.60",
							Map.of("APPLE", 1, "BANANA", 1, "ORANGE", 1), Money.ofCents(60)),
					new Offer("Apple & banana for 0.30", Map.of("APPLE", 1, "BANANA", 1), Money.ofCents(30)),
					new Offer("2 oranges for 0.55", Map.of("ORANGE", 2), Money.ofCents(55)),
					new Offer("3 apples for 0.70", Map.of("APPLE", 3), Money.ofCents(70)),
					new Offer("2 bananas for 0.30", Map.of("BANANA", 2), Money.ofCents(30))));

		BestPrice.OfferCombination combination = new BestPrice(counting)
			.cheapestCombinationFor(Map.of("APPLE", 20, "BANANA", 20, "ORANGE", 20));

		// The search asks for a price at the end of every route it walks, so the lookups count
		// its work. Walking every route asks about 90,000 times; answering each situation once,
		// and reusing it, asks under 2,000 times.
		assertThat(combination.total()).isEqualTo(Money.ofCents(1150));
		assertThat(counting.lookups).isLessThan(5_000);
	}

	private BestPrice.OfferCombination choose(Map<String, Integer> basket) {
		return new BestPrice(shop).cheapestCombinationFor(basket);
	}

	/**
	 * Counts every price the search asks for. It is handed to {@link BestPrice} directly, with no
	 * {@link RememberingCatalog} in between, so nothing hides the repeats.
	 */
	private static final class CountingCatalog implements Catalog {

		private final Catalog shop;

		private int lookups;

		CountingCatalog(Catalog shop) {
			this.shop = shop;
		}

		@Override
		public Optional<Product> findBySku(String sku) {
			lookups++;
			return shop.findBySku(sku);
		}

		@Override
		public List<Product> products() {
			return shop.products();
		}

		@Override
		public List<Offer> activeOffers() {
			return shop.activeOffers();
		}
	}
}
