package com.accountabilityatlas.locationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SQS configuration for cross-service message deserialization.
 *
 * <p>By default, Spring Cloud AWS SQS adds a {@code JavaType} message attribute containing the
 * producer's fully-qualified class name. When the consumer defines the same event in a different
 * package, deserialization fails with {@code ClassNotFoundException}.
 *
 * <p>This configuration disables the {@code JavaType} header on the receiving side by providing a
 * custom {@link SqsMessagingMessageConverter} whose {@code payloadTypeMapper} always returns null.
 * This forces Jackson to use the {@code @SqsListener} method parameter type instead of the header
 * value.
 */
@Configuration
public class SqsConfig {

  @Bean
  public SqsMessagingMessageConverter sqsMessagingMessageConverter(ObjectMapper objectMapper) {
    SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
    converter.setObjectMapper(objectMapper);
    converter.setPayloadTypeMapper(message -> null);
    return converter;
  }
}
