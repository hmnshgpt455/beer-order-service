package io.github.hmnshgpt455.beerorderservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.hmnshgpt455.beerorderservice.config.JmsConfig;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrder;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderLine;
import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderStatusEnum;
import io.github.hmnshgpt455.beerorderservice.domain.Customer;
import io.github.hmnshgpt455.beerorderservice.repositories.BeerOrderRepository;
import io.github.hmnshgpt455.beerorderservice.repositories.CustomerRepository;
import io.github.hmnshgpt455.beerorderservice.services.beer.BeerServiceRestTemplateImpl;
import io.github.hmnshgpt455.brewery.events.InventoryAllocationFailedNotification;
import io.github.hmnshgpt455.brewery.model.BeerDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BeerOrderManagerImplIT {

    public static final String FAIL_VALIDATION_INDICATOR = "fail-validation";
    public static final String FAIL_ALLOCATION_EXCEPTION_INDICATOR = "fail-allocation-exception";
    public static final String PARTIAL_ALLOCATION_INDICATOR = "partial-allocation";
    public static final Integer PARTIAL_QUANTITY_ALLOCATED = 5;
    public static final String PENDING_VALIDATION_INDICATOR = "PENDING_VALIDATION_INDICATOR";
    public static final String DO_NOT_ALLOCATE_INDICATOR = "do-not-allocate-indicator";

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

    @Autowired
    JmsTemplate jmsTemplate;

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
    @Order(1)
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
    @Order(2)
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
    @Order(3)
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
    @Order(10000)
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

//        InventoryAllocationFailedNotification notification = (InventoryAllocationFailedNotification) jmsTemplate.receiveAndConvert(JmsConfig.INVENTORY_ALLOCATION_FAILED_QUEUE);
//        assertNotNull(notification);
//        assertNotNull(notification.getBeerOrderId());
//        assertNotNull(notification.getFailureMessage());
//        assertEquals(beerOrder.getId(), notification.getBeerOrderId());
//        assertEquals(JmsConfig.INVENTORY_ALLOCATION_EXCEPTION_FAILURE_MESSAGE, notification.getFailureMessage());
    }

    @Test
    @Order(4)
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

        InventoryAllocationFailedNotification notification = (InventoryAllocationFailedNotification) jmsTemplate.receiveAndConvert(JmsConfig.INVENTORY_ALLOCATION_FAILED_QUEUE);
        assertNotNull(notification);
        assertNotNull(notification.getBeerOrderId());
        assertNotNull(notification.getFailureMessage());
        assertEquals(beerOrder.getId(), notification.getBeerOrderId());
        assertEquals(JmsConfig.PARTIAL_INVENTORY_ALLOCATION_MESSAGE, notification.getFailureMessage());

    }

    @Test
    void testPendingValidationToCancelled() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_SERVICE_V1 + "upc/" + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef(PENDING_VALIDATION_INDICATOR);
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.PENDING_VALIDATION, foundOrder.getOrderStatus());
        });

        beerOrderManager.cancelOrder(savedBeerOrder.getId());
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
        });
    }

    @Test
    void testPartialAllocationToCancelled() throws JsonProcessingException {
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
        UUID beerOrderId = savedBeerOrder.getId();
        beerOrderManager.cancelOrder(savedBeerOrder.getId());
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrderId).get();

            assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
        });

    }

    @Test
    void testAllocationFailedToCancelled() throws JsonProcessingException {
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

        beerOrderManager.cancelOrder(savedBeerOrder.getId());
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
        });
    }

    @Test
    void testAllocatedToCancelled() throws JsonProcessingException {
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

        beerOrderManager.cancelOrder(savedBeerOrder.getId());
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
        });
    }

    @Test
    void pendingAllocationToCancelled() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder().id(beerId).upc("12345").build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_SERVICE_V1 + "upc/" + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDTO))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef(DO_NOT_ALLOCATE_INDICATOR);

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        assertNotNull(savedBeerOrder);
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY_ALLOCATION, foundOrder.getOrderStatus());
        });

        beerOrderManager.cancelOrder(savedBeerOrder.getId());
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
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
                    .orderQuantity(10)
                    .beerOrder(beerOrder)
                    .build());

        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }
}
