package com.dosmartie.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductCreateRequest {
    @Pattern(regexp = "^[a-zA-Z_ ]*$", message = "Invalid brand")
    private String brand;
    @Pattern(regexp = "^[a-zA-Z_ ]*$", message = "Invalid ProductName")
    private String name;
    private String color;
    private String size;
    private String sku;
    @NotNull(message = "Price must not be null")
    @Positive(message = "Price cannot be negative")
    private Double price;
    private List<String> images;
    @NotNull(message = "Quantity must not be null")
    @Positive(message = "Quantity must not be negative or zero")
    private Integer quantity;
    private String description;
    private String category;
    private String merchantEmail;
}
