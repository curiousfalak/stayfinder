package com.example.stayfinder.backend.document;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Document(indexName = "properties")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String country;

    @Field(type = FieldType.Double)
    private BigDecimal pricePerNight;

    @Field(type = FieldType.Integer)
    private int maxGuests;

    @Field(type = FieldType.Double)
    private double avgRating;

    @Field(type = FieldType.Keyword)
    private List<String> amenities;

    @Field(type = FieldType.Keyword)
    private List<String> imageUrls;
}
