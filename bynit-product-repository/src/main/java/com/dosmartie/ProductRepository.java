package com.dosmartie;

import com.dosmartie.document.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, Integer> {
    Optional<Product> findByNameIgnoreCase(String name);
    Optional<Product> findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsColorIgnoreCaseAndVariantsSizeIgnoreCase(
            String brand,
            String name,
            String merchantEmail,
            String color,
            String size);
    Optional<Product> findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCase(String brand, String name, String merchantEmail);
}
