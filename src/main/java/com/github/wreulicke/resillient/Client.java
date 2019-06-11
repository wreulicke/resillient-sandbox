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

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import org.apache.http.impl.client.HttpClientBuilder;

@Component
public class Client {

  private RestTemplate restTemplate = new RestTemplateBuilder().defaultMessageConverters()
    .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create()
      .setMaxConnTotal(10000)
      .setMaxConnPerRoute(500)
      .build()))
    .errorHandler(new NoOpResponseErrorHandler())
    .build();

  private final OtherSystemClientProperties properties;

  public Client(OtherSystemClientProperties properties) {
    this.properties = properties;
  }

  public ResponseEntity<String> execute() {
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
