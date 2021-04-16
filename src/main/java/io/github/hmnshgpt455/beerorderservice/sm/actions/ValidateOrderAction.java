package io.github.hmnshgpt455.beerorderservice.sm.actions;

import io.github.hmnshgpt455.beerorderservice.config.JmsConfig;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderEventEnum;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderStatusEnum;
import io.github.hmnshgpt455.beerorderservice.repositories.BeerOrderRepository;
import io.github.hmnshgpt455.beerorderservice.services.BeerOrderManagerImpl;
import io.github.hmnshgpt455.beerorderservice.web.mappers.BeerOrderMapper;
import io.github.hmnshgpt455.brewery.model.BeerOrderDto;
import io.github.hmnshgpt455.brewery.events.ValidateBeerOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        Optional.ofNullable(context.getMessage()).flatMap(msg -> Optional.ofNullable((String) msg.getHeaders().getOrDefault(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER, " "))).ifPresent(orderId -> {
            UUID beerOrderUUID = UUID.fromString(orderId);
            if (beerOrderRepository.existsById(beerOrderUUID)) {
                BeerOrderDto beerOrderDto = beerOrderMapper.beerOrderToDto(beerOrderRepository.getOne(beerOrderUUID));
                jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_QUEUE, new ValidateBeerOrderRequest(beerOrderDto));
                log.debug("Sent validation request to queue for order id : " + beerOrderUUID);
            }
        });
    }
}
