package io.github.hmnshgpt455.beerorderservice.domain;

public enum BeerOrderStatusEnum {
    NEW, VALIDATED, PENDING_VALIDATION, VALIDATION_FAILED, INVENTORY_ALLOCATED,
    INVENTORY_ALLOCATION_FAILED, PENDING_INVENTORY_ALLOCATION, DELIVERED, DELIVERY_FAILED, PICKED_UP, CANCELLED
}
