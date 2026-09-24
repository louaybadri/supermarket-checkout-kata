package com.louaybadri.checkout.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.louaybadri.checkout.pricing.Checkout;

@RestController
@RequestMapping("/api/checkout")
class CheckoutController {

	private final Checkout checkout;

	CheckoutController(Checkout checkout) {
		this.checkout = checkout;
	}

	@PostMapping
	ReceiptResponse checkout(@Valid @RequestBody CheckoutRequest request) {
		return ReceiptResponse.from(checkout.receiptFor(request.toCart()));
	}
}
