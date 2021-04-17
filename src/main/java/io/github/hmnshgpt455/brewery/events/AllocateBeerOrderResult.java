package io.github.hmnshgpt455.brewery.events;

import io.github.hmnshgpt455.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AllocateBeerOrderResult {

    private BeerOrderDto beerOrder;
    private Boolean isAllocationFailed;
    private Boolean isAllocationComplete;
}
