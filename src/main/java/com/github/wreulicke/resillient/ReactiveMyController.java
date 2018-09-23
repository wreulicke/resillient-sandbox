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
			.retryWhen(RetryWhen.exponentialBackoff(1, TimeUnit.SECONDS).build())
			.map(response -> {
				if (response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
				}
				return ResponseEntity.ok(response.getResponseBody());
			})
			.timeout(10, TimeUnit.SECONDS)
			.onErrorResumeNext(throwable -> {
				log.info("error", throwable);
				if (throwable instanceof TimeoutException) {
					return Single.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
				}
				return Single.error(throwable);
			})
			.onErrorReturn(e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error"))
			.observeOn(Schedulers.computation());
	}
	
	static class RetryableException extends RuntimeException {
	}
}
