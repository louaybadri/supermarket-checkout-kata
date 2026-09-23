package com.louaybadri.checkout.catalog;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import com.louaybadri.checkout.pricing.Money;
import com.louaybadri.checkout.pricing.Offer;

/**
 * A row of the {@code offer} table, with the products it requires in {@code offer_item}. An
 * offer can name any number of products, so the items are their own table rather than columns.
 */
@Entity
@Table(name = "offer")
class OfferEntity {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private long priceCents;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "offer_item", joinColumns = @JoinColumn(name = "offer_id"))
	@MapKeyColumn(name = "sku")
	@Column(name = "quantity")
	private Map<String, Integer> items = new LinkedHashMap<>();

	protected OfferEntity() {
	}

	OfferEntity(String name, long priceCents, Map<String, Integer> items) {
		this.name = name;
		this.priceCents = priceCents;
		this.items = new LinkedHashMap<>(items);
	}

	Offer toOffer() {
		return new Offer(name, items, Money.ofCents(priceCents));
	}
}
