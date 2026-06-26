package com.example.stayfinder.backend.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.stayfinder.backend.document.PropertyDocument;
import com.example.stayfinder.backend.repository.PropertySearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertySearchService {

    private final PropertySearchRepository searchRepository;
    private final ElasticsearchClient elasticsearchClient;

    public void indexProperty(PropertyDocument document) {
        searchRepository.save(document);
    }

    public void deleteProperty(String id) {
        searchRepository.deleteById(id);
    }

    public List<PropertyDocument> searchByCity(String city) {
        return searchRepository.findByCity(city);
    }

    public List<PropertyDocument> search(
            String city,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minGuests,
            String keyword) {

        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // Filter by city
            if (city != null && !city.isBlank()) {
                boolQuery.filter(f -> f
                        .term(t -> t
                                .field("city")
                                .value(city)
                        )
                );
            }

            // Filter by min price
            if (minPrice != null) {
                boolQuery.filter(f -> f
                        .range(r -> r
                                .number(n -> n
                                        .field("pricePerNight")
                                        .gte(minPrice.doubleValue())
                                )
                        )
                );
            }

            // Filter by max price
            if (maxPrice != null) {
                boolQuery.filter(f -> f
                        .range(r -> r
                                .number(n -> n
                                        .field("pricePerNight")
                                        .lte(maxPrice.doubleValue())
                                )
                        )
                );
            }

            // Filter by minimum guests
            if (minGuests != null) {
                boolQuery.filter(f -> f
                        .range(r -> r
                                .number(n -> n
                                        .field("maxGuests")
                                        .gte(minGuests.doubleValue())
                                )
                        )
                );
            }

            // Full text search on title and description
            if (keyword != null && !keyword.isBlank()) {
                boolQuery.must(m -> m
                        .multiMatch(mm -> mm
                                .fields("title", "description")
                                .query(keyword)
                        )
                );
            }

            SearchResponse<PropertyDocument> response =
                    elasticsearchClient.search(s -> s
                                    .index("properties")
                                    .query(q -> q
                                            .bool(boolQuery.build())
                                    ),
                            PropertyDocument.class
                    );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Search failed: " + e.getMessage());
        }
    }
}