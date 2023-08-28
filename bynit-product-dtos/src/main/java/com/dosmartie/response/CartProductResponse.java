package com.dosmartie.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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
}
