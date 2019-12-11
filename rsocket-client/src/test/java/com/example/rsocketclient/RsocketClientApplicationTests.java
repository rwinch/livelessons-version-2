package com.example.rsocketclient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.rsocket.RSocketRequester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RsocketClientApplicationTests {
  @Autowired
  RSocketRequester.Builder builder;
  @Test
  void works() {
    RSocketRequester localhost = builder
            .connectTcp("localhost", 8888)
            .block();
    GreetingResponse response = localhost
            .route("greeting")
            .retrieveMono(GreetingResponse.class)
            .block();
    assertThat(response.getMessage()).isEqualTo("greeting");
  }

}
