package com.github.wreulicke.resillient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

public class RetryTest {
	
	Logger LOG = LoggerFactory.getLogger(RetryTest.class);
	
	@Test
	public void test1() throws InterruptedException {
		final RetryConfig retryConfig = RetryConfig.custom()
			.intervalFunction(
				IntervalFunction.ofExponentialBackoff(10, 2)
			)
			.maxAttempts(3) // default
			// .retryOnException(throwable -> true)
			// .retryExceptions(RuntimeException.class)
			// .ignoreExceptions(RuntimeException.class);
			.build()
			;
		final Retry retry = Retry.of("test", retryConfig);
		final String str = Retry.decorateSupplier(retry, () -> "test").get();
	}
}
