package com.jpdr.apps.demo.webflux.gateway.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {
  
  @Value("${app.base-url.token}")
  private String tokenBaseUrl;
  
  @Bean(name = "connectionProvider")
  @Scope(scopeName = "prototype")
  public ConnectionProvider connectionProvider(){
    return ConnectionProvider.builder("connectionProvider")
      .maxIdleTime(Duration.ofSeconds(10))
      .build();
  }
  
  @Bean(name = "sslContextSpec")
  @Scope(scopeName = "prototype")
  public Http11SslContextSpec sslContextSpec(){
    return Http11SslContextSpec.forClient();
  }
  
  @Bean(name = "reactorClientHttpConnector")
  @Scope(scopeName = "prototype")
  public ReactorClientHttpConnector reactorClientHttpConnector(
    @Qualifier("connectionProvider") ConnectionProvider connectionProvider,
    @Qualifier("sslContextSpec") Http11SslContextSpec sslContextSpec){
    return new ReactorClientHttpConnector(HttpClient.create(connectionProvider)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
      .responseTimeout(Duration.ofMillis(5000))
      .doOnConnected( connection ->  connection
        .addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
        .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS)))
      .secure(spec -> spec.sslContext(sslContextSpec)
        .handshakeTimeout(Duration.ofMillis(5000))
        .closeNotifyFlushTimeout(Duration.ofMillis(5000))
        .closeNotifyReadTimeout(Duration.ofMillis(5000))));
  }
  
  @Bean(name = "exchangeStrategies")
  @Scope(scopeName = "prototype")
  public ExchangeStrategies exchangeStrategies(){
    return ExchangeStrategies.builder()
      .codecs(codecConfig -> codecConfig.defaultCodecs().maxInMemorySize(500 * 1024))
      .build();
  }
  
  @Bean(name = "tokenWebClient")
  public WebClient accountWebClient(WebClient.Builder webClientBuilder,
    @Qualifier("reactorClientHttpConnector") ReactorClientHttpConnector reactorClientHttpConnector,
    @Qualifier("exchangeStrategies") ExchangeStrategies exchangeStrategies){
    return webClientBuilder.baseUrl(tokenBaseUrl)
      .clientConnector(reactorClientHttpConnector)
      .exchangeStrategies(exchangeStrategies)
      .build();
  }
  
}
