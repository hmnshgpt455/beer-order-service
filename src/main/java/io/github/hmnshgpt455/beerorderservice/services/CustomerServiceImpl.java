package io.github.hmnshgpt455.beerorderservice.services;

import io.github.hmnshgpt455.beerorderservice.domain.Customer;
import io.github.hmnshgpt455.beerorderservice.repositories.CustomerRepository;
import io.github.hmnshgpt455.beerorderservice.web.mappers.CustomerMapper;
import io.github.hmnshgpt455.brewery.model.CustomerPagedList;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerPagedList listAllCustomers(Integer pageNumber, Integer pageSize) {
        Page<Customer> customerPage = customerRepository.findAll(PageRequest.of(pageNumber, pageSize));
        return new CustomerPagedList(customerPage.getContent()
                .stream()
                .map(customerMapper::customerToCustomerDto)
                .collect(Collectors.toList()),
                PageRequest.of(customerPage.getPageable().getPageNumber(),
                        customerPage.getPageable().getPageSize()),
                customerPage.getTotalElements());
    }
}
