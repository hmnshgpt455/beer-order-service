package io.github.hmnshgpt455.beerorderservice.services.testcomponents;

import io.github.hmnshgpt455.beerorderservice.config.JmsConfig;
import io.github.hmnshgpt455.beerorderservice.services.BeerOrderManagerImplIT;
import io.github.hmnshgpt455.brewery.events.AllocateBeerOrderRequest;
import io.github.hmnshgpt455.brewery.events.AllocateBeerOrderResult;
import io.github.hmnshgpt455.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class InventoryAllocationRequestListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    private void listen(AllocateBeerOrderRequest allocateBeerOrderRequest) {

        allocateBeerOrderRequest.getBeerOrderDto().getBeerOrderLines().forEach(line -> line.setQuantityAllocated(line.getOrderQuantity()));

        AtomicBoolean isAllocationComplete = new AtomicBoolean(true);
        AtomicBoolean isAllocationFailed = new AtomicBoolean(false);
        AtomicBoolean isAllocationRequired = new AtomicBoolean(true);

        Optional.ofNullable(allocateBeerOrderRequest.getBeerOrderDto().getCustomerRef()).ifPresent(customerRef -> {
            if (BeerOrderManagerImplIT.FAIL_ALLOCATION_EXCEPTION_INDICATOR.equals(customerRef)) {
                isAllocationComplete.set(false);
                isAllocationFailed.set(true);
            }

            if (BeerOrderManagerImplIT.PARTIAL_ALLOCATION_INDICATOR.equals(customerRef)) {
                isAllocationComplete.set(false);
                isAllocationFailed.set(false);
                updatePartialAllocation(allocateBeerOrderRequest.getBeerOrderDto().getBeerOrderLines());
            }

            if (BeerOrderManagerImplIT.DO_NOT_ALLOCATE_INDICATOR.equals(customerRef)) {
                isAllocationRequired.set(false);
            }
        });

        if (isAllocationRequired.get()) {
            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESULT_QUEUE, AllocateBeerOrderResult.builder()
                    .beerOrder(allocateBeerOrderRequest.getBeerOrderDto())
                    .isAllocationComplete(isAllocationComplete.get())
                    .isAllocationFailed(isAllocationFailed.get())
                    .build());
        }

    }

    private void updatePartialAllocation(List<BeerOrderLineDto> beerOrderLines) {
        beerOrderLines.forEach(line -> line.setQuantityAllocated(BeerOrderManagerImplIT.PARTIAL_QUANTITY_ALLOCATED));
    }
}
