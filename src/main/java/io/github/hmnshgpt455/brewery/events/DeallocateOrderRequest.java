package io.github.hmnshgpt455.brewery.events;

import io.github.hmnshgpt455.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeallocateOrderRequest {

    BeerOrderDto beerOrderDto;
}
