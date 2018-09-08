package com.github.wreulicke.resillient;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import org.springframework.stereotype.Component;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

@Component
public class ReactiveClient {
	
	private final AsyncHttpClient client = asyncHttpClient();
	
	private final OtherSystemClientProperties otherSystemClientProperties;
	
	public ReactiveClient(OtherSystemClientProperties otherSystemClientProperties) {
		this.otherSystemClientProperties = otherSystemClientProperties;
	}
	
	
	public Observable<Response> execute() {
		String url = otherSystemClientProperties.getUri().toString();
		return Observable.defer(() -> Observable.fromFuture(client.preparePost(url).execute()))
			.subscribeOn(Schedulers.io());
	}
}
