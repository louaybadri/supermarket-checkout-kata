package com.louaybadri.checkout.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CheckoutReceiptTest {

	private static final Product APPLE = new Product("APPLE", "Apple", Money.ofCents(30));

	private static final Product BANANA = new Product("BANANA", "Banana", Money.ofCents(20));

	private static final Offer TWO_APPLES = new Offer("2 apples for 0.45", Map.of("APPLE", 2),
			Money.ofCents(45));

	private static final Offer BUNDLE = new Offer("Apple & banana", Map.of("APPLE", 1, "BANANA", 1),
			Money.ofCents(40));

	private final Checkout checkout = new Checkout(
			new StubCatalog().selling(APPLE, BANANA).offering(TWO_APPLES));

	@Test
	void printsOneLinePerProductAtShelfPrice() {
		Receipt receipt = checkout.receiptFor(cartOf("APPLE", "APPLE", "APPLE", "BANANA"));

		assertThat(receipt.lines())
			.extracting(ReceiptLine::sku, ReceiptLine::name, ReceiptLine::quantity, ReceiptLine::lineTotal)
			.containsExactly(tuple("APPLE", "Apple", 3, Money.ofCents(90)),
					tuple("BANANA", "Banana", 1, Money.ofCents(20)));
	}

	@Test
	void printsTheOfferThatFiredAndWhatItSaved() {
		Receipt receipt = checkout.receiptFor(cartOf("APPLE", "APPLE", "APPLE"));

		assertThat(receipt.discounts())
			.extracting(AppliedOffer::name, AppliedOffer::times, AppliedOffer::saving)
			.containsExactly(tuple("2 apples for 0.45", 1, Money.ofCents(15)));
		assertThat(receipt.shelfTotal()).isEqualTo(Money.ofCents(90));
		assertThat(receipt.total()).isEqualTo(Money.ofCents(75));
	}

	@Test
	void countsAnOfferThatFiresSeveralTimesOnOneLine() {
		Receipt receipt = checkout.receiptFor(cartOf("APPLE", "APPLE", "APPLE", "APPLE", "APPLE"));

		assertThat(receipt.discounts())
			.extracting(AppliedOffer::times, AppliedOffer::saving)
			.containsExactly(tuple(2, Money.ofCents(30)));
		assertThat(receipt.total()).isEqualTo(Money.ofCents(120));
	}

	@Test
	void showsNoDiscountsWhenNoOfferFires() {
		Receipt receipt = checkout.receiptFor(cartOf("BANANA", "BANANA"));

		assertThat(receipt.discounts()).isEmpty();
		assertThat(receipt.totalSavings()).isEqualTo(Money.ZERO);
		assertThat(receipt.total()).isEqualTo(Money.ofCents(40));
	}

	@Test
	void showsABundleAsOneDiscountAcrossTwoLines() {
		Checkout withBundle = new Checkout(new StubCatalog().selling(APPLE, BANANA).offering(BUNDLE));

		Receipt receipt = withBundle.receiptFor(cartOf("APPLE", "BANANA"));

		assertThat(receipt.lines()).hasSize(2);
		assertThat(receipt.discounts())
			.extracting(AppliedOffer::name, AppliedOffer::saving)
			.containsExactly(tuple("Apple & banana", Money.ofCents(10)));
		assertThat(receipt.total()).isEqualTo(Money.ofCents(40));
	}

	@Test
	void anEmptyCartPrintsNothingAndCostsNothing() {
		Receipt receipt = checkout.receiptFor(Cart.empty());

		assertThat(receipt.lines()).isEmpty();
		assertThat(receipt.discounts()).isEmpty();
		assertThat(receipt.total()).isEqualTo(Money.ZERO);
	}

	@Test
	void theTotalMatchesTheOffersPlusWhateverTheyDidNotCover() {
		Checkout withBundle = new Checkout(
				new StubCatalog().selling(APPLE, BANANA).offering(TWO_APPLES, BUNDLE));

		Receipt receipt = withBundle.receiptFor(cartOf("APPLE", "APPLE", "APPLE", "BANANA"));

		// Two apples for 0.45, then the spare apple and banana as a bundle for 0.40.
		assertThat(receipt.total()).isEqualTo(Money.ofCents(85));
		assertThat(receipt.shelfTotal().minus(receipt.totalSavings())).isEqualTo(receipt.total());
	}

	private static Cart cartOf(String... skus) {
		return new Cart(List.of(skus).stream().map(sku -> new Cart.Item(sku, 1)).toList());
	}
}
