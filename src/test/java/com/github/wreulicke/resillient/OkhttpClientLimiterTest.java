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

import java.io.IOException;
import java.util.Optional;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.concurrency.limits.micrometer.MicrometerMetricRegistry;
import org.springframework.test.context.junit4.SpringRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limiter.AbstractPartitionedLimiter;
import com.netflix.concurrency.limits.limiter.BlockingLimiter;

import io.micrometer.core.instrument.Metrics;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class OkhttpClientLimiterTest {

  @LocalServerPort
  int port;

  @Test
  public void test() throws IOException, InterruptedException {
    OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new OkHttpClientLimitInterceptor(new OkHttpClientLimiterBuilder()
      .partitionByHost()
      .metricRegistry(new MicrometerMetricRegistry(Metrics.globalRegistry))
      .build()))
      .build();
    while (true) {
      for (int i = 0; i < 50; i++) {
        client.newCall(new Request.Builder().url("http://localhost:" + port + "/test")
          .get()
          .build())
          .enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
              System.out.println("IOE: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
              System.out.println("Response: " + response.code() + ":" + response.message());
            }
          });
      }
      Thread.sleep(100);
    }
  }

  public class OkHttpClientLimitInterceptor implements Interceptor {
    private final Limiter<OkhttpClientRequestContext> contextLimiter;

    public OkHttpClientLimitInterceptor(Limiter<OkhttpClientRequestContext> contextLimiter) {
      this.contextLimiter = contextLimiter;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
      OkhttpClientRequestContext context = new OkhttpClientRequestContext(chain.request());
      Optional<Limiter.Listener> listerOpt = contextLimiter.acquire(context);
      if (listerOpt.isPresent()) {
        Limiter.Listener listener = listerOpt.get();
        try {
          Response response = chain.proceed(chain.request());
          if (response.isSuccessful()) {
            listener.onSuccess();
          }
          else if (response.code() == 503) {
            listener.onDropped();
          }
          else {
            listener.onIgnore();
          }

          return response;
        } catch (IOException e) {
          listener.onIgnore();
          throw e;
        }
      }
      else {
        return new Response.Builder().code(503)
          .protocol(Protocol.HTTP_1_1) // dummy
          .request(chain.request())
          .message("Client concurrency limit reached")
          .body(ResponseBody.create(null, new byte[0]))
          .build();
      }
    }
  }

  public class OkHttpClientLimiterBuilder extends AbstractPartitionedLimiter.Builder<OkHttpClientLimiterBuilder, OkhttpClientRequestContext> {

    private boolean blockOnLimit = false;

    public OkHttpClientLimiterBuilder partitionByHeaderName(String headerName) {
      return partitionResolver(context -> context.request()
        .header(headerName));
    }

    public OkHttpClientLimiterBuilder partitionByHost() {
      return partitionResolver(context -> context.request()
        .url()
        .host());
    }

    /**
     * When set to true new calls to the channel will block when the limit has been reached instead
     * of failing fast with an UNAVAILABLE status.
     *
     * @param blockOnLimit
     * @return Chainable builder
     */
    public <T> OkHttpClientLimiterBuilder blockOnLimit(boolean blockOnLimit) {
      this.blockOnLimit = blockOnLimit;
      return this;
    }

    @Override
    protected OkHttpClientLimiterBuilder self() {
      return this;
    }

    public Limiter<OkhttpClientRequestContext> build() {
      Limiter<OkhttpClientRequestContext> limiter = super.build();

      if (blockOnLimit) {
        limiter = BlockingLimiter.wrap(limiter);
      }
      return limiter;
    }
  }

  public class OkhttpClientRequestContext {

    private final Request request;

    public OkhttpClientRequestContext(Request request) {
      this.request = request;
    }

    Request request() {
      return request;
    }
  }

}
