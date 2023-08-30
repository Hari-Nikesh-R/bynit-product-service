package com.dosmartie;

import com.dosmartie.document.Product;
import com.dosmartie.document.mongohelper.Variant;
import com.dosmartie.helper.DefaultSkuGenerator;
import com.dosmartie.helper.PropertiesCollector;
import com.dosmartie.helper.ResponseMessage;
import com.dosmartie.request.CartProductRequest;
import com.dosmartie.request.ProductCreateRequest;
import com.dosmartie.response.BaseResponse;
import com.dosmartie.response.CartProductResponse;
import com.dosmartie.response.ProductQuantityCheckResponse;
import com.dosmartie.response.ProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private final PropertiesCollector propertiesCollector;

    @Autowired
    public ProductServiceImpl(ObjectMapper mapper, ProductRepository productRepository, ResponseMessage<String> responseMessage, ResponseMessage<List<ProductResponse>> productResponseResponseMessage, PropertiesCollector propertiesCollector) {
        this.mapper = mapper;
        this.productRepository = productRepository;
        this.responseMessage = responseMessage;
        this.productResponseResponseMessage = productResponseResponseMessage;
        this.propertiesCollector = propertiesCollector;
    }

    @Override
    public ResponseEntity<BaseResponse<String>> addOrUpdateProduct(ProductCreateRequest productRequest, String authId) throws IOException {
        try {
            if (decryptAuthIdAndValidateRequest(authId)) {
                Optional<Product> optionalProduct = getUniqueProduct(productRequest);
                return optionalProduct.map(product -> updateProduct(productRequest, product))
                        .orElseGet(() -> createNewProduct(productRequest));
            } else {
                return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Access denied"));
            }
        } catch (Exception exception) {
            log.error("thrown an exception: " + exception.getMessage());
            return ResponseEntity.ok(responseMessage.setFailureResponse("Failed to do operation", exception));
        }
    }

    @Override
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getAllProductViaCategory(String category, int page, int size, String merchant, String authId) {
        try {
            if (decryptAuthIdAndValidateRequest(authId)) {
                List<Product> productList = Objects.isNull(merchant) ? getProductViaCategory(category, PageRequest.of(page, size)) : getProductViaCategoryAndMerchant(category, merchant, PageRequest.of(page, size));
                return ResponseEntity.ok(productResponseResponseMessage.setSuccessResponse("Fetched category Result", loadProductToProductResponse(productList)));
            }
            else {
                return ResponseEntity.ok(productResponseResponseMessage.setUnauthorizedResponse("Access denied"));
            }
            } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseEntity.ok(productResponseResponseMessage.setFailureResponse("Unable to retrieve result", exception));
        }
    }

    @Override
    public ResponseEntity<BaseResponse<String>> deleteAllProduct(String authId, String email) {
        try {
            if (decryptAuthIdAndValidateRequest(authId)) {
                productRepository.deleteAll();
                return ResponseEntity.ok(responseMessage.setSuccessResponse("All product Deleted successfully", null));
            }
            else{
                return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Access denied"));
            }
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product", exception));
        }
    }

    @Override
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getProduct(String searchParam, int page, int size, String merchant, String authId) {
        try {
            if (decryptAuthIdAndValidateRequest(authId)) {
                List<Product> productList = Objects.nonNull(merchant) ? getProductViaMerchantSearchQuery(searchParam, merchant, PageRequest.of(page, size)) : getProductViaSearchQuery(searchParam, PageRequest.of(page, size));
                return ResponseEntity.ok(productResponseResponseMessage.setSuccessResponse("Fetched Result", loadProductToProductResponse(productList)));
            }
            else{
                return ResponseEntity.ok(productResponseResponseMessage.setUnauthorizedResponse("Access denied"));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseEntity.ok(productResponseResponseMessage.setFailureResponse("Unable to retrieve result", exception));
        }
    }

    @Override
    public BaseResponse<List<ProductResponse>> getAllProductsFromInventory(int page, int size, String merchant, String authId) {
        try {
            if(decryptAuthIdAndValidateRequest(authId)) {
                return productResponseResponseMessage.setSuccessResponse("Fetched result", Objects.nonNull(merchant) ? loadProductToProductResponse(productRepository.findAllByMerchantEmail(merchant, PageRequest.of(page, size))) : loadProductToProductResponse(productRepository.findAll(PageRequest.of(page, size))));
            }
            else {
                return productResponseResponseMessage.setUnauthorizedResponse("Access denied");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return productResponseResponseMessage.setFailureResponse("Unable to fetch result", exception);
        }
    }

    @Override
    public ResponseEntity<BaseResponse<String>> deleteProduct(String defaultSku, String authId, String email) {
        try {
            if(decryptAuthIdAndValidateRequest(authId)) {
                Optional<Product> optionalProduct = getProductByDefaultSku(defaultSku);
                return optionalProduct.map(product -> {
                    if (product.getMerchantEmail().equalsIgnoreCase(email)) {
                        productRepository.delete(product);
                        return ResponseEntity.ok(responseMessage.setSuccessResponse("Product deleted successfully", null));
                    } else {
                        return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Don't have access to delete product"));
                    }
                }).orElseGet(() -> ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product")));
            }
            else {
                return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Access denied"));
            }
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product variant", exception));
        }
    }


    @Override
    public ResponseEntity<BaseResponse<String>> deleteProductVariant(String itemSku, String authId, String email) {
        try {
            if (validateRequest(authId)) {
                Optional<Product> optionalProduct = getProductVariantItemSku(itemSku);
                return optionalProduct.map(product -> {
                    if (product.getMerchantEmail().equalsIgnoreCase(email)) {
                        product.getVariants().stream().filter(variant -> variant.getSku().equalsIgnoreCase(itemSku)).findFirst().map((productVariant) -> {
                            product.getVariants().remove(productVariant);
                            if (product.getVariants().size() == 0) {
                                productRepository.delete(product);
                            } else {
                                productRepository.save(product);
                            }
                            return ResponseEntity.ok(responseMessage.setSuccessResponse("Removed variant successfully", null));
                        }).orElseGet(() -> ResponseEntity.ok(responseMessage.setFailureResponse("No variant found")));
                    }
                    return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Don't have access to remove variant"));
                }).orElseGet(() -> ResponseEntity.ok(responseMessage.setFailureResponse("No product with variant found")));
            }
            else {
                return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Access denied"));
            }
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product variant", exception));
        }
    }

    private ResponseEntity<BaseResponse<String>> updateProduct(ProductCreateRequest productCreateRequest, Product product) {
        try {
            Optional<Product> optionalVariant = getUniqueProductByVariant(productCreateRequest);
            AtomicReference<Boolean> stockUpdated = new AtomicReference<>(false);
            return optionalVariant.map((variantProduct) -> {
                variantProduct.getVariants().forEach(variant -> {
                    if (checkForStockUpdate(productCreateRequest, variant)) {
                        product.getVariants().remove(variant);
                        variant.setQuantity(variant.getQuantity() + productCreateRequest.getQuantity());
                        stockUpdated.set(true);
                        product.getVariants().add(variant);
                    }
                });
                if (stockUpdated.get()) {
                    productRepository.save(product);
                    return ResponseEntity.ok(responseMessage.setSuccessResponse("variant stock updated", null));
                }
                return ResponseEntity.ok(responseMessage.setFailureResponse("Product variant already exist"));
            }).orElseGet(() -> {
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

    private boolean checkForStockUpdate(ProductCreateRequest productCreateRequest, Variant variant) {
        return (Objects.equals(productCreateRequest.getSku(), variant.getSku()));
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

    @Override
    public ResponseEntity<BaseResponse<String>> updateStock(CartProductRequest cartProductRequest, String authId) {
        try{
            if (decryptAuthIdAndValidateRequest(authId)) {
                return getProductByDefaultSku(constructDefaultSkuFromItemSku(cartProductRequest.getSku()))
                        .map(product -> {
                          Variant variant = product.getVariants().stream().filter(prodVariant -> prodVariant.getSku().equals(cartProductRequest.getSku())).findFirst().orElseGet(() -> null);
                          if(Objects.nonNull(variant)) {
                              product.getVariants().remove(variant);
                              variant.setQuantity(variant.getQuantity() + cartProductRequest.getQuantity());
                              product.getVariants().add(variant);
                              productRepository.save(product);
                              return ResponseEntity.ok(responseMessage.setSuccessResponse("Stock updated", null));
                          }
                          else {
                              return ResponseEntity.ok(responseMessage.setFailureResponse("No variant found with given sku"));
                          }
                        })
                        .orElseGet(() ->  ResponseEntity.ok(responseMessage.setFailureResponse("No Product found to update")));
            }
            else {
                return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Access denied"));
            }
        }
        catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to update stock", exception));
        }
    }

    private String constructDefaultSkuFromItemSku(String itemSku) {
        String[] skus = itemSku.split("-");
        return skus[0]+"-"+skus[1];
    }

    private ProductQuantityCheckResponse getProductQuantity(Product product, CartProductRequest cartProductRequest) {
        AtomicReference<ProductQuantityCheckResponse> productQuantityCheckResponse = new AtomicReference<>(new ProductQuantityCheckResponse());
        product.getVariants().forEach(variant -> {
            if (!(variant.getQuantity() >= cartProductRequest.getQuantity())) {
                productQuantityCheckResponse.set(new ProductQuantityCheckResponse(false, product.getName() + " with variantSku "+ variant.getSku() +" out of stock", null));
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

    private synchronized boolean validateRequest(String auth) {
        return propertiesCollector.getAuthId().equals(auth);
    }
    private boolean decryptAuthIdAndValidateRequest(String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec("1234567812345678".getBytes(StandardCharsets.UTF_8));
            SecretKeySpec skeySpec = new SecretKeySpec("1234567812345678".getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));
            return validateRequest(new String(original));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

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
    private synchronized Optional<Variant> getVariantBySku(String itemSku) {
        return productRepository.findVariantByVariantsSku(itemSku);
    }


    private synchronized Optional<Variant> getUniqueVariant(ProductCreateRequest productCreateRequest) {
        if (Objects.nonNull(productCreateRequest.getSize()) && Objects.nonNull(productCreateRequest.getColor())) {
            return productRepository.findVariantByBrandIgnoreCaseAndNameIgnoreCaseAndVariantsColorAndVariantsSize(productCreateRequest.getBrand(),productCreateRequest.getName(), productCreateRequest.getColor(), productCreateRequest.getSize());
        } else if (Objects.nonNull(productCreateRequest.getSize())) {
            return productRepository.findVariantByBrandIgnoreCaseAndNameIgnoreCaseAndVariantsSize(productCreateRequest.getBrand(),productCreateRequest.getName(), productCreateRequest.getSize());
        } else {
            return productRepository.findVariantByBrandIgnoreCaseAndNameIgnoreCaseAndVariantsColor(productCreateRequest.getBrand(),productCreateRequest.getName(),productCreateRequest.getColor());
        }
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
