package com.louaybadri.checkout.api;

import java.util.Comparator;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.louaybadri.checkout.pricing.UnknownProductException;

/**
 * A cart the shop cannot price is bad input, not a server failure, so it comes back as a 400 that
 * says which part is wrong.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} makes every error Spring MVC raises itself,
 * such as a body that is not JSON or a number sent as text, come back as a {@link ProblemDetail}
 * too, instead of Spring's default error page.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

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

	/**
	 * A request that fails {@code @Valid}. Spring's own answer says only "Invalid request content",
	 * so the detail is replaced with each broken field and what is wrong with it, for example
	 * {@code items[0].quantity: must be greater than or equal to 1}. Several broken fields are
	 * joined with "; ".
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String detail = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			// Validation reports the fields in no fixed order; sorting keeps the message the same
			// for the same cart.
			.sorted(Comparator.comparing(FieldError::getField))
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.collect(Collectors.joining("; "));

		ProblemDetail problem = exception.getBody();
		problem.setTitle("Invalid cart");
		problem.setDetail(detail);
		return handleExceptionInternal(exception, problem, headers, status, request);
	}
}
