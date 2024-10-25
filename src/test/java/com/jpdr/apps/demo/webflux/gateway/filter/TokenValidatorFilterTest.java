package com.jpdr.apps.demo.webflux.gateway.filter;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static com.jpdr.apps.demo.webflux.gateway.util.TestDataGenerator.SERVER;
import static com.jpdr.apps.demo.webflux.gateway.util.TestDataGenerator.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenValidatorFilterTest {
  
  private TokenValidatorFilter tokenValidatorFilter;
  private TokenValidatorFilter.Config config;
  private ServerWebExchange exchange;
  @Mock
  private GatewayFilterChain gatewayFilterChain;
  private static MockWebServer mockWebServer;
  private GatewayFilter gatewayFilter;
  
  @BeforeAll
  static void beforeAll() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start(9999);
  }
  
  @BeforeEach
  void beforeEach(){
    config = new TokenValidatorFilter.Config();
    when(gatewayFilterChain.filter(any(ServerWebExchange.class)))
      .thenReturn(Mono.empty());
    String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
    WebClient webClient = WebClient.builder()
      .baseUrl(baseUrl)
      .build();
    tokenValidatorFilter = new TokenValidatorFilter(webClient);
    gatewayFilter = tokenValidatorFilter.apply(config);
  }
  
  @Test
  @DisplayName("OK - Validator Token - Valid Token")
  void givenValidTokenWhenFilterThenReturnEmpty(){
    
    MockResponse response = new MockResponse();
    response.setResponseCode(HttpStatus.NO_CONTENT.value());
    mockWebServer.enqueue(response);
    
    MockServerHttpRequest request = MockServerHttpRequest
      .get(SERVER)
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
      .build();
    
    exchange = MockServerWebExchange.from(request);
    Mono<Void> filterMono = gatewayFilter.filter(exchange, gatewayFilterChain);
    
    StepVerifier.create(filterMono)
      .expectComplete()
      .verify();
  }
  
  @Test
  @DisplayName("Error - Validator Token - Invalid Token")
  void givenInvalidTokenWhenFilterThenReturnError(){
    
    MockResponse response = new MockResponse();
    response.setResponseCode(HttpStatus.UNAUTHORIZED.value());
    mockWebServer.enqueue(response);
    
    MockServerHttpRequest request = MockServerHttpRequest
      .get(SERVER)
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
      .build();
    
    exchange = MockServerWebExchange.from(request);
    Mono<Void> filterMono = gatewayFilter.filter(exchange, gatewayFilterChain);
    
    StepVerifier.create(filterMono)
      .expectError(ResponseStatusException.class)
      .verify();
  }
  
  @Test
  @DisplayName("Error - Validator Token - Missing Token Header")
  void givenMissingTokenHeaderWhenFilterThenReturnError(){
    MockServerHttpRequest request = MockServerHttpRequest
      .get(SERVER)
      .build();
    exchange = MockServerWebExchange.from(request);
    Mono<Void> filterMono = gatewayFilter.filter(exchange, gatewayFilterChain);
    StepVerifier.create(filterMono)
      .expectError(ResponseStatusException.class)
      .verify();
  }
  
  @Test
  @DisplayName("OK - Get Order")
  void givenOrderWhenGetOrderThenReturnOrder(){
    assertEquals(Integer.MIN_VALUE, tokenValidatorFilter.getOrder());
  }
  
  
  @AfterAll
  static void afterAll() throws IOException {
    mockWebServer.shutdown();
  }
  
}
