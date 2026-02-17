package com.accountabilityatlas.locationservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.junit.jupiter.api.Test;

class SqsConfigTest {

  private final SqsConfig sqsConfig = new SqsConfig();

  @Test
  void sqsMessagingMessageConverter_validObjectMapper_createsConverter() {
    // Arrange
    ObjectMapper objectMapper = new ObjectMapper();

    // Act
    SqsMessagingMessageConverter converter = sqsConfig.sqsMessagingMessageConverter(objectMapper);

    // Assert
    assertThat(converter).isNotNull();
  }
}
