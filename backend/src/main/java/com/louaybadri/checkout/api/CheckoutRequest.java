package com.louaybadri.checkout.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.louaybadri.checkout.pricing.Cart;

/**
 * The cart as it arrives over HTTP: {@code {"items":[{"sku":"APPLE","quantity":3}]}}.
 */
record CheckoutRequest(@NotNull @Valid List<ItemRequest> items) {

	record ItemRequest(@NotBlank String sku, @Min(1) int quantity) {
	}

	Cart toCart() {
		return new Cart(items.stream().map(item -> new Cart.Item(item.sku(), item.quantity())).toList());
	}
}
