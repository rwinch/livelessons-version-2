package com.example.rsocketclient;

import io.rsocket.RSocket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.rsocket.client.BrokerClient;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;

@Log4j2
@SpringBootApplication
public class RsocketClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(RsocketClientApplication.class, args);
  }

  @Bean
  ApplicationListener<ApplicationReadyEvent> secureClient(RSocketRequester.Builder builder) {
    return event -> {



      RSocketRequester localhost = builder
          .connectTcp("localhost", 8888)
          .block();
      localhost
          .route("greeting")
          .retrieveMono(GreetingResponse.class)
          .subscribe(gr -> log.info("secure response: " + gr.getMessage()));
    };
  }


//  @Bean
  ApplicationListener<ApplicationReadyEvent> client(RSocketRequester.Builder builder) {
    return event ->
        builder
            .connectTcp("localhost", 8888)
            .block()
            .route("greetings")
            .data(new GreetingRequest("Livelessons"))
            .retrieveFlux(GreetingResponse.class)
            .subscribe(gr -> log.info("client: " + gr.getMessage()));
  }

//  @Bean
  ApplicationListener<PayloadApplicationEvent<RSocketRequester>> gatewayClient(BrokerClient client) {
    return event ->
        event
            .getPayload()
            .route("greetings")
            .metadata(client.forwarding("greetings-service"))
            .data(new GreetingRequest("World"))
            .retrieveFlux(GreetingResponse.class)
            .subscribe(gr -> log.info("rsocket client: " + gr.getMessage()));
  }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest {
  private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse {
  private String message;
}