package com.accountabilityatlas.locationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mapbox")
public record MapboxProperties(String accessToken) {}
