package com.github.wreulicke.resillient;

import org.junit.Test;

import io.reactivex.Observable;

public class RxJavaTest {
	
	@Test
	public void test() {
		Observable.fromCallable(() -> {
			System.out.println("start");
			throw new RuntimeException();
		})
			.retryWhen(throwableObservable -> Observable.concatArray(Observable.range(0, 2), throwableObservable.flatMap(Observable::error)))
			.doOnComplete(() -> System.out.println("complete"))
			.blockingSubscribe(o -> {
				System.out.println("exit");
			});
		
	}
}
