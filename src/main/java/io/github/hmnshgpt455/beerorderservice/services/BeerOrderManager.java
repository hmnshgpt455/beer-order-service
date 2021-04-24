package io.github.hmnshgpt455.beerorderservice.services;

import io.github.hmnshgpt455.beerorderservice.domain.BeerOrder;
import io.github.hmnshgpt455.brewery.model.BeerOrderDto;

import java.util.UUID;

public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void handleBeerOrderValidationResult(Boolean isValidBeerOrder, UUID beerOrderId);

    void handleBeerOrderAllocationResult(BeerOrderDto beerOrderDto, Boolean isAllocationComplete, Boolean isAllocationFailed);

    void pickUpOrder(UUID id);

    void cancelOrder(UUID id);
}
