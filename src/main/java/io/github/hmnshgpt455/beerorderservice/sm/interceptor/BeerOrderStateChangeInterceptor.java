package io.github.hmnshgpt455.beerorderservice.sm.interceptor;

import io.github.hmnshgpt455.beerorderservice.domain.BeerOrder;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderEventEnum;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderStatusEnum;
import io.github.hmnshgpt455.beerorderservice.repositories.BeerOrderRepository;
import io.github.hmnshgpt455.beerorderservice.sm.StateMachinesHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderStateChangeInterceptor extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;
    private final StateMachinesHelper stateMachinesHelper;

    @Override
    public void preStateChange(State<BeerOrderStatusEnum, BeerOrderEventEnum> state, Message<BeerOrderEventEnum> message,
                               Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition, StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine,
                               StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> rootStateMachine) {

        Optional<BeerOrder> beerOrderOptional = stateMachinesHelper.extractBeerOrderFromMessage(message);
        beerOrderOptional.ifPresent(beerOrder -> {
            log.debug("Saving state for order with ID : " + beerOrder.getId() + " new status : " + state.getId());
            System.out.println("Saving state for order with ID : " + beerOrder.getId() + " new status : " + state.getId());
            beerOrder.setOrderStatus(state.getId());
            beerOrderRepository.saveAndFlush(beerOrder);
        });

    }
}
