package com.dosmartie.document;

import com.dosmartie.document.auditing.MongoBaseAuditing;
import com.dosmartie.document.mongohelper.Variant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "product")
@AllArgsConstructor
@NoArgsConstructor
public class Product extends MongoBaseAuditing {
    @Id
    private String id;
    private String defaultSku;
    private String name;
    private List<Variant> variants;
    private String brand;
    @CreatedBy
    private String merchantEmail;
    private int counter;
}
