package io.github.hmnshgpt455.beerorderservice.services;

import io.github.hmnshgpt455.brewery.model.CustomerPagedList;

public interface CustomerService {

    CustomerPagedList listAllCustomers(Integer pageNumber, Integer pageSize);
}
