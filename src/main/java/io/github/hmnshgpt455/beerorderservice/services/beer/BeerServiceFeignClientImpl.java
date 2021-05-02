package io.github.hmnshgpt455.beerorderservice.services.beer;

import io.github.hmnshgpt455.brewery.model.BeerDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Profile("local-discovery")
@RequiredArgsConstructor
public class BeerServiceFeignClientImpl implements BeerService {

    private final BeerServiceFeignClient beerServiceFeignClient;

    @Override
    public Optional<BeerDTO> getBeerById(UUID beerId) {
        return beerServiceFeignClient.getBeerById(beerId);
    }

    @Override
    public Optional<BeerDTO> getBeerByUpc(String upc) {
        return beerServiceFeignClient.getBeerByUpc(upc);
    }
}
