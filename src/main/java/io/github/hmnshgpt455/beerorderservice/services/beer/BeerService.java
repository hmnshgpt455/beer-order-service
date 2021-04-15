package io.github.hmnshgpt455.beerorderservice.services.beer;

import io.github.hmnshgpt455.brewery.model.BeerDTO;

import java.util.Optional;
import java.util.UUID;

public interface BeerService {

    Optional<BeerDTO> getBeerById(UUID beerId);

    Optional<BeerDTO> getBeerByUpc(String upc);
}
