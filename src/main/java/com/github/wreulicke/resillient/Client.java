package com.github.wreulicke.resillient;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Component
public class Client {
	
	private RestTemplate restTemplate =
		new RestTemplateBuilder()
			.defaultMessageConverters()
			.errorHandler(new NoOpResponseErrorHandler())
			.build();
	
	private final OtherSystemClientProperties properties;
	
	public Client(OtherSystemClientProperties properties) {
		this.properties = properties;
	}
	
	public ResponseEntity<String> execute(){
		return restTemplate.postForEntity(properties.getUri(), HttpMethod.POST, String.class);
	}
	
	public static class NoOpResponseErrorHandler implements ResponseErrorHandler {
		
		@Override
		public boolean hasError(ClientHttpResponse response) {
			return false;
		}
		
		@Override
		public void handleError(ClientHttpResponse response) {
			throw new AssertionError("Cannot reach here");
		}
	}
}
