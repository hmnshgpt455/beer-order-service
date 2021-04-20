package io.github.hmnshgpt455.beerorderservice.sm.actions;

import io.github.hmnshgpt455.beerorderservice.config.JmsConfig;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrder;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderEventEnum;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderStatusEnum;
import io.github.hmnshgpt455.beerorderservice.repositories.BeerOrderRepository;
import io.github.hmnshgpt455.beerorderservice.sm.StateMachinesHelper;
import io.github.hmnshgpt455.beerorderservice.web.mappers.BeerOrderMapper;
import io.github.hmnshgpt455.brewery.events.ValidateBeerOrderRequest;
import io.github.hmnshgpt455.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderMapper beerOrderMapper;
    private final StateMachinesHelper stateMachinesHelper;
    private final BeerOrderRepository beerOrderRepository;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {

        Optional<BeerOrder> beerOrderOptional = stateMachinesHelper.extractBeerOrderFromMessage(context.getMessage());
        beerOrderOptional.ifPresent(beerOrder -> {
            BeerOrderDto beerOrderDto = beerOrderMapper.beerOrderToDto(beerOrder);
            jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_QUEUE, new ValidateBeerOrderRequest(beerOrderDto));
            log.debug("Sent validation request to queue for order id : " + beerOrderDto.getId());
            System.out.println("Sent validation request to queue for order id : " + beerOrderDto.getId());
        });

    }
}
