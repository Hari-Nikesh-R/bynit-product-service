package com.dosmartie.document.mongohelper;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Variant {
    private String sku;
    private String color;
    private String size;
    private Double price;
    private Integer quantity;
    private List<String> images;
}
