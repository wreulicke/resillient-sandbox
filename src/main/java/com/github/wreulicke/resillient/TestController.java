package com.github.wreulicke.resillient;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
	
	
	@GetMapping
	public ResponseEntity<?> test() throws InterruptedException {
		Thread.sleep(50);
		return ResponseEntity
			.status(503)
			.build();
	}
}
