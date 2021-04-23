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

    public static final String FAIL_VALIDATION_INDICATOR = "fail-validation";
    public static final String FAIL_ALLOCATION_EXCEPTION_INDICATOR = "fail-allocation-exception";
    public static final String PARTIAL_ALLOCATION_INDICATOR = "partial-allocation";
    public static final Integer PARTIAL_QUANTITY_ALLOCATED = 5;

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
    void testNewToAllocated() throws JsonProcessingException {

        BeerDTO beerDTO = BeerDTO.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_SERVICE_V1 + "upc/" + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        assertNotNull(savedBeerOrder);
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.INVENTORY_ALLOCATED, foundOrder.getOrderStatus());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            foundOrder.getBeerOrderLines().forEach(line -> assertEquals(line.getOrderQuantity(), line.getQuantityAllocated()));
        });

    }

    @Test
    void testPickUpOrder() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_SERVICE_V1 + "upc/" + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        assertNotNull(savedBeerOrder);
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.INVENTORY_ALLOCATED, foundOrder.getOrderStatus());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            foundOrder.getBeerOrderLines().forEach(line -> assertEquals(line.getOrderQuantity(), line.getQuantityAllocated()));
        });

        beerOrderManager.pickUpOrder(savedBeerOrder.getId());


        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.PICKED_UP, foundOrder.getOrderStatus());
        });

    }

    @Test
    void testFailedValidation() throws JsonProcessingException {

        BeerDTO beerDTO = BeerDTO.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_SERVICE_V1 + "upc/" + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef(FAIL_VALIDATION_INDICATOR);

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        assertNotNull(savedBeerOrder);
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.VALIDATION_FAILED, foundOrder.getOrderStatus());
        });

    }

    @Test
    void testFailedAllocation() throws JsonProcessingException {

        BeerDTO beerDTO = BeerDTO.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_SERVICE_V1 + "upc/" + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef(FAIL_ALLOCATION_EXCEPTION_INDICATOR);

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        assertNotNull(savedBeerOrder);
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.INVENTORY_ALLOCATION_FAILED_EXCEPTION, foundOrder.getOrderStatus());
        });

    }

    @Test
    void testPartialAllocation() throws JsonProcessingException {

        BeerDTO beerDTO = BeerDTO.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_SERVICE_V1 + "upc/" + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef(PARTIAL_ALLOCATION_INDICATOR);

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        assertNotNull(savedBeerOrder);
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.INVENTORY_ALLOCATION_FAILED_PENDING_INVENTORY, foundOrder.getOrderStatus());
        });

        savedBeerOrder = beerOrderRepository.findById(beerOrder.getId()).get();

        savedBeerOrder.getBeerOrderLines().forEach(line -> assertEquals(PARTIAL_QUANTITY_ALLOCATED, line.getQuantityAllocated()));

    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                    .customer(customer)
                    .build();

        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(BeerOrderLine.builder()
                    .beerId(beerId)
                    .upc("12345")
                    .orderQuantity(10)
                    .beerOrder(beerOrder)
                    .build());

        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }
}
