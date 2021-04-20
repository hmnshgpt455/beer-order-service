package io.github.hmnshgpt455.beerorderservice.services.listeners;

import io.github.hmnshgpt455.beerorderservice.config.JmsConfig;
import io.github.hmnshgpt455.beerorderservice.repositories.BeerOrderRepository;
import io.github.hmnshgpt455.beerorderservice.services.BeerOrderManager;
import io.github.hmnshgpt455.brewery.events.BeerOrderValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BeerOrderValidationResultListener {

    private final BeerOrderManager beerOrderManager;
    private final BeerOrderRepository beerOrderRepository;

    @JmsListener(destination = JmsConfig.VALIDATION_RESULT_QUEUE)
    public void listen(BeerOrderValidationResult beerOrderValidationResult) {

        log.debug("Validation for beer order : " + beerOrderValidationResult.getBeerOrderId() +
                " is " + beerOrderValidationResult.getIsValidOrder());

        beerOrderManager.handleBeerOrderValidationResult(beerOrderValidationResult.getIsValidOrder(),
                beerOrderValidationResult.getBeerOrderId());
    }
}
