package io.github.hmnshgpt455.beerorderservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
public class JmsConfig {

    public static final String VALIDATE_ORDER_QUEUE = "validate-order";
    public static final String VALIDATION_RESULT_QUEUE = "validate-order-result";
    public static final String ALLOCATE_ORDER_QUEUE = "allocate-order";
    public static final String ALLOCATE_ORDER_RESULT_QUEUE = "allocate-order-result";
    public static final String INVENTORY_ALLOCATION_FAILED_QUEUE = "inventory-allocation-failed";
    public static final String INVENTORY_ALLOCATION_EXCEPTION_FAILURE_MESSAGE = "INVENTORY_ALLOCATION_FAILED_WITH_EXCEPTION";
    public static final String PARTIAL_INVENTORY_ALLOCATION_MESSAGE = "PARTIAL_INVENTORY_ALLOCATED";

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter();
        mappingJackson2MessageConverter.setTargetType(MessageType.TEXT);
        mappingJackson2MessageConverter.setTypeIdPropertyName("_type");
        mappingJackson2MessageConverter.setObjectMapper(objectMapper);

        return mappingJackson2MessageConverter;
    }
}
