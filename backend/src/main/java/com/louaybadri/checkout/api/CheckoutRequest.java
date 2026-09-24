package com.louaybadri.checkout.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.louaybadri.checkout.pricing.Cart;

/**
 * The cart as it arrives over HTTP: {@code {"items":[{"sku":"APPLE","quantity":3}]}}.
 *
 * <p>The constraints sit on the element type, {@code List<@NotNull @Valid ItemRequest>}, so each
 * entry of the list is checked: {@code @NotNull} rejects {@code {"items":[null]}}, and
 * {@code @Valid} goes on to check the sku and the quantity inside every item.
 */
record CheckoutRequest(@NotNull List<@NotNull @Valid ItemRequest> items) {

	/**
	 * The most of one product a line may ask for. No shopper puts a hundred of one thing through
	 * a till, and a cart that tries is turned away before any pricing work starts. This is the
	 * only place the number is written.
	 */
	static final int MAX_QUANTITY = 99;

	record ItemRequest(@NotBlank String sku, @Min(1) @Max(MAX_QUANTITY) int quantity) {
	}

	Cart toCart() {
		return new Cart(items.stream().map(item -> new Cart.Item(item.sku(), item.quantity())).toList());
	}
}
