package com.example.stayfinder.backend.service;

import com.example.stayfinder.backend.document.PropertyDocument;
import com.example.stayfinder.backend.repository.PropertySearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertySearchService {

    private final PropertySearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

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

        Criteria criteria = new Criteria();

        if (city != null && !city.isBlank()) {
            criteria = criteria.and(
                    new Criteria("city").is(city));
        }

        if (minPrice != null) {
            criteria = criteria.and(
                    new Criteria("pricePerNight")
                            .greaterThanEqual(minPrice.doubleValue()));
        }

        if (maxPrice != null) {
            criteria = criteria.and(
                    new Criteria("pricePerNight")
                            .lessThanEqual(maxPrice.doubleValue()));
        }

        if (minGuests != null) {
            criteria = criteria.and(
                    new Criteria("maxGuests")
                            .greaterThanEqual(minGuests));
        }

        if (keyword != null && !keyword.isBlank()) {
            Criteria titleMatch =
                    new Criteria("title").contains(keyword);
            Criteria descMatch =
                    new Criteria("description").contains(keyword);
            criteria = criteria.and(titleMatch.or(descMatch));
        }

        CriteriaQuery query = new CriteriaQuery(criteria);

        return elasticsearchOperations
                .search(query, PropertyDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
}