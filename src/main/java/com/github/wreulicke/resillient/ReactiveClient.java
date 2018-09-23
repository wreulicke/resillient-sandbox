package com.github.wreulicke.resillient;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

@Component
public class ReactiveClient {
	
	private static final Logger log = LoggerFactory.getLogger(ReactiveClient.class);
	
	private final AsyncHttpClient client = asyncHttpClient();
	
	private final OtherSystemClientProperties otherSystemClientProperties;
	
	public ReactiveClient(OtherSystemClientProperties otherSystemClientProperties) {
		this.otherSystemClientProperties = otherSystemClientProperties;
	}
	
	
	public Single<Response> execute() {
		String url = otherSystemClientProperties.getUri().toString();
		return Single.defer(() -> {
			log.info("request start");
			return Single.fromFuture(client.preparePost(url).execute());
		})
			.subscribeOn(Schedulers.io());
	}
}
