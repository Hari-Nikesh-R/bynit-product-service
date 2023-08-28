package com.dosmartie;

import com.dosmartie.document.Product;
import com.dosmartie.document.mongohelper.Variant;
import com.dosmartie.helper.DefaultSkuGenerator;
import com.dosmartie.helper.ResponseMessage;
import com.dosmartie.request.CartProductRequest;
import com.dosmartie.request.ProductCreateRequest;
import com.dosmartie.response.BaseResponse;
import com.dosmartie.response.CartProductResponse;
import com.dosmartie.response.ProductQuantityCheckResponse;
import com.dosmartie.response.ProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


@Service
@Slf4j
public class ProductServiceImpl implements ProductService {
    private static final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    //todo: Mapstruct

    private final ObjectMapper mapper;
    private final ProductRepository productRepository;
    private final ResponseMessage<String> responseMessage;
    private final ResponseMessage<List<ProductResponse>> productResponseResponseMessage;

    @Autowired
    public ProductServiceImpl(ObjectMapper mapper, ProductRepository productRepository, ResponseMessage<String> responseMessage, ResponseMessage<List<ProductResponse>> productResponseResponseMessage) {
        this.mapper = mapper;
        this.productRepository = productRepository;
        this.responseMessage = responseMessage;
        this.productResponseResponseMessage = productResponseResponseMessage;
    }

    @Override
    public synchronized ResponseEntity<BaseResponse<String>> addOrUpdateProduct(ProductCreateRequest productRequest) throws IOException {
        try {
            Optional<Product> optionalProduct = getUniqueProduct(productRequest);
            return optionalProduct.map(product -> updateProduct(productRequest, product)).orElseGet(() -> createNewProduct(productRequest));
        } catch (Exception exception) {
            log.error("thrown an exception: " + exception.getMessage());
            return ResponseEntity.ok(responseMessage.setFailureResponse("Failed to do operation", exception));
        }
    }

    @Override
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getAllProductViaCategory(String category, int page, int size, String merchant) {
        try {
            List<Product> productList = Objects.isNull(merchant) ? getProductViaCategory(category, PageRequest.of(page, size)) : getProductViaCategoryAndMerchant(category, merchant, PageRequest.of(page, size));
            return ResponseEntity.ok(productResponseResponseMessage.setSuccessResponse("Fetched category Result", loadProductToProductResponse(productList)));
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseEntity.ok(productResponseResponseMessage.setFailureResponse("Unable to retrieve result", exception));
        }
    }

    @Override
    public ResponseEntity<BaseResponse<String>> deleteAllProduct() {
        try {
            productRepository.deleteAll();
            return ResponseEntity.ok(responseMessage.setSuccessResponse("All product Deleted successfully", null));
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product", exception));
        }
    }

    @Override
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getProduct(String searchParam, int page, int size, String merchant, String authId) {
        try {
            List<Product> productList = Objects.nonNull(merchant) ? getProductViaMerchantSearchQuery(searchParam, merchant, PageRequest.of(page, size)) : getProductViaSearchQuery(searchParam, PageRequest.of(page, size));
            return ResponseEntity.ok(productResponseResponseMessage.setSuccessResponse("Fetched Result", loadProductToProductResponse(productList)));
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseEntity.ok(productResponseResponseMessage.setFailureResponse("Unable to retrieve result", exception));
        }
    }

    @Override
    public BaseResponse<List<ProductResponse>> getAllProductsFromInventory(int page, int size, String merchant, String authId) {
        try {
            return productResponseResponseMessage.setSuccessResponse("Fetched result", Objects.nonNull(merchant) ? loadProductToProductResponse(productRepository.findAllByMerchantEmail(merchant, PageRequest.of(page, size))) : loadProductToProductResponse(productRepository.findAll(PageRequest.of(page, size))));
        } catch (Exception exception) {
            exception.printStackTrace();
            return productResponseResponseMessage.setFailureResponse("Unable to fetch result", exception);
        }
    }

    @Override
    public ResponseEntity<BaseResponse<String>> deleteProduct(String defaultSku) {
        try {
            Optional<Product> optionalProduct = getProductByDefaultSku(defaultSku);
            return optionalProduct.map(product -> {
                productRepository.delete(product);
                return ResponseEntity.ok(responseMessage.setSuccessResponse("Product deleted successfully", null));
            }).orElseGet(() -> ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product")));
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product variant", exception));
        }
    }


    private ResponseEntity<BaseResponse<String>> updateProduct(ProductCreateRequest productCreateRequest, Product product) {
        try {
            Optional<Product> optionalProduct = getUniqueProductByVariant(productCreateRequest);
            return optionalProduct.map((variant) -> ResponseEntity.ok(responseMessage.setFailureResponse("Product variant already exist"))).orElseGet(() -> {
                product.getVariants().add(constructVariant(productCreateRequest, product));
                productRepository.save(product);
                return ResponseEntity.ok(responseMessage.setSuccessResponse("Variant updated", null));
            });

        } catch (Exception exception) {
            exception.printStackTrace();
            log.info(exception.fillInStackTrace().getLocalizedMessage());
            return ResponseEntity.ok(responseMessage.setFailureResponse("Product Not updated", exception));
        }
    }

    @Override
    public ResponseEntity<BaseResponse<String>> deleteProductVariant(String itemSku) {
        try {
            Optional<Product> optionalProduct = getProductVariantItemSku(itemSku);
            return optionalProduct.map(product -> product.getVariants().stream().filter(variant -> variant.getSku().equalsIgnoreCase(itemSku)).findFirst().map((productVariant) -> {
                product.getVariants().remove(productVariant);
                if (product.getVariants().size() == 0) {
                    productRepository.delete(product);
                } else {
                    productRepository.save(product);
                }
                return ResponseEntity.ok(responseMessage.setSuccessResponse("Removed variant successfully", null));
            }).orElseGet(() -> ResponseEntity.ok(responseMessage.setFailureResponse("No variant found")))).orElseGet(() -> ResponseEntity.ok(responseMessage.setFailureResponse("No product with variant found")));
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product variant", exception));
        }
    }

    private ResponseEntity<BaseResponse<String>> createNewProduct(ProductCreateRequest productCreateRequest) {
        try {
            productRepository.save(constructProduct(productCreateRequest));
            log.info("Product saved in database");
            return ResponseEntity.ok(responseMessage.setSuccessResponse("Product added successfully", null));
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Product not added successfully", exception));
        }
    }

    private List<ProductResponse> loadProductToProductResponse(List<Product> productList) {
        List<ProductResponse> productResponses = new ArrayList<>();
        productList.forEach(product -> productResponses.add(mapper.convertValue(product, ProductResponse.class)));
        return productResponses;
    }

    private List<ProductResponse> loadProductToProductResponse(Page<Product> productList) {
        List<ProductResponse> productResponses = new ArrayList<>();
        productList.forEach(product -> productResponses.add(mapper.convertValue(product, ProductResponse.class)));
        return productResponses;
    }

    private String defaultSkuGenerator(String name) {
        return DefaultSkuGenerator.generateDefaultSku(name);
    }

    private Product constructProduct(ProductCreateRequest productCreateRequest) {
        Product product = new Product();
        product.setMerchantEmail(productCreateRequest.getMerchantEmail());
        product.setName(productCreateRequest.getName());
        product.setDefaultSku(defaultSkuGenerator(productCreateRequest.getName()));
        product.setBrand(productCreateRequest.getBrand().toUpperCase());
        product.setCategory(productCreateRequest.getCategory().toUpperCase());
        product.setDescription(productCreateRequest.getDescription());
        product.setVariants(setVariant(product, productCreateRequest));
        return product;
    }

    private List<Variant> setVariant(Product product, ProductCreateRequest productCreateRequest) {
        List<Variant> variants = new ArrayList<>();
        variants.add(constructVariant(productCreateRequest, product));
        return variants;
    }

    private Variant constructVariant(ProductCreateRequest productCreateRequest, Product product) {
        Variant variant = new Variant();
        variant.setQuantity(productCreateRequest.getQuantity());
        variant.setPrice(productCreateRequest.getPrice());
        variant.setSku(skuIncrementor(product));
        variant.setColor(productCreateRequest.getColor());
        variant.setImages(productCreateRequest.getImages());
        variant.setSize(productCreateRequest.getSize());
        return variant;
    }


    private String skuIncrementor(Product product) {
        product.setCounter(product.getCounter() + 1);
        return product.getDefaultSku() + "-" + product.getCounter();
    }

    @Override
    public synchronized ProductQuantityCheckResponse[] checkStock(List<CartProductRequest> productRequest) {
        List<ProductQuantityCheckResponse> productQuantityCheckResponses = new ArrayList<>();
        try {
            productRequest.forEach(product -> productRepository.findByVariantsSku(product.getSku()).ifPresent(dbProduct -> productQuantityCheckResponses.add(getProductQuantity(dbProduct, product))));
        } catch (Exception exception) {
            exception.printStackTrace();
            productQuantityCheckResponses.add(new ProductQuantityCheckResponse(false, exception.getMessage(), null));
        }
        return productQuantityCheckResponses.toArray(ProductQuantityCheckResponse[]::new);
    }

    private ProductQuantityCheckResponse getProductQuantity(Product product, CartProductRequest cartProductRequest) {
        AtomicReference<ProductQuantityCheckResponse> productQuantityCheckResponse = new AtomicReference<>(new ProductQuantityCheckResponse());
        product.getVariants().forEach(variant -> {
            if (!(variant.getQuantity() >= cartProductRequest.getQuantity())) {
                productQuantityCheckResponse.set(new ProductQuantityCheckResponse(false, product.getName() + " out of stock", null));
            } else {
                CartProductResponse productResponse = new CartProductResponse();
                BeanUtils.copyProperties(product, productResponse);
                if (Objects.nonNull(copyVariantToCartRequest(product.getVariants(), productResponse, cartProductRequest))) {
                    productQuantityCheckResponse.set(new ProductQuantityCheckResponse(true, "Product available", productResponse));
                } else {
                    productQuantityCheckResponse.set(new ProductQuantityCheckResponse(true, "variant not available", productResponse));
                }
            }
        });
        return productQuantityCheckResponse.get();
    }

    private CartProductResponse copyVariantToCartRequest(List<Variant> variant, CartProductResponse productResponse, CartProductRequest cartProductRequest) {
        Variant prodVariant = variant.stream().filter(productVariant -> productVariant.getSku().equalsIgnoreCase(cartProductRequest.getSku())).findFirst().orElseGet(() -> null);
        if (Objects.nonNull(prodVariant)) {
            productResponse.setSize(prodVariant.getSize());
            productResponse.setPrice(prodVariant.getPrice());
            productResponse.setSku(prodVariant.getSku());
            productResponse.setColor(prodVariant.getColor());
            return productResponse;
        } else {
            return null;
        }
    }

//    private ProductQuantityCheckResponse getProductByQuantity(List<Product> productList, CartProductRequest purchaseRequest) {
//        Optional<Product> product = productList.stream().filter(prod -> prod.getProductName(
//        ).equalsIgnoreCase(purchaseRequest.getSku())).findFirst();
//        if (product.isPresent()) {
//            if (!(product.get().getProductQuantity() >= purchaseRequest.getQuantity())) {
//                return new ProductQuantityCheckResponse(false, product.get().getName() + " out of stock", null);
//            } else {
//                ProductResponse productResponse = new ProductResponse();
//                BeanUtils.copyProperties(product.get(), productResponse);
//                return new ProductQuantityCheckResponse(true, "Product available", productResponse);
//            }
//        } else {
//            return new ProductQuantityCheckResponse(false, "No product found", null);
//        }
//    }


    // Database query sections;
    private synchronized List<Product> getProductViaSearchQuery(String searchParam, Pageable pageable) {
        return productRepository.findAllByBrandIgnoreCaseOrNameContainingIgnoreCase(searchParam, searchParam, pageable);
    }

    private synchronized List<Product> getProductViaMerchantSearchQuery(String searchParam, String merchant, Pageable pageable) {
        return productRepository.findAllByBrandIgnoreCaseOrNameContainingIgnoreCaseAndMerchantEmailContainingIgnoreCase(searchParam, searchParam, merchant, pageable);
    }

    private synchronized List<Product> getProductViaCategory(String category, Pageable pageable) {
        return productRepository.findAllByCategoryContainingIgnoreCase(category, pageable);
    }

    private synchronized List<Product> getProductViaCategoryAndMerchant(String category, String merchant, Pageable pageable) {
        return productRepository.findAllByCategoryContainingIgnoreCaseAndMerchantEmailContainingIgnoreCase(category, merchant, pageable);
    }

    private synchronized Optional<Product> getProductVariantItemSku(String itemSku) {
        return productRepository.findByVariantsSkuIgnoreCase(itemSku);
    }

    private synchronized Optional<Product> getProductByDefaultSku(String defaultSku) {
        return productRepository.findByDefaultSku(defaultSku);
    }

    //todo: for updating variant
    private synchronized Optional<Product> getUniqueProductByVariant(ProductCreateRequest productCreateRequest) {
        if (Objects.nonNull(productCreateRequest.getSize()) && Objects.nonNull(productCreateRequest.getColor())) {
            return productRepository.findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsColorIgnoreCaseAndVariantsSizeIgnoreCase(productCreateRequest.getBrand(), productCreateRequest.getName(), productCreateRequest.getMerchantEmail(), productCreateRequest.getColor(), productCreateRequest.getSize());
        } else if (Objects.nonNull(productCreateRequest.getSize())) {
            return productRepository.findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsSizeIgnoreCase(productCreateRequest.getBrand(), productCreateRequest.getName(), productCreateRequest.getMerchantEmail(), productCreateRequest.getSize());
        } else {
            return productRepository.findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsColorIgnoreCase(productCreateRequest.getBrand(), productCreateRequest.getName(), productCreateRequest.getMerchantEmail(), productCreateRequest.getColor());
        }
    }

    //todo: For Adding variant and updating product [default]
    private synchronized Optional<Product> getUniqueProduct(ProductCreateRequest productCreateRequest) {
        return productRepository.findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCase(productCreateRequest.getBrand(), productCreateRequest.getName(), productCreateRequest.getMerchantEmail());
    }
}
