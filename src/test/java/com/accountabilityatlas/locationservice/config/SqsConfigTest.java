package com.accountabilityatlas.locationservice.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.junit.jupiter.api.Test;

class SqsConfigTest {

  private final SqsConfig sqsConfig = new SqsConfig();

  @Test
  void sqsMessagingMessageConverter_createsConverterThatIgnoresPayloadTypeHeader() {
    ObjectMapper objectMapper = new ObjectMapper();

    SqsMessagingMessageConverter converter = sqsConfig.sqsMessagingMessageConverter(objectMapper);

    assertNotNull(converter);
  }
}
