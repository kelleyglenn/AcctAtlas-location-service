package com.accountabilityatlas.locationservice;

import com.accountabilityatlas.locationservice.config.MapboxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MapboxProperties.class)
public class LocationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(LocationServiceApplication.class, args);
  }
}
