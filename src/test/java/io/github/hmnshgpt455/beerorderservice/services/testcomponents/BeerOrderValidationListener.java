package io.github.hmnshgpt455.beerorderservice.services.testcomponents;

import io.github.hmnshgpt455.beerorderservice.config.JmsConfig;
import io.github.hmnshgpt455.beerorderservice.services.BeerOrderManagerImplIT;
import io.github.hmnshgpt455.brewery.events.BeerOrderValidationResult;
import io.github.hmnshgpt455.brewery.events.ValidateBeerOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void listen(@Payload ValidateBeerOrderRequest request) {

        boolean isValidOrder = !BeerOrderManagerImplIT.FAIL_VALIDATION_INDICATOR.equals(request.getBeerOrderDto().getCustomerRef());
        boolean isPendingValidationOrder = BeerOrderManagerImplIT.PENDING_VALIDATION_INDICATOR.equals(request.getBeerOrderDto().getCustomerRef());

        if (!isPendingValidationOrder) {
            jmsTemplate.convertAndSend(JmsConfig.VALIDATION_RESULT_QUEUE, BeerOrderValidationResult
                    .builder()
                    .isValidOrder(isValidOrder)
                    .beerOrderId(request.getBeerOrderDto().getId())
                    .build());
        }
    }
}
