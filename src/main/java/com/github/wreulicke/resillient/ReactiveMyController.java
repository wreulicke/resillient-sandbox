package com.github.wreulicke.resillient;

import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observable;
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
	public Observable<ResponseEntity<?>> test() {
		log.info("test start");
		return reactiveClient.execute()
			.flatMap(response -> {
				int statusCode = response.getStatusCode();
				if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()) {
					return Observable.error(new RetryableException());
				}
				return Observable.just(response);
			})
			.retryWhen(observable ->
				observable.zipWith(Observable.range(0, 3), (e, i) -> i)
					.flatMap(retryCount -> {
						return Observable.timer((long) Math.pow(4, retryCount), TimeUnit.SECONDS)
							.doOnNext(aLong -> {
								log.warn("request is failed. retrying...");
							});
					}))
			.map(response -> {
				if (response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
				}
				return ResponseEntity.ok(response.getResponseBody());
			})
			.onErrorReturn(e -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error"))
			.observeOn(Schedulers.computation());
	}
	
	static class RetryableException extends RuntimeException {
	}
}
