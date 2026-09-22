package com.louaybadri.checkout.pricing;

/**
 * Something the supermarket sells, identified by its sku.
 */
public record Product(String sku, String name, Money unitPrice) {
}
