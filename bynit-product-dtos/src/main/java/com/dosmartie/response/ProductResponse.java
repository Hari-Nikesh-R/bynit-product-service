package com.dosmartie.response;

import com.dosmartie.document.mongohelper.Variant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private String defaultSku;
    private String name;
    private List<Variant> variants;
    private String brand;
    private String merchantEmail;
    private String description;
    private String category;
    private double overAllRating;

}
