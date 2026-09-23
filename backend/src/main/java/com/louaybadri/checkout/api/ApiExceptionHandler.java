package com.louaybadri.checkout.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.louaybadri.checkout.pricing.UnknownProductException;

/**
 * A cart the shop cannot price is bad input, not a server failure, so it comes back as a 400 that
 * says which part is wrong.
 */
@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(UnknownProductException.class)
	ProblemDetail onUnknownProduct(UnknownProductException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"The shop does not sell '" + exception.sku() + "'");
		problem.setTitle("Unknown product");
		problem.setProperty("sku", exception.sku());
		return problem;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail onInvalidCart(IllegalArgumentException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				exception.getMessage());
		problem.setTitle("Invalid cart");
		return problem;
	}
}
