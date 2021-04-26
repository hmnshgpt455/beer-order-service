package io.github.hmnshgpt455.beerorderservice.config;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableDiscoveryClient
@Profile("local-discovery")
public class LocalDiscoveryConfig {
}
