package com.louaybadri.checkout.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<ProductEntity, String> {
}
