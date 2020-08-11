package com.example.demo.controller;

import java.time.Duration;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import com.example.demo.service.DemoService;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;

@RestController
@RequestMapping("/v1/cb-demo")
public class DemoController {

	@Autowired
	DemoService demoService;

	@GetMapping
	public String invokeCostBasisService() throws InterruptedException {

		CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().failureRateThreshold(50)
				.waitDurationInOpenState(Duration.ofMillis(5000))
				.permittedNumberOfCallsInHalfOpenState(5).minimumNumberOfCalls(10)
				.slidingWindowType(SlidingWindowType.COUNT_BASED).slidingWindowSize(10)

				.build();
		// Create a CircuitBreaker (use default configuration)
		CircuitBreaker circuitBreaker = CircuitBreaker.of("backendName", circuitBreakerConfig);
		
		// Decorate your call to BackendService.doSomething() with a CircuitBreaker
		RetryConfig config = RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ofMillis(1000))
				.retryOnException(e -> e instanceof HttpClientErrorException).build();

		// Create a RetryRegistry with a custom global configuration
		Retry retry = Retry.of("demoService", config);
		
		Supplier<String> supplier = () -> demoService.invokeCallWithCB();

		Supplier<String> decoratedSupplier = Decorators.ofSupplier(supplier).withCircuitBreaker(circuitBreaker)
				.withRetry(retry)
				.decorate();

		demoService.i = -1;
		Queue<State> state= new PriorityQueue<>();
		for (int i = 0; i < 20; i++) {
			state.add(circuitBreaker.getState());
			System.out.println(state);
			System.out.println("Current State ->i::"+i);
			System.out.println(Try.ofSupplier(decoratedSupplier).recover(throwable -> "Hello from Recovery").get());
			if (circuitBreaker.getState().equals(State.OPEN)) {
				System.out.println("Thread is Sleeping....");
				Thread.sleep(10000);
			}
			System.out.println(circuitBreaker.getMetrics().getNumberOfFailedCalls() + "-"+circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
			
			System.out.println(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()+"--"+circuitBreaker.getMetrics().getFailureRate());
			
			
		}

		// Execute the decorated supplier and recover from any exception

		// When you don't want to decorate your lambda expression,
		// but just execute it and protect the call by a CircuitBreaker.
		return "Executed";

	}

}
