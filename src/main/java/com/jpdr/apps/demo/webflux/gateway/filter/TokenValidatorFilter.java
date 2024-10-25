package com.jpdr.apps.demo.webflux.gateway.filter;

import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TokenValidatorFilter extends AbstractGatewayFilterFactory<TokenValidatorFilter.Config> implements Ordered {
  
  private final WebClient webClient;
  
    public TokenValidatorFilter(@Qualifier("tokenWebClient") WebClient webClient){
      super(Config.class);
      this.webClient = webClient;
  }
  
  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> Mono.just(exchange.getRequest().getHeaders())
      .filter(httpHeaders -> httpHeaders.containsKey(HttpHeaders.AUTHORIZATION) &&
          !StringUtils.isBlank(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION)))
      .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)))
      .map(httpHeaders -> httpHeaders.getFirst(HttpHeaders.AUTHORIZATION).substring(7))
      .flatMap(token -> webClient.post()
        .uri("/authentication/unsecure/tokens/validate")
        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
        .bodyValue(token)
        .retrieve()
        .onStatus(httpStatusCode -> !httpStatusCode.equals(HttpStatus.NO_CONTENT),
          response -> response.createException()
            .map(error -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, error.getMessage())))
        .toBodilessEntity()).flatMap(response -> chain.filter(exchange));
  }
  
  @Override
  public int getOrder() {
    return Integer.MIN_VALUE;
  }
  
  @Generated
  public static class Config{
  
  }
  
}
