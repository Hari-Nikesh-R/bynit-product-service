package com.dosmartie;

import com.dosmartie.document.Product;
import com.dosmartie.document.mongohelper.Variant;
import com.dosmartie.helper.DefaultSkuGenerator;
import com.dosmartie.helper.ResponseMessage;
import com.dosmartie.request.ProductCreateRequest;
import com.dosmartie.response.BaseResponse;
import com.dosmartie.response.ProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@Service
@Slf4j
public class ProductServiceImpl implements ProductService {
    private static final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    @Autowired
    private final ObjectMapper mapper;
    @Autowired
    private final ProductRepository productRepository;
    @Autowired
    private final ResponseMessage<String> responseMessage;

    public ProductServiceImpl(ObjectMapper mapper, ProductRepository productRepository, ResponseMessage<String> responseMessage) {
        this.mapper = mapper;
        this.productRepository = productRepository;
        this.responseMessage = responseMessage;
    }

    @Override
    public ResponseEntity<BaseResponse<String>> addOrUpdateProduct(ProductCreateRequest productRequest) throws IOException {
        try {

            Optional<Product> optionalProduct = getUniqueProduct(productRequest);
            return optionalProduct.map(product -> {
                        //todo: Add variant
                        Optional<Product> productByVariant = getUniqueProductByVariant(productRequest);
//                    updateProduct(productRequest)
                    return ResponseEntity.ok(responseMessage.setSuccessResponse("Product updated", null));
                    }
            ).orElseGet(() -> createNewProduct(productRequest));
        } catch (Exception exception) {
            log.error("thrown an exception: " + exception.getMessage());
            return ResponseEntity.ok(responseMessage.setFailureResponse("Failed to do operation", exception));
        }
    }

    @Override
    public ResponseEntity<BaseResponse<ProductResponse>> getProduct(String productName) {
        return null;
    }

    @Override
    public ResponseEntity<BaseResponse<String>> deleteProduct(String productName) {
        return null;
    }

    @Override
    public ResponseEntity<BaseResponse<String>> deleteAllProduct() {
        return null;
    }

