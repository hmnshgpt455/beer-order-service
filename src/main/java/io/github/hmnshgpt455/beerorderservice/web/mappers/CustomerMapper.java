package io.github.hmnshgpt455.beerorderservice.web.mappers;

import io.github.hmnshgpt455.beerorderservice.domain.Customer;
import io.github.hmnshgpt455.brewery.model.CustomerDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(uses = {DateMapper.class, BeerOrderLineMapper.class})
public interface CustomerMapper {

    @Mappings(@Mapping(source = "customerName", target = "name"))
    CustomerDto customerToCustomerDto(Customer customer);
}
