package io.github.hmnshgpt455.brewery.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class InventoryAllocationFailedNotification {

    private UUID beerOrderId;
    private String failureMessage;

}
