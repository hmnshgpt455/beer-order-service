package io.github.hmnshgpt455.brewery.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class BeerOrderValidationResult {
    private UUID beerOrderId;
    private Boolean isValidOrder;
}
