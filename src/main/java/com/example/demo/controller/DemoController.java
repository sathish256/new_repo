package com.example.demo.controller;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.DemoService;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.vavr.control.Try;

@RestController
@RequestMapping("/v1/cb-demo")
public class DemoController {
	
	@Autowired
	DemoService demoService;
	
	
	@GetMapping
	public String invokeCostBasisService() throws InterruptedException {
		
		CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
				  .failureRateThreshold(75)
				  //.slowCallRateThreshold(50)
				  .waitDurationInOpenState(Duration.ofMinutes(1))
				  //.slowCallDurationThreshold(Duration.ofSeconds(2))
				  .permittedNumberOfCallsInHalfOpenState(50)
				  .minimumNumberOfCalls(1000)
				  .slidingWindowType(SlidingWindowType.COUNT_BASED)
				  .slidingWindowSize(1000)
				  
				  .build();
		// Create a CircuitBreaker (use default configuration)
		CircuitBreaker circuitBreaker = CircuitBreaker.of("backendName", circuitBreakerConfig);
		// Decorate your call to BackendService.doSomething() with a CircuitBreaker
		Supplier<String> decoratedSupplier = CircuitBreaker
		    .decorateSupplier(circuitBreaker, demoService::invokeCallWithCB);
		
		demoService.i = 3;
		
		for(int i=0;i<30;i++) {
			System.out.println("Current State ->"+circuitBreaker.getState());
			if(circuitBreaker.getState().equals(State.OPEN)) {
				System.out.println("Thread is Sleeing....");
				Thread.sleep(20000);
			}
			System.out.println(Try.ofSupplier(decoratedSupplier)
		    .recover(throwable -> "Hello from Recovery").get());
			
		}
		

		// Execute the decorated supplier and recover from any exception

		// When you don't want to decorate your lambda expression,
		// but just execute it and protect the call by a CircuitBreaker.
		return Try.ofSupplier(decoratedSupplier)
			    .recover(throwable -> "Hello from Recovery").get();
		
	}

}