    @Override
    public BaseResponse<List<ProductResponse>> getAllProductsFromInventory() {
        return null;
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

//    private ResponseEntity<BaseResponse<String>> updateProduct(ProductCreateRequest productRequest) {
//        try {
//            Optional<Product> optionalProduct = getProductByName(productRequest.getName());
//            return optionalProduct.map((product -> {
//                product.getVariants().product.getProductQuantity() + productRequest.getProductQuantity());
//                if (Objects.nonNull(productRequest.getProductPrice())) {
//                    product.setProductPrice(productRequest.getProductPrice());
//                }
//                product.setProductName(productRequest.getProductName());
//                productRepository.save(product);
//                return ResponseEntity.ok(new BaseResponse<>("Product Updated", null, true, HttpStatus.OK.value()));
//            })).orElseGet(() -> ResponseEntity.ok(new BaseResponse<>(null, "Product not updated", false, HttpStatus.NO_CONTENT.value())));
//        } catch (Exception exception) {
//            return ResponseEntity.ok(new BaseResponse<>(null, exception.getMessage(), false, HttpStatus.INTERNAL_SERVER_ERROR.value()));
//        }
//    }

    private String defaultSkuGenerator(String name) {
        return DefaultSkuGenerator.generateDefaultSku(name);
    }

    private Product constructProduct(ProductCreateRequest productCreateRequest) {
        Product product = new Product();
        product.setMerchantEmail(productCreateRequest.getMerchantEmail());
        product.setName(productCreateRequest.getName());
        product.setDefaultSku(defaultSkuGenerator(productCreateRequest.getName()));
        product.setBrand(productCreateRequest.getBrand());

        product.setVariants(setVariant(product, productCreateRequest));
        return product;
    }

    private List<Variant> setVariant(Product product, ProductCreateRequest productCreateRequest) {
        List<Variant> variants = new ArrayList<>();
        Variant variant = new Variant();
        variant.setQuantity(productCreateRequest.getQuantity());
        variant.setSku(skuIncrementor(product));
        variant.setColor(productCreateRequest.getColor());
        variant.setImages(productCreateRequest.getImages());
        variant.setSize(productCreateRequest.getSize());
        variants.add(variant);
        return variants;
    }

    private String skuIncrementor(Product product) {
        product.setCounter(product.getCounter() + 1);
        return product.getDefaultSku() + "-" + product.getCounter();
    }

    //
//    @Override
//    public ResponseEntity<BaseResponse<ProductResponse>> getProduct(String productName) {
//        try {
//            Optional<Product> optionalProduct = getProductByName(productName);
//            return optionalProduct.map(product -> {
//                ProductResponse productResponse = new ProductResponse();
//                BeanUtils.copyProperties(optionalProduct.get(), productResponse);
//                return ResponseEntity.ok(new BaseResponse<>(productResponse, null, true, HttpStatus.OK.value()));
//            }).orElseGet(() -> ResponseEntity.ok(new BaseResponse<>(null, "Product Not found", true, HttpStatus.NO_CONTENT.value())));
//        } catch (Exception exception) {
//            return ResponseEntity.ok(new BaseResponse<>(null, exception.getMessage(), false, HttpStatus.INTERNAL_SERVER_ERROR.value()));
//        }
//    }
//
//    @Override
//    public ResponseEntity<BaseResponse<Map<String, List<Report>>>> generateReport() {
//        try {
//            double totalValue = 0;
//            Map<String, List<Report>> doubleListMap = new HashMap<>();
//            List<Report> reports = new Vector<>();
//            List<Product> productList = productRepository.findAll();
//            for (Product product : productList) {
//                totalValue += product.getProductPrice() * product.getProductQuantity();
//                reports.add(new Report(product.getProductName(), product.getProductPrice(), product.getProductQuantity()));
//            }
//            doubleListMap.put("Total value of inventory: " + totalValue, reports);
//            return ResponseEntity.ok(new BaseResponse<>(doubleListMap, null, true, HttpStatus.OK.value()));
//        } catch (Exception exception) {
//            return ResponseEntity.ok(new BaseResponse<>(new HashMap<>(), exception.getMessage(), false, HttpStatus.INTERNAL_SERVER_ERROR.value()));
//        }
//    }
//
//    @Override
//    public ResponseEntity<BaseResponse<String>> deleteProduct(String productName) {
//        try {
//            Optional<Product> optionalProduct = getProductByName(productName);
//            if (optionalProduct.isPresent()) {
//                productRepository.delete(optionalProduct.get());
//                return ResponseEntity.ok(new BaseResponse<>("Deleted successfully", null, true, HttpStatus.OK.value()));
//            } else {
//                return ResponseEntity.ok(new BaseResponse<>(null, "No Product found", false, HttpStatus.NO_CONTENT.value()));
//            }
//        } catch (Exception exception) {
//            return ResponseEntity.ok(new BaseResponse<>(null, exception.getMessage(), false, HttpStatus.INTERNAL_SERVER_ERROR.value()));
//        }
//    }
//
//    @Override
//    public ResponseEntity<BaseResponse<String>> deleteAllProduct() {
//        try {
//            productRepository.deleteAll();
//            return ResponseEntity.ok(new BaseResponse<>("Deleted successfully", null, true, HttpStatus.OK.value()));
//        } catch (Exception exception) {
//            return ResponseEntity.ok(new BaseResponse<>(null, exception.getMessage(), false, HttpStatus.INTERNAL_SERVER_ERROR.value()));
//        }
//    }
//
//    @Override
//    public BaseResponse<List<ProductResponse>> getAllProductsFromInventory() {
//        try {
//            List<ProductResponse> productResponses = mapper.convertValue(productRepository.findAll(), new TypeReference<>() {
//            });
//            return new BaseResponse<>(productResponses, null, true, HttpStatus.OK.value());
//        } catch (Exception exception) {
//            return new BaseResponse<>(null, exception.getMessage(), false, HttpStatus.INTERNAL_SERVER_ERROR.value());
//        }
//    }
//
//    @Override
//    public synchronized ProductQuantityCheckResponse[] checkStock(List<PurchaseRequest> productRequest) {
//        List<ProductQuantityCheckResponse> productQuantityCheckResponses = new ArrayList<>();
//        try {
//            List<Product> productList = productRepository.findAll();
//            productRequest.forEach(product -> {
//                productQuantityCheckResponses.add(getProductByQuantity(productList, product));
//            });
//        } catch (Exception exception) {
//            exception.printStackTrace();
//            productQuantityCheckResponses.add(new ProductQuantityCheckResponse(false, exception.getMessage(), null));
//        }
//        return productQuantityCheckResponses.toArray(ProductQuantityCheckResponse[]::new);
//    }
//
//    private ProductQuantityCheckResponse getProductByQuantity(List<Product> productList, PurchaseRequest purchaseRequest) {
//        Optional<Product> product = productList.stream().filter(prod -> prod.getProductName(
//        ).equalsIgnoreCase(purchaseRequest.getProductName())).findFirst();
//        if (product.isPresent()) {
//            if (!(product.get().getProductQuantity() >= purchaseRequest.getQuantity())) {
//                return new ProductQuantityCheckResponse(false, product.get().getProductName() + " out of stock", null);
//            } else {
//                ProductResponse productResponse = new ProductResponse();
//                BeanUtils.copyProperties(product.get(), productResponse);
//                return new ProductQuantityCheckResponse(true, "Product available", productResponse);
//            }
//        } else {
//            return new ProductQuantityCheckResponse(false, "No product found", null);
//        }
//    }
//
//
//    private synchronized Optional<Product> getProductBySku(String itemSku) {
//        return productRepository.findProductContainingDefaultSku(itemSku);
//    }
    private synchronized Optional<Product> getProductByName(String name) {
        return productRepository.findByNameIgnoreCase(name);
    }

    //todo: for updating variant
    private synchronized Optional<Product> getUniqueProductByVariant(ProductCreateRequest productCreateRequest) {
        return productRepository.findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCaseAndVariantsColorIgnoreCaseAndVariantsSizeIgnoreCase(productCreateRequest.getBrand(), productCreateRequest.getName(), productCreateRequest.getMerchantEmail(), productCreateRequest.getColor(), productCreateRequest.getSize());
    }

    //todo: For Adding variant and updating product [default]
    private synchronized Optional<Product> getUniqueProduct(ProductCreateRequest productCreateRequest) {
        return productRepository.findByBrandIgnoreCaseAndNameIgnoreCaseAndMerchantEmailIgnoreCase(productCreateRequest.getBrand(), productCreateRequest.getName(), productCreateRequest.getMerchantEmail());
    }
}
