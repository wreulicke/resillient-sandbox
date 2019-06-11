package com.github.wreulicke.resillient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;

public class Resilience4jTest {
	
	Logger LOG = LoggerFactory.getLogger(Resilience4jTest.class);
	
	@Test
	public void test1() throws InterruptedException {
		final int ringBufferSizeInClosedState = 100;
		final Duration waitDurationInOpenState = Duration.ofSeconds(1);
		final CircuitBreaker circuitBreaker = CircuitBreaker.of("circuitBreaker",
			CircuitBreakerConfig.custom()
				.ringBufferSizeInClosedState(ringBufferSizeInClosedState) // default
				.ringBufferSizeInHalfOpenState(10) // default
				// .enableAutomaticTransitionFromOpenToHalfOpen() // default: disable
				.failureRateThreshold(50) // percentage. default: 50 percentage
				.waitDurationInOpenState(waitDurationInOpenState) // default: 60 seconds
				.build());
		
		circuitBreaker.getEventPublisher()
			.onStateTransition(event -> {
				LOG.debug("state transition. from:{}, to:{}",
					event.getStateTransition().getFromState(),
					event.getStateTransition().getToState());
			});
		
		assertThat(circuitBreaker.getState())
			.isEqualTo(CircuitBreaker.State.CLOSED);
		
		LOG.debug("start test");
		// record  error at ringBufferSizeInClosedState times
		for (int i = 0; i < ringBufferSizeInClosedState; i++) {
			circuitBreaker.acquirePermission();
			circuitBreaker.onError(1, new Throwable());
			if (i % 9 == 0) {
				LOG.info("record failure. count:{}", i);
			}
		}

		// transition to open state after record 100 error
		LOG.debug("state is {}", circuitBreaker.getState());
		assertThat(circuitBreaker.getState())
			.isEqualTo(CircuitBreaker.State.OPEN);
		
		// throw exception if state is open
		assertThatThrownBy(circuitBreaker::acquirePermission)
			.isInstanceOf(CallNotPermittedException.class);
		
		LOG.debug("wait to transit to half open state");
		// wait to transit to half open state
		Thread.sleep(waitDurationInOpenState.toMillis() + 200); // waitDurationInOpenState + alpha (200)
		
		// state is open yet until call isCallPermitted
		// ref: https://github.com/resilience4j/resilience4j/blob/06ff9fd59dd49389abba9d88cc67d35f04c36b59/resilience4j-circuitbreaker/src/main/java/io/github/resilience4j/circuitbreaker/internal/CircuitBreakerStateMachine.java#L485
		LOG.debug("state is {}", circuitBreaker.getState());
		assertThat(circuitBreaker.getState())
			.isEqualTo(CircuitBreaker.State.OPEN);
		
		// no exception
		LOG.debug("call isCallPermitted");
		circuitBreaker.acquirePermission();
		
		// transit to half open state
		LOG.debug("state is {}", circuitBreaker.getState());
		assertThat(circuitBreaker.getState())
			.isEqualTo(CircuitBreaker.State.HALF_OPEN);
		
		// failureRate = 4 / 12 = 1 / 3 = 33%
		for (int i = 0; i < 4; i++) {
			circuitBreaker.onSuccess(1);
			circuitBreaker.onSuccess(1);
			circuitBreaker.onError(1, new Throwable());
		}
		
		// transit to close state
		LOG.debug("state is {}", circuitBreaker.getState());
		assertThat(circuitBreaker.getState())
			.isEqualTo(CircuitBreaker.State.CLOSED);
		
		// no exception
		circuitBreaker.acquirePermission();
	}
}
