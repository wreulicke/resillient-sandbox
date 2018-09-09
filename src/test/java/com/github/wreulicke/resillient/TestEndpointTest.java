package com.github.wreulicke.resillient;

import java.net.URI;

import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@TestPropertySource(properties = "")
public class TestEndpointTest {
	
	private static final Logger log = LoggerFactory.getLogger(TestEndpointTest.class);
	
	RestTemplate testRestTemplate = new RestTemplateBuilder()
		.errorHandler(new Client.NoOpResponseErrorHandler())
		.requestFactory(new HttpComponentsClientHttpRequestFactory(
			HttpClientBuilder.create()
				.setMaxConnTotal(10000)
				.setMaxConnPerRoute(500)
				.build()
		))
		.setConnectTimeout(0)
		.setReadTimeout(0)
		.build();
	
	@MockBean
	OtherSystemClientProperties otherSystemClientProperties;
	
	@Rule
	public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());
	
	@LocalServerPort
	int port;
	
	@Before
	public void setUp() {
		Mockito.when(otherSystemClientProperties.getUri())
			.thenReturn(URI.create("http://localhost:" + wireMockRule.port()));
	}
	
	@Test
	public void test() {
		wireMockRule.stubFor(WireMock.post("/").willReturn(WireMock.aResponse()
			.withStatus(503)
			.withFixedDelay(100)));
		
		Observable.range(0, 500)
			.flatMap(ignore -> Observable.fromCallable(
				() -> {
					log.info("requesting");
					return testRestTemplate.getForObject("http://localhost:" + port + "/traditional/test",
						String.class);
				})
				.subscribeOn(Schedulers.newThread()))
			.blockingSubscribe();
	}
	
	@Test
	public void test2() {
		wireMockRule.stubFor(WireMock.post("/").willReturn(WireMock.aResponse()
			.withStatus(200)
			.withFixedDelay(100)));
		
		Observable.range(0, 500)
			.flatMap(ignore -> Observable.fromCallable(
				() -> {
					log.info("requesting");
					return testRestTemplate.getForObject("http://localhost:" + port + "/reactive/test",
						String.class);
				})
				.subscribeOn(Schedulers.newThread()))
			.blockingSubscribe();
	}
}
