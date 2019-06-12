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

import java.net.URI;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
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

  RestTemplate testRestTemplate = new RestTemplateBuilder().errorHandler(new Client.NoOpResponseErrorHandler())
    .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create()
      .setMaxConnTotal(10000)
      .setMaxConnPerRoute(500)
      .build()))
    .setConnectTimeout(500000)
    .setReadTimeout(500000)
    .build();

  @MockBean
  OtherSystemClientProperties otherSystemClientProperties;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig()
    .dynamicPort());

  @LocalServerPort
  int port;

  @Before
  public void setUp() {
    Mockito.when(otherSystemClientProperties.getUri())
      .thenReturn(URI.create("http://localhost:" + wireMockRule.port()));
  }

  @Test
  public void test() {
    wireMockRule.stubFor(WireMock.post("/")
      .willReturn(WireMock.aResponse()
        .withStatus(503)
        .withFixedDelay(100)));

    Observable.range(0, 500)
      .flatMap(ignore -> Observable.fromCallable(() -> {
        log.info("requesting");
        return testRestTemplate.getForObject("http://localhost:" + port + "/traditional/test", String.class);
      })
        .subscribeOn(Schedulers.newThread()))
      .blockingSubscribe();
  }

  @Test
  public void test2() {
    wireMockRule.stubFor(WireMock.post("/")
      .willReturn(WireMock.aResponse()
        .withStatus(503)
        .withFixedDelay(100)));

    Observable.range(0, 500)
      .flatMap(ignore -> Observable.fromCallable(() -> {
        log.info("requesting");
        return testRestTemplate.getForEntity("http://localhost:" + port + "/reactive/test", String.class);
      })
        .subscribeOn(Schedulers.newThread()))
      .blockingSubscribe(entity -> {
        if (!entity.getStatusCode()
          .equals(HttpStatus.SERVICE_UNAVAILABLE)) {
          log.error("error: {}", entity);
        }
      });

    log.info("exit");
  }

  @Test
  public void test3() {
    wireMockRule.stubFor(WireMock.post("/")
      .willReturn(WireMock.aResponse()
        .withStatus(503)
        .withFixedDelay(100)));

    testRestTemplate.getForObject("http://localhost:" + port + "/reactive/test", String.class);
  }
}
