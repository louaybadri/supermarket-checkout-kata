package com.louaybadri.checkout.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.louaybadri.checkout.pricing.Money;
import com.louaybadri.checkout.pricing.Product;

/**
 * A row of the {@code product} table. It exists because JPA needs a mutable class with a no-arg
 * constructor, which a record cannot be, and the domain is worth keeping as records.
 */
@Entity
@Table(name = "product")
class ProductEntity {

	@Id
	private String sku;

	private String name;

	private long unitPriceCents;

	protected ProductEntity() {
	}

	ProductEntity(String sku, String name, long unitPriceCents) {
		this.sku = sku;
		this.name = name;
		this.unitPriceCents = unitPriceCents;
	}

	Product toProduct() {
		return new Product(sku, name, Money.ofCents(unitPriceCents));
	}
}
