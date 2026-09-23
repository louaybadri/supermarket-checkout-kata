package com.louaybadri.checkout.pricing;

/**
 * A discount on the bill: which offer fired, how often, and what it took off the shelf price.
 */
public record AppliedOffer(String name, int times, Money saving) {
}
