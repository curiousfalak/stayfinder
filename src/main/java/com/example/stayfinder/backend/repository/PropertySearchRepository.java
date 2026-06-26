package com.example.stayfinder.backend.repository;



import com.example.stayfinder.backend.document.PropertyDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PropertySearchRepository
        extends ElasticsearchRepository<PropertyDocument, String> {

    List<PropertyDocument> findByCity(String city);

    List<PropertyDocument> findByCityAndMaxGuestsGreaterThanEqual(
            String city, int maxGuests);
}