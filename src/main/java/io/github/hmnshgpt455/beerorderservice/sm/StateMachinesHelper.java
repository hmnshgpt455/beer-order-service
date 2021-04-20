package io.github.hmnshgpt455.beerorderservice.sm;

import io.github.hmnshgpt455.beerorderservice.domain.BeerOrder;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderEventEnum;
import io.github.hmnshgpt455.beerorderservice.repositories.BeerOrderRepository;
import io.github.hmnshgpt455.beerorderservice.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StateMachinesHelper {

    private final BeerOrderRepository beerOrderRepository;

    public Optional<BeerOrder> extractBeerOrderFromMessage(Message<BeerOrderEventEnum> message) {

        return Optional.ofNullable(message).flatMap(msg ->
                Optional.ofNullable((String) msg.getHeaders().getOrDefault(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER, UUID.randomUUID())))
                .map(beerOrderId -> {
                    UUID beerUUID = UUID.fromString(beerOrderId);
                    if (beerOrderRepository.existsById(beerUUID)) {
                        return  beerOrderRepository.findById(beerUUID).orElse(null);
                    }

                    return null;
                });
    }
}
