package com.louaybadri.checkout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import com.louaybadri.checkout.pricing.Catalog;
import com.louaybadri.checkout.pricing.Checkout;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CheckoutApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheckoutApplication.class, args);
	}

	/**
	 * The checkout is plain Java with no Spring annotations on it, so it is wired up here.
	 */
	@Bean
	Checkout checkout(Catalog catalog) {
		return new Checkout(catalog);
	}

}
