package com.example.greetingservice;

/*
 * This demo supports server-sent events, websockets and RSocket
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class GreetingServiceApplication {

  public static void main(String[] args) {
    Hooks.onOperatorDebug();
    SpringApplication.run(GreetingServiceApplication.class, args);
  }

  @Bean
  RouterFunction<ServerResponse> routes(GreetingService greetingService) {
    return route()
        .GET("/greetings/{name}", request -> {
          var name = request.pathVariable("name");
          var greetingRequest = new GreetingRequest(name);
          var greet = greetingService.greetings(greetingRequest);
          return ServerResponse
              .ok()
              .contentType(MediaType.TEXT_EVENT_STREAM)
              .body(greet, GreetingResponse.class);
        })
        .build();
  }

}


@Configuration
class WebSocketConfiguration {

  @Bean
  SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler wsh) {
    return new SimpleUrlHandlerMapping(Map.of("/ws/greetings", wsh), 10);
  }

  @Bean
  WebSocketHandler webSocketHandler(GreetingService gs) {
    return session -> {
      var responses = session
          .receive()
          .map(WebSocketMessage::getPayloadAsText)
          .map(GreetingRequest::new)
          .flatMap(gs::greetings)
          .map(GreetingResponse::getMessage)
          .map(session::textMessage);
      return session.send(responses);
    };
  }

  @Bean
  WebSocketHandlerAdapter webSocketHandlerAdapter() {
    return new WebSocketHandlerAdapter();
  }
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse {
  private String message;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest {
  private String name;
}

@Log4j2
@Controller
class GreetingService {

  private GreetingResponse greet(String name) {
    return new GreetingResponse("Hello " + name + " @ " + Instant.now());
  }

  @MessageMapping("greetings")
  Flux<GreetingResponse> greetings(GreetingRequest request) {
    return Flux
        .fromStream(Stream.generate(() -> greet(request.getName())))
        .delayElements(Duration.ofSeconds(1));
  }

  @MessageMapping("greeting")
  Mono<GreetingResponse> greeting(GreetingRequest request) {
    return Mono.just(greet(request.getName()));
  }

  @MessageMapping("error-signal")
  Mono<String> handleAndReturnError(String payload) {
    return Mono.error(new IllegalArgumentException("Invalid input error"));
  }

  @MessageExceptionHandler(IllegalArgumentException.class)
  Mono<String> onIllegalArgumentException(
      IllegalArgumentException iae) {
    log.error(iae);
    return Mono.just("OoOps!");
  }

}


@Data
@AllArgsConstructor
class Now {

  private long now;

  public Now() {
    this(new Date());
  }

  public Now(Date date) {
    Assert.isTrue(date != null, "the date must not be null");
    this.now = date.getTime();
  }
}