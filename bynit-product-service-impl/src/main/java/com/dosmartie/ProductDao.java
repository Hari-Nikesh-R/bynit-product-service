package com.dosmartie;

import com.dosmartie.document.Product;
import com.dosmartie.document.mongohelper.Variant;
import com.dosmartie.request.ProductCreateRequest;
import org.apache.zookeeper.Op;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class ProductDao {

    @Autowired
    private ProductRepository productRepository;

    public synchronized Page<Product> getProductViaSearchQuery(String searchParam, Pageable pageable) {
        return productRepository.findAllByBrandIgnoreCaseOrNameContainingIgnoreCase(searchParam, searchParam, pageable);
    }

    public synchronized Page<Product> getProductViaMerchantSearchQuery(String searchParam, String merchant, Pageable pageable) {
        return productRepository.findAllByBrandIgnoreCaseOrNameContainingIgnoreCaseAndMerchantEmailContainingIgnoreCase(searchParam, searchParam, merchant, pageable);
    }

    public synchronized List<Product> getProductViaCategory(String category, Pageable pageable) {
        return productRepository.findAllByCategoryContainingIgnoreCase(category, pageable);
    }
    public synchronized Optional<Product> getProductByVariantSku(String itemSku) {
        return productRepository.findByVariantsSku(itemSku);
    }

    public synchronized List<Product> getProductViaCategoryAndMerchant(String category, String merchant, Pageable pageable) {
        return productRepository.findAllByCategoryContainingIgnoreCaseAndMerchantEmailContainingIgnoreCase(category, merchant, pageable);
    }

    public synchronized Optional<Product> getProductVariantItemSku(String itemSku) {
        return productRepository.findByVariantsSkuIgnoreCase(itemSku);
    }

    public synchronized Optional<Product> getProductByDefaultSku(String defaultSku) {
        return productRepository.findByDefaultSku(defaultSku);
    }
    public synchronized Optional<Variant> getVariantBySku(String itemSku) {
        return productRepository.findVariantByVariantsSku(itemSku);
    }


    public synchronized Optional<Variant> getUniqueVariant(ProductCreateRequest productCreateRequest) {
        if (Objects.nonNull(productCreateRequest.getSize()) && Objects.nonNull(productCreateRequest.getColor())) {
            return productRepository.findVariantByBrandIgnoreCaseAndNameIgnoreCaseAndVariantsColorAndVariantsSize(productCreateRequest.getBrand(),productCreateRequest.getName(), productCreateRequest.getColor(), productCreateRequest.getSize());
        } else if (Objects.nonNull(productCreateRequest.getSize())) {
            return productRepository.findVariantByBrandIgnoreCaseAndNameIgnoreCaseAndVariantsSize(productCreateRequest.getBrand(),productCreateRequest.getName(), productCreateRequest.getSize());
        } else {
            return productRepository.findVariantByBrandIgnoreCaseAndNameIgnoreCaseAndVariantsColor(productCreateRequest.getBrand(),productCreateRequest.getName(),productCreateRequest.getColor());
        }
    }



    //todo: for updating variant
    public synchronized Optional<Product> getUniqueProductByVariant(ProductCreateRequest productCreateRequest) {
        if (Objects.nonNull(productCreateRequest.getSize()) && Objects.nonNull(productCreateRequest.getColor())) {
            return productRepository.findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsColorIgnoreCaseAndVariantsSizeIgnoreCase(productCreateRequest.getBrand(), productCreateRequest.getName(), productCreateRequest.getMerchantEmail(), productCreateRequest.getColor(), productCreateRequest.getSize());
        } else if (Objects.nonNull(productCreateRequest.getSize())) {
            return productRepository.findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsSizeIgnoreCase(productCreateRequest.getBrand(), productCreateRequest.getName(), productCreateRequest.getMerchantEmail(), productCreateRequest.getSize());
        } else {
            return productRepository.findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsColorIgnoreCase(productCreateRequest.getBrand(), productCreateRequest.getName(), productCreateRequest.getMerchantEmail(), productCreateRequest.getColor());
        }
    }

    //todo: For Adding variant and updating product [default]
    public synchronized Optional<Product> getUniqueProduct(ProductCreateRequest productCreateRequest) {
        return productRepository.findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCase(productCreateRequest.getBrand(), productCreateRequest.getName(), productCreateRequest.getMerchantEmail());
    }

    public synchronized List<Product> getAllProductByMerchant(String email, Pageable pageable) {
        return productRepository.findAllByMerchantEmail(email, pageable);
    }

    public synchronized Page<Product> findAllProduct(Pageable pageable) {
        return productRepository.findAll(pageable);
    }


    public synchronized void saveProduct(Product product) {
        productRepository.save(product);
    }
    public synchronized void deleteAllProduct() {
        productRepository.deleteAll();
    }
 }
