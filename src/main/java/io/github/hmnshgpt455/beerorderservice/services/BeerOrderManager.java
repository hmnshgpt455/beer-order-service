package io.github.hmnshgpt455.beerorderservice.services;

import io.github.hmnshgpt455.beerorderservice.domain.BeerOrder;

public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);
}
