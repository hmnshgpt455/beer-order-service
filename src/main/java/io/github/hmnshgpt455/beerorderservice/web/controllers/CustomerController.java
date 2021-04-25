package io.github.hmnshgpt455.beerorderservice.web.controllers;

import io.github.hmnshgpt455.beerorderservice.services.CustomerService;
import io.github.hmnshgpt455.brewery.model.CustomerPagedList;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public CustomerPagedList listAllCustomers(@RequestParam(value = "pageNumber", defaultValue = "0") Integer pageNumber,
                                              @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize) {

        return customerService.listAllCustomers(pageNumber, pageSize);

    }

}
