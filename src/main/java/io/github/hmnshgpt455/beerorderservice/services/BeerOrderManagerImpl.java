package io.github.hmnshgpt455.beerorderservice.services;

import io.github.hmnshgpt455.beerorderservice.domain.BeerOrder;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderEventEnum;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderStatusEnum;
import io.github.hmnshgpt455.beerorderservice.repositories.BeerOrderRepository;
import io.github.hmnshgpt455.beerorderservice.sm.interceptor.BeerOrderStateChangeInterceptor;
import io.github.hmnshgpt455.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String BEER_ORDER_ID_HEADER = "BEER_ORDER_ID";

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    @Override
    @Transactional
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
        sendEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void handleBeerOrderValidationResult(Boolean isValidBeerOrder, UUID beerOrderId) {

        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);

        if (isValidBeerOrder) {
            sendEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);
            beerOrder = beerOrderRepository.getOne(beerOrderId);
            sendEvent(beerOrder, BeerOrderEventEnum.ALLOCATE_INVENTORY_TO_ORDER);
        } else {
            sendEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
        }
    }

    @Override
    public void handleBeerOrderAllocationResult(BeerOrderDto beerOrderDto, Boolean isAllocationComplete, Boolean isAllocationFailed) {

        if (isAllocationComplete) {
            handleBeerOrderAllocationPassed(beerOrderDto);
        } else if (!isAllocationFailed) {
            handleBeerOrderAllocationNoInventory(beerOrderDto);
        } else {
            handleBeerOrderAllocationFailed(beerOrderDto);
        }
    }

    private void handleBeerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendEvent(beerOrder, BeerOrderEventEnum.INVENTORY_ALLOCATION_FAILURE_EXCEPTION);
    }

    private void handleBeerOrderAllocationNoInventory(BeerOrderDto beerOrderDto) {

        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendEvent(beerOrder, BeerOrderEventEnum.INVENTORY_ALLOCATION_FAILURE_NO_INVENTORY);

        beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        updateAllocatedQuantity(beerOrder, beerOrderDto);
    }

    private void handleBeerOrderAllocationPassed(BeerOrderDto beerOrderDto) {

        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendEvent(beerOrder, BeerOrderEventEnum.INVENTORY_ALLOCATION_SUCCESS);

        beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        updateAllocatedQuantity(beerOrder, beerOrderDto);
    }

    private void updateAllocatedQuantity(BeerOrder beerOrder, BeerOrderDto beerOrderDto) {

        beerOrderDto.getBeerOrderLines().forEach(orderLineDto -> {
            beerOrder.getBeerOrderLines().forEach(orderLine -> {
                if (orderLineDto.getId().equals(orderLine.getId())) {
                    orderLine.setQuantityAllocated(orderLineDto.getQuantityAllocated());
                }
            });
        });

        beerOrderRepository.saveAndFlush(beerOrder);

    }


    private void sendEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine = buildSM(beerOrder);

        Message message = MessageBuilder
                            .withPayload(eventEnum)
                            .setHeader(BEER_ORDER_ID_HEADER, beerOrder.getId().toString())
                            .build();

        stateMachine.sendEvent(message);
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> buildSM(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine =
                stateMachineFactory.getStateMachine(beerOrder.getId());

        stateMachine.stopReactively();

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
                    sma.resetStateMachine(new DefaultStateMachineContext<>(
                                    beerOrder.getOrderStatus(), null, null, null));
                });

        stateMachine.startReactively();

        return stateMachine;
    }
}
