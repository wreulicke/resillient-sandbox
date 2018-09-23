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
		if (!entity.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE))
			return ResponseEntity.ok(entity.getBody());
		
		int retryCount = 0;
		
		while (retryCount < 3) {
			TimeUnit.SECONDS.sleep((long) Math.pow(2, retryCount));
			entity = client.execute();
			if (!entity.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE))
				return ResponseEntity.ok(entity.getBody());
			retryCount++;
			log.warn("request is failed. retrying ...");
		}
		
		log.error("retry failed.");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body("error");
	}
	
}
