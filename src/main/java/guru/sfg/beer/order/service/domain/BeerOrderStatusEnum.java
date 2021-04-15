package guru.sfg.beer.order.service.domain;

public enum BeerOrderStatusEnum {
    NEW, VALIDATED, VALIDATION_FAILED, ALLOCATED,
    ALLOCATION_FAILED, PENDING_INVENTORY, DELIVERED, DELIVERY_FAILED, PICKED_UP
}
