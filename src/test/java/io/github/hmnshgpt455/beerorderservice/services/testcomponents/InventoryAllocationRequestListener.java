package io.github.hmnshgpt455.beerorderservice.services.testcomponents;

import io.github.hmnshgpt455.beerorderservice.config.JmsConfig;
import io.github.hmnshgpt455.brewery.events.AllocateBeerOrderRequest;
import io.github.hmnshgpt455.brewery.events.AllocateBeerOrderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryAllocationRequestListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    private void listen(AllocateBeerOrderRequest allocateBeerOrderRequest) {

        allocateBeerOrderRequest.getBeerOrderDto().getBeerOrderLines().forEach(line -> line.setQuantityAllocated(line.getOrderQuantity()));

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESULT_QUEUE, AllocateBeerOrderResult.builder()
                                                        .beerOrder(allocateBeerOrderRequest.getBeerOrderDto())
                                                        .isAllocationComplete(true)
                                                        .isAllocationFailed(false)
                                                        .build());

    }
}
