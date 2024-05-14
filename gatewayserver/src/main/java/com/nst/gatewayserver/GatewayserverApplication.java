package com.nst.gatewayserver;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootApplication
public class GatewayserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayserverApplication.class, args);
	}

	@Bean
	public RouteLocator nstbankRouteConfig(RouteLocatorBuilder routeLocatorBuilder) {
		return routeLocatorBuilder.routes()
						.route(p -> p
								.path("/nstbank/accounts/**")
								.filters( f -> f.rewritePath("/nstbank/accounts/(?<segment>.*)","/${segment}")
										.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
										.circuitBreaker(config -> config.setName("accountsCircuitBreaker")
												.setFallbackUri("forward:/contactSupport")))
								.uri("lb://ACCOUNTS"))
					.route(p -> p
							.path("/nstbank/loans/**")
							.filters( f -> f.rewritePath("/nstbank/loans/(?<segment>.*)","/${segment}")
									.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
									.retry(retryConfig -> retryConfig.setRetries(3)
											.setMethods(HttpMethod.GET)
											.setBackoff(Duration.ofMillis(100),Duration.ofMillis(1000),2,true)))
							.uri("lb://LOANS"))
					.route(p -> p
							.path("/nstbank/cards/**")
							.filters( f -> f.rewritePath("/nstbank/cards/(?<segment>.*)","/${segment}")
									.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
									).uri("lb://CARDS")).build();
	}
}
