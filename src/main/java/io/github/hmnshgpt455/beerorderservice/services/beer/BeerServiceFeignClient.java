package io.github.hmnshgpt455.beerorderservice.services.beer;

import io.github.hmnshgpt455.brewery.model.BeerDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Optional;
import java.util.UUID;

@FeignClient(name = "beer-service")
public interface BeerServiceFeignClient {

    @RequestMapping(method = RequestMethod.GET, path = "/api/v1/beer/upc/{upc}")
    Optional<BeerDTO> getBeerByUpc(@PathVariable String upc);

    @RequestMapping(method = RequestMethod.GET, path = "/api/v1/beer/{beerId}")
    Optional<BeerDTO> getBeerById(@PathVariable UUID beerId);
}
