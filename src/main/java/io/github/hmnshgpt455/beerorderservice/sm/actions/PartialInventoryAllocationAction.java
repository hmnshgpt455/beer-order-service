package io.github.hmnshgpt455.beerorderservice.sm.actions;

import io.github.hmnshgpt455.beerorderservice.config.JmsConfig;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderEventEnum;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderStatusEnum;
import io.github.hmnshgpt455.beerorderservice.services.BeerOrderManagerImpl;
import io.github.hmnshgpt455.brewery.events.InventoryAllocationFailedNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PartialInventoryAllocationAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {

        Optional.ofNullable(context.getMessage()).ifPresent(message -> Optional.ofNullable((String) message.getHeaders().get(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER))
                .ifPresentOrElse(orderId -> {
                    jmsTemplate.convertAndSend(JmsConfig.INVENTORY_ALLOCATION_FAILED_QUEUE, InventoryAllocationFailedNotification.builder()
                            .failureMessage(JmsConfig.PARTIAL_INVENTORY_ALLOCATION_MESSAGE)
                            .beerOrderId(UUID.fromString(orderId))
                            .build());

                    log.error("Sent partial allocation failure message for order id : " + orderId);
                },() ->  log.error("Order id not found in the message payload")));

    }
}
