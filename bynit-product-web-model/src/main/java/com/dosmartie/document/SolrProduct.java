package com.dosmartie.document;

import com.dosmartie.document.mongohelper.Variant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.stereotype.Component;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
@SolrDocument(solrCoreName = "product_search")
public class SolrProduct {
    @Id
    @Indexed(name = "id", type = "string")
    private String id;
    @Indexed(name = "defaultSku", type = "string")
    private String defaultSku;
    @Indexed(name = "name", type = "string")
    private String name;
    private List<Variant> variants;
    @Indexed(name = "brand", type = "string")
    private String brand;
    @Indexed(name = "description", type = "string")
    private String description;
    @Indexed(name = "category", type = "string")
    private String category;
    @Indexed(name = "rating", type = "double")
    private double rating;
    private String merchantEmail;
    private int counter;
}
