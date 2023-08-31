package com.dosmartie.response;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartProductResponse implements Serializable {
    private String defaultSku;
    private String name;
    private String sku;
    private String color;
    private String size;
    private double price;
    private Integer quantity;
    private String brand;
    private String category;
    private Map<String, String> reviews;
    private boolean isRated;
    private double ratingBasedOnOrder;
}
