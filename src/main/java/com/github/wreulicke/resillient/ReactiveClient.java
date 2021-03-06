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
    String url = otherSystemClientProperties.getUri()
      .toString();
    return Single.fromFuture(client.preparePost(url)
      .execute())
      .subscribeOn(Schedulers.io());
  }
}
