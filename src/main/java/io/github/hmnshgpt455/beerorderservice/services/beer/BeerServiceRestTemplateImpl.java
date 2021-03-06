package io.github.hmnshgpt455.beerorderservice.services.beer;

import io.github.hmnshgpt455.brewery.model.BeerDTO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Service
@ConfigurationProperties(prefix = "brewery", ignoreUnknownFields = false)
@Profile("!local-discovery")
public class BeerServiceRestTemplateImpl implements BeerService {

    private final RestTemplate restTemplate;

    public static final String BEER_SERVICE_V1 = "/api/v1/beer/";
    private String beerServiceHost;
    private String beerServicePort;
    private String beerServiceScheme;

    public BeerServiceRestTemplateImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public Optional<BeerDTO> getBeerById(UUID beerId) {
        UriBuilder uriBuilder = getBaseBeerServiceURI();
        URI uri = uriBuilder.path(beerId.toString())
                    .build();
        return callBeerServiceAndReturnResult(uri, HttpMethod.GET, null);
    }

    @Override
    public Optional<BeerDTO> getBeerByUpc(String upc) {
        UriBuilder uriBuilder = getBaseBeerServiceURI();
        URI uri = uriBuilder.path("upc/" + upc)
                    .build();
        return callBeerServiceAndReturnResult(uri, HttpMethod.GET, null);
    }

    private Optional<BeerDTO> callBeerServiceAndReturnResult(URI uri, HttpMethod method, RequestEntity requestEntity) {
        ResponseEntity<BeerDTO> responseEntity = restTemplate.exchange(uri,
                method, requestEntity, new ParameterizedTypeReference<>() {});

        return Optional.ofNullable(responseEntity.getBody());
    }

    private UriBuilder getBaseBeerServiceURI() {
        UriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        return uriBuilderFactory
                .builder()
                .scheme(beerServiceScheme)
                .host(beerServiceHost)
                .port(beerServicePort)
                .path(BEER_SERVICE_V1);
    }

    public void setBeerServiceHost(String beerServiceHost) {
        this.beerServiceHost = beerServiceHost;
    }

    public void setBeerServicePort(String beerServicePort) {
        this.beerServicePort = beerServicePort;
    }

    public void setBeerServiceScheme(String beerServiceScheme) {
        this.beerServiceScheme = beerServiceScheme;
    }
}
