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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String BEER_ORDER_ID_HEADER = "BEER_ORDER_ID";

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    @Override
    //This method is called by the scheduler every 10 seconds
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        sendEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    //This is the method that will be called once M2 replies back to the message being sent in A. The problem is happening in this method.
    public void handleBeerOrderValidationResult(Boolean isValidBeerOrder, UUID beerOrderId) {

        /**
         * If i don't call this method, then for some orders the state remains as new and the below sendEvent is not accepted.
         * This was resolved with this WA method. Wanted to know, if there is anything in SM, which can be used to handle this situation
         */
        awaitForStatus(beerOrderId, BeerOrderStatusEnum.PENDING_VALIDATION);

        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            if (isValidBeerOrder) {
                log.debug("Order status right now : " + beerOrder.getOrderStatus());
                //This event is not getting accepted, because it's source state is PENDING_VALIDATION state
                Boolean isEventAccepted = sendEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);
                if (!isEventAccepted) log.debug("Event from pending validation to validation passed not accepted for order with id " + beerOrderId);

                //awaitForStatus(beerOrderId, BeerOrderStatusEnum.VALIDATED);
                BeerOrder validatedOrder = beerOrderRepository.findById(beerOrderId).get();
                isEventAccepted = sendEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_INVENTORY_TO_ORDER);
                if (!isEventAccepted) log.debug("Event from validation passed to pending inventory not accepted for order with id " + beerOrderId);
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

    @Override
    public void pickUpOrder(UUID id) {
        Optional<BeerOrder> allocatedOrderOptional = beerOrderRepository.findById(id);
        allocatedOrderOptional.ifPresentOrElse(allocatedOrder -> sendEvent(allocatedOrder, BeerOrderEventEnum.PICK_UP_ORDER),
                () -> log.error("Order not found with id : " + id));
    }

    @Override
    public void cancelOrder(UUID id) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(id);
        beerOrderOptional.ifPresentOrElse(beerOrder -> sendEvent(beerOrder, BeerOrderEventEnum.CANCEL_ORDER),
                () -> log.error("Order not found with id : " + id));
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


    private Boolean sendEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine = buildSM(beerOrder);

        Message message = MessageBuilder
                            .withPayload(eventEnum)
                            .setHeader(BEER_ORDER_ID_HEADER, beerOrder.getId().toString())
                            .build();

        return stateMachine.sendEvent(message);

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

    private void awaitForStatus(UUID beerOrderId, BeerOrderStatusEnum statusEnum) {

        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger loopCount = new AtomicInteger(0);

        while (!found.get()) {
            if (loopCount.incrementAndGet() > 40) {
                found.set(true);
                log.debug("Loop Retries exceeded");
            }

            beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
                if (beerOrder.getOrderStatus().equals(statusEnum)) {
                    found.set(true);
                    log.debug("Order Found");
                } else {
                    //log.debug("Order Status Not Equal. Expected: " + statusEnum.name() + " Found: " + beerOrder.getOrderStatus().name());
                }
            }, () -> {
                log.debug("Order Id Not Found");
            });

            if (!found.get()) {
                try {
                    //log.debug("Sleeping for retry");
                    Thread.sleep(100);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }
}
