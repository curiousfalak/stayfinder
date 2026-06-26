package com.example.stayfinder.backend.controller;



import com.example.stayfinder.backend.document.PropertyDocument;
import com.example.stayfinder.backend.service.PropertySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final PropertySearchService searchService;

    @GetMapping
    public ResponseEntity<List<PropertyDocument>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minGuests,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(
                searchService.search(
                        city, minPrice, maxPrice, minGuests, keyword));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<PropertyDocument>> searchByCity(
            @PathVariable String city) {
        return ResponseEntity.ok(
                searchService.searchByCity(city));
    }
}