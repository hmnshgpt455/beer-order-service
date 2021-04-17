package io.github.hmnshgpt455.beerorderservice.services;

import io.github.hmnshgpt455.beerorderservice.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void handleBeerOrderValidationResult(Boolean isValidBeerOrder, UUID beerOrderId);
}
