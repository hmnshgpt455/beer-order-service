package io.github.hmnshgpt455.beerorderservice.services.listeners;

import io.github.hmnshgpt455.beerorderservice.config.JmsConfig;
import io.github.hmnshgpt455.beerorderservice.services.BeerOrderManager;
import io.github.hmnshgpt455.brewery.events.AllocateBeerOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BeerOrderAllocationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RESULT_QUEUE)
    public void listen(AllocateBeerOrderResult allocateBeerOrderResult) {

        beerOrderManager.handleBeerOrderAllocationResult(allocateBeerOrderResult.getBeerOrder(),
                allocateBeerOrderResult.getIsAllocationComplete(), allocateBeerOrderResult.getIsAllocationFailed());
    }
}
