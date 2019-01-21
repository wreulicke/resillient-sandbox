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

import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/traditional")
public class MyController {

  private static final Logger log = LoggerFactory.getLogger(MyController.class);

  private final Client client;

  public MyController(Client client) {
    this.client = client;
  }

  @GetMapping("/test")
  public ResponseEntity<?> test() throws InterruptedException {
    log.info("test start");
    ResponseEntity<String> entity = client.execute();
    if (!entity.getStatusCode()
      .equals(HttpStatus.SERVICE_UNAVAILABLE))
      return ResponseEntity.ok(entity.getBody());

    int retryCount = 0;

    while (retryCount < 3) {
      TimeUnit.SECONDS.sleep((long) Math.pow(2, retryCount));
      entity = client.execute();
      if (!entity.getStatusCode()
        .equals(HttpStatus.SERVICE_UNAVAILABLE))
        return ResponseEntity.ok(entity.getBody());
      retryCount++;
      log.warn("request is failed. retrying ...");
    }

    log.error("retry failed.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body("error");
  }

}
