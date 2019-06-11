/**
 * MIT License
 *
 * Copyright (c) 2017 Wreulicke
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.wreulicke.resillient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.asynchttpclient.Response;

import com.github.davidmoten.rx2.RetryWhen;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

@RestController
@RequestMapping("/resilience4j")
public class Resilience4jController {

  private final ReactiveClient reactiveClient;

  private static final Logger log = LoggerFactory.getLogger(Resilience4jController.class);

  private final CircuitBreaker circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
    .failureRateThreshold(3)
    .ringBufferSizeInClosedState(20)
    .ringBufferSizeInHalfOpenState(5)
    .waitDurationInOpenState(Duration.ofSeconds(5))
    .build());

  private final Function<Flowable<? extends Throwable>, Flowable<Object>> exponentialBackoffRetrier = RetryWhen.exponentialBackoff(1,
    TimeUnit.SECONDS)
    .retryWhenInstanceOf(RetryableException.class)
    .maxRetries(3)
    .build();

  public Resilience4jController(ReactiveClient reactiveClient) {
    this.reactiveClient = reactiveClient;
  }

  @GetMapping("/test")
  public Single<ResponseEntity<String>> test() {
    return reactiveClient.execute()
      .flatMap(response -> {
        int statusCode = response.getStatusCode();
        if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()) {
          return Single.error(new RetryableException());
        }
        return Single.just(response);
      })
      .compose(CircuitBreakerOperator.of(circuitBreaker))
      .retryWhen(exponentialBackoffRetrier)
      .map(response -> ResponseEntity.ok(response.getResponseBody()))
      .timeout(10, TimeUnit.SECONDS)
      .observeOn(Schedulers.computation());
  }

  @ExceptionHandler({
    TimeoutException.class, CallNotPermittedException.class, RetryableException.class
  })
  public ResponseEntity<String> errorHandler() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
      .body("Service Unavailable");
  }
}
