package io.github.hmnshgpt455.beerorderservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrder;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderLine;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderStatusEnum;
import io.github.hmnshgpt455.beerorderservice.domain.Customer;
import io.github.hmnshgpt455.beerorderservice.repositories.BeerOrderRepository;
import io.github.hmnshgpt455.beerorderservice.repositories.CustomerRepository;
import io.github.hmnshgpt455.beerorderservice.services.beer.BeerServiceRestTemplateImpl;
import io.github.hmnshgpt455.brewery.model.BeerDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ExtendWith(WireMockExtension.class)
public class BeerOrderManagerImplIT {

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    ObjectMapper objectMapper;

    Customer customer;

    UUID beerId = UUID.randomUUID();

    @TestConfiguration
    static class RestTemplateBuilderProvider {
        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer() {
            WireMockServer server = with(wireMockConfig().port(8083));
            server.start();
            return server;
        }
    }

    @BeforeEach
    void setUp() {
        customer = customerRepository.save(Customer.builder()
                .customerName("Roronoa Zoro")
                .build());
    }

    @Test
    BeerOrder testNewToAllocated() throws JsonProcessingException {

        BeerDTO beerDTO = BeerDTO.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_SERVICE_V1 + "upc/" + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.INVENTORY_ALLOCATED, foundOrder.getOrderStatus());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            foundOrder.getBeerOrderLines().forEach(line -> assertEquals(line.getOrderQuantity(), line.getQuantityAllocated()));
        });

        savedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertNotNull(savedBeerOrder);
        assertEquals(BeerOrderStatusEnum.INVENTORY_ALLOCATED, savedBeerOrder.getOrderStatus());

        return savedBeerOrder;
    }

    @Test
    void testPickUpOrder() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_SERVICE_V1 + "upc/" + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.INVENTORY_ALLOCATED, foundOrder.getOrderStatus());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            foundOrder.getBeerOrderLines().forEach(line -> assertEquals(line.getOrderQuantity(), line.getQuantityAllocated()));
        });

        savedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertNotNull(savedBeerOrder);
        assertEquals(BeerOrderStatusEnum.INVENTORY_ALLOCATED, savedBeerOrder.getOrderStatus());

        beerOrderManager.pickUpOrder(savedBeerOrder.getId());

        savedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.PICKED_UP, foundOrder.getOrderStatus());
        });

    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                    .customer(customer)
                    .build();

        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(BeerOrderLine.builder()
                    .beerId(beerId)
                    .upc("12345")
                    .orderQuantity(1)
                    .beerOrder(beerOrder)
                    .build());

        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }
}
