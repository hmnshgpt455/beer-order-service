package io.github.hmnshgpt455.beerorderservice.web.mappers;

import io.github.hmnshgpt455.beerorderservice.domain.BeerOrderLine;
import io.github.hmnshgpt455.beerorderservice.services.beer.BeerService;
import io.github.hmnshgpt455.brewery.model.BeerDTO;
import io.github.hmnshgpt455.brewery.model.BeerOrderLineDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Optional;

public abstract class BeerOrderLineMapperDecorator implements BeerOrderLineMapper {

    private BeerService beerService;
    private BeerOrderLineMapper beerOrderLineMapper;

    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {
        BeerOrderLineDto beerOrderLineDto = beerOrderLineMapper.beerOrderLineToDto(line);
        Optional<BeerDTO> beerDTOOptional = beerService.getBeerByUpc(line.getUpc());
        beerDTOOptional.ifPresent(beerDTO -> {
            beerOrderLineDto.setBeerName(beerDTO.getBeerName());
            beerOrderLineDto.setBeerStyle(beerDTO.getBeerStyle());
            beerOrderLineDto.setPrice(beerDTO.getPrice());
            beerOrderLineDto.setBeerId(beerDTO.getId());
        });
        return beerOrderLineDto;
    }

    @Autowired
    public void setBeerService(BeerService beerService) {
        this.beerService = beerService;
    }

    @Autowired
    @Qualifier("delegate")
    public void setBeerOrderLineMapper(BeerOrderLineMapper beerOrderLineMapper) {
        this.beerOrderLineMapper = beerOrderLineMapper;
    }
}
