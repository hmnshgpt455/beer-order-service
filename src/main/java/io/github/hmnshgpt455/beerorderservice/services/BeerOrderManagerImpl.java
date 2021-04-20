package io.github.hmnshgpt455.beerorderservice.services;

import io.github.hmnshgpt455.beerorderservice.domain.BeerOrder;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderEventEnum;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderStatusEnum;
import io.github.hmnshgpt455.beerorderservice.repositories.BeerOrderRepository;
import io.github.hmnshgpt455.beerorderservice.sm.interceptor.BeerOrderStateChangeInterceptor;
import io.github.hmnshgpt455.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String BEER_ORDER_ID_HEADER = "BEER_ORDER_ID";

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        beerOrderRepository.flush();
        sendEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    public void handleBeerOrderValidationResult(Boolean isValidBeerOrder, UUID beerOrderId) {

        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            if (isValidBeerOrder) {
                sendEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);
                BeerOrder validatedOrder = beerOrderRepository.findById(beerOrderId).get();
                sendEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_INVENTORY_TO_ORDER);
            } else {
                sendEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
            }
        }, () -> log.error("Order not found with id : " + beerOrderId));


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
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> sendEvent(beerOrder, BeerOrderEventEnum.INVENTORY_ALLOCATION_FAILURE_EXCEPTION),
                () -> log.error("Order not found with id : " + beerOrderDto.getId()));
    }

    private void handleBeerOrderAllocationNoInventory(BeerOrderDto beerOrderDto) {

        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> sendEvent(beerOrder, BeerOrderEventEnum.INVENTORY_ALLOCATION_FAILURE_NO_INVENTORY),
                () -> log.error("Order not found with id : " + beerOrderDto.getId()));


        beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> updateAllocatedQuantity(beerOrder, beerOrderDto),
                () -> log.error("Order not found with id : " + beerOrderDto.getId()));
    }

    private void handleBeerOrderAllocationPassed(BeerOrderDto beerOrderDto) {

        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> sendEvent(beerOrder, BeerOrderEventEnum.INVENTORY_ALLOCATION_SUCCESS),
                () -> log.error("Order not found with id : " + beerOrderDto.getId()));


        beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> updateAllocatedQuantity(beerOrder, beerOrderDto),
                () -> log.error("Order not found with id : " + beerOrderDto.getId()));
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

        stateMachine.stop();

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
                    sma.resetStateMachine(new DefaultStateMachineContext<>(
                                    beerOrder.getOrderStatus(), null, null, null));
                });

        stateMachine.start();

        return stateMachine;
    }
}
