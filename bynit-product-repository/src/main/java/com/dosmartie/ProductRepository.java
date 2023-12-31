package com.dosmartie;

import com.dosmartie.document.Product;
import com.dosmartie.document.mongohelper.Variant;
import org.apache.zookeeper.Op;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, Integer> {

    // todo: Can be used as alternative for search for now
    Page<Product> findAllByBrandIgnoreCaseOrNameContainingIgnoreCase(String brand, String name, Pageable pageable);

    Optional<Product> findByVariantsSku(String itemSku);
    Optional<Variant> findVariantByVariantsSku(String itemSku);

    Page<Product> findAllByBrandIgnoreCaseOrNameContainingIgnoreCaseAndMerchantEmailContainingIgnoreCase(String brand, String name, String merchant, Pageable pageable);

    List<Product> findAllByCategoryContainingIgnoreCase(String category, Pageable pageable);

    List<Product> findAllByCategoryContainingIgnoreCaseAndMerchantEmailContainingIgnoreCase(String category, String merchant, Pageable pageable);

    List<Product> findAllByMerchantEmail(String name, Pageable pageable);

    Optional<Product> findByVariantsSkuIgnoreCase(String itemSku);

    Optional<Product> findByDefaultSku(String defaultSku);

    Optional<Product> findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsColorIgnoreCaseAndVariantsSizeIgnoreCase(
            String brand,
            String name,
            String merchantEmail,
            String color,
            String size);

    Optional<Variant> findVariantByBrandIgnoreCaseAndNameIgnoreCaseAndVariantsColorAndVariantsSize(String brand, String name, String color, String size);
    Optional<Variant> findVariantByBrandIgnoreCaseAndNameIgnoreCaseAndVariantsColor(String brand, String name, String color);
    Optional<Variant> findVariantByBrandIgnoreCaseAndNameIgnoreCaseAndVariantsSize(String brand, String name, String size);

    Optional<Product> findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsSizeIgnoreCase(
            String brand,
            String name,
            String merchantEmail,
            String size);

    Optional<Product> findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsColorIgnoreCase(
            String brand,
            String name,
            String merchantEmail,
            String color);

    Optional<Product> findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCase(String brand, String name, String merchantEmail);
}
