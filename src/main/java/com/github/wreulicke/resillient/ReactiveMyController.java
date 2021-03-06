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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.rx2.RetryWhen;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

@RestController
@RequestMapping("/reactive")
public class ReactiveMyController {

  private final ReactiveClient reactiveClient;

  private static final Logger log = LoggerFactory.getLogger(ReactiveMyController.class);

  public ReactiveMyController(ReactiveClient reactiveClient) {
    this.reactiveClient = reactiveClient;
  }

  @GetMapping("/test")
  public Single<ResponseEntity<String>> test() {
    log.info("test start");
    return reactiveClient.execute()
      .flatMap(response -> {
        int statusCode = response.getStatusCode();
        if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()) {
          return Single.error(new RetryableException());
        }
        return Single.just(response);
      })
      .retryWhen(RetryWhen.exponentialBackoff(1, TimeUnit.SECONDS)
        .retryWhenInstanceOf(RetryableException.class)
        .maxRetries(3)
        .build())
      .map(response -> {
        if (response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("error");
        }
        return ResponseEntity.ok(response.getResponseBody());
      })
      .timeout(10, TimeUnit.SECONDS)
      .onErrorResumeNext(throwable -> {
        if (throwable instanceof TimeoutException) {
          return Single.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .build());
        }
        return Single.error(throwable);
      })
      .onErrorReturn(e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("error"))
      .observeOn(Schedulers.computation());
  }

}
