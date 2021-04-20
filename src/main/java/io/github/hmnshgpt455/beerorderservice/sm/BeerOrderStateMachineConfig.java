package io.github.hmnshgpt455.beerorderservice.sm;

import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderEventEnum;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderStatusEnum;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
@AllArgsConstructor
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final Action<BeerOrderStatusEnum, BeerOrderEventEnum> validateOrderAction;
    private final Action<BeerOrderStatusEnum, BeerOrderEventEnum> allocateOrderAction;

    @Override
    public void configure(StateMachineStateConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> states) throws Exception {
        states.withStates()
                .initial(BeerOrderStatusEnum.NEW)
                .states(EnumSet.allOf(BeerOrderStatusEnum.class))
                .end(BeerOrderStatusEnum.DELIVERED)
                .end(BeerOrderStatusEnum.VALIDATION_FAILED)
                .end(BeerOrderStatusEnum.CANCELLED)
                .end(BeerOrderStatusEnum.PICKED_UP)
                .end(BeerOrderStatusEnum.DELIVERY_FAILED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BeerOrderStatusEnum, BeerOrderEventEnum> transitions) throws Exception {
        transitions.withExternal()
                .source(BeerOrderStatusEnum.NEW).target(BeerOrderStatusEnum.PENDING_VALIDATION)
                .event(BeerOrderEventEnum.VALIDATE_ORDER)
                .action(validateOrderAction)
            .and().withExternal()
                .source(BeerOrderStatusEnum.PENDING_VALIDATION).target(BeerOrderStatusEnum.VALIDATED)
                .event(BeerOrderEventEnum.VALIDATION_PASSED)
            .and().withExternal()
                .source(BeerOrderStatusEnum.PENDING_VALIDATION).target(BeerOrderStatusEnum.VALIDATION_FAILED)
                .event(BeerOrderEventEnum.VALIDATION_FAILED)
            .and().withExternal()
                .source(BeerOrderStatusEnum.VALIDATED).target(BeerOrderStatusEnum.PENDING_INVENTORY_ALLOCATION)
                .event(BeerOrderEventEnum.ALLOCATE_INVENTORY_TO_ORDER)
                .action(allocateOrderAction)
            .and().withExternal()
                .source(BeerOrderStatusEnum.PENDING_INVENTORY_ALLOCATION).target(BeerOrderStatusEnum.INVENTORY_ALLOCATION_FAILED_EXCEPTION)
                .event(BeerOrderEventEnum.INVENTORY_ALLOCATION_FAILURE_EXCEPTION)
            .and().withExternal()
                .source(BeerOrderStatusEnum.PENDING_INVENTORY_ALLOCATION).target(BeerOrderStatusEnum.INVENTORY_ALLOCATED)
                .event(BeerOrderEventEnum.INVENTORY_ALLOCATION_SUCCESS)
            .and().withExternal()
                .source(BeerOrderStatusEnum.PENDING_INVENTORY_ALLOCATION).target(BeerOrderStatusEnum.INVENTORY_ALLOCATION_FAILED_PENDING_INVENTORY)
                .event(BeerOrderEventEnum.INVENTORY_ALLOCATION_FAILURE_NO_INVENTORY);
    }
}
