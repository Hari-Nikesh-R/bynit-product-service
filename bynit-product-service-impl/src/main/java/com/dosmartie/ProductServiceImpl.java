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
import com.dosmartie.utils.EncryptionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    //todo: Mapstruct
    private final ObjectMapper mapper;
    private final ResponseMessage<String> responseMessage;
    private final ResponseMessage<List<ProductResponse>> productResponseResponseMessage;
    private final EncryptionUtils encryptionUtils;
    private final ProductDao productDao;
    private final ProductRepository productRepository;

    public ProductServiceImpl(ObjectMapper mapper, ResponseMessage<String> responseMessage, ResponseMessage<List<ProductResponse>> productResponseResponseMessage, EncryptionUtils encryptionUtils, ProductDao productDao, ProductRepository productRepository) {
        this.mapper = mapper;
        this.responseMessage = responseMessage;
        this.productResponseResponseMessage = productResponseResponseMessage;
        this.encryptionUtils = encryptionUtils;
        this.productDao = productDao;
        this.productRepository = productRepository;
    }

    @Override
    public ResponseEntity<BaseResponse<String>> addOrUpdateProduct(ProductCreateRequest productRequest, String authId) throws IOException {
        try {
            if (encryptionUtils.decryptAuthIdAndValidateRequest(authId)) {
                Optional<Product> optionalProduct = productDao.getUniqueProduct(productRequest);
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
            if (encryptionUtils.decryptAuthIdAndValidateRequest(authId)) {
                List<Product> productList = Objects.isNull(merchant) ? productDao.getProductViaCategory(category, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "overAllRating"))) : productDao.getProductViaCategoryAndMerchant(category, merchant, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "overAllRating")));
                return ResponseEntity.ok(productResponseResponseMessage.setSuccessResponse("Fetched category Result", loadProductToProductResponse(productList)));
            } else {
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
            if (encryptionUtils.decryptAuthIdAndValidateRequest(authId)) {
                productDao.deleteAllProduct();
                return ResponseEntity.ok(responseMessage.setSuccessResponse("All product Deleted successfully", null));
            } else {
                return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Access denied"));
            }
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product", exception));
        }
    }

    @Override
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getProduct(String searchParam, int page, int size, String merchant, String authId) {
        try {
            if (encryptionUtils.decryptAuthIdAndValidateRequest(authId)) {
                Page<Product> productList = Objects.nonNull(merchant) ? productDao.getProductViaMerchantSearchQuery(searchParam, merchant, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "overAllRating"))) : productDao.getProductViaSearchQuery(searchParam, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "overAllRating")));
                return ResponseEntity.ok(productResponseResponseMessage.setSuccessResponse("Fetched Result", loadProductToProductResponse(productList)));
            } else {
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
            if (encryptionUtils.decryptAuthIdAndValidateRequest(authId)) {
                return productResponseResponseMessage.setSuccessResponse("Fetched result", Objects.nonNull(merchant) ? loadProductToProductResponse(productDao.getAllProductByMerchant(merchant, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "overAllRating")))) : loadProductToProductResponse(productDao.findAllProduct(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "overAllRating")))));
            } else {
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
            if (encryptionUtils.decryptAuthIdAndValidateRequest(authId)) {
                Optional<Product> optionalProduct = productDao.getProductByDefaultSku(defaultSku);
                return optionalProduct.map(product -> {
                    if (product.getMerchantEmail().equalsIgnoreCase(email)) {
                        productRepository.delete(product);
                        return ResponseEntity.ok(responseMessage.setSuccessResponse("Product deleted successfully", null));
                    } else {
                        return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Don't have access to delete product"));
                    }
                }).orElseGet(() -> ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product")));
            } else {
                return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Access denied"));
            }
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product variant", exception));
        }
    }


    @Override
    public ResponseEntity<BaseResponse<String>> deleteProductVariant(String itemSku, String authId, String email) {
        try {
            if (encryptionUtils.decryptAuthIdAndValidateRequest(authId)) {
                Optional<Product> optionalProduct = productDao.getProductVariantItemSku(itemSku);
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
            } else {
                return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Access denied"));
            }
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to delete product variant", exception));
        }
    }

    private ResponseEntity<BaseResponse<String>> updateProduct(ProductCreateRequest productCreateRequest, Product product) {
        try {
            Optional<Product> optionalVariant = productDao.getUniqueProductByVariant(productCreateRequest);
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
        try {
            if (encryptionUtils.decryptAuthIdAndValidateRequest(authId)) {
                return productDao.getProductByDefaultSku(constructDefaultSkuFromItemSku(cartProductRequest.getSku()))
                        .map(product -> {
                            Variant variant = product.getVariants().stream().filter(prodVariant -> prodVariant.getSku().equals(cartProductRequest.getSku())).findFirst().orElseGet(() -> null);
                            if (Objects.nonNull(variant)) {
                                product.getVariants().remove(variant);
                                variant.setQuantity(variant.getQuantity() + cartProductRequest.getQuantity());
                                product.getVariants().add(variant);
                                productRepository.save(product);
                                return ResponseEntity.ok(responseMessage.setSuccessResponse("Stock updated", null));
                            } else {
                                return ResponseEntity.ok(responseMessage.setFailureResponse("No variant found with given sku"));
                            }
                        })
                        .orElseGet(() -> ResponseEntity.ok(responseMessage.setFailureResponse("No Product found to update")));
            } else {
                return ResponseEntity.ok(responseMessage.setUnauthorizedResponse("Access denied"));
            }
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to update stock", exception));
        }
    }

    private String constructDefaultSkuFromItemSku(String itemSku) {
        String[] skus = itemSku.split("-");
        return skus[0] + "-" + skus[1];
    }

    private ProductQuantityCheckResponse getProductQuantity(Product product, CartProductRequest cartProductRequest) {
        AtomicReference<ProductQuantityCheckResponse> productQuantityCheckResponse = new AtomicReference<>(new ProductQuantityCheckResponse());
        product.getVariants().forEach(variant -> {
            if (!(variant.getQuantity() >= cartProductRequest.getQuantity())) {
                productQuantityCheckResponse.set(new ProductQuantityCheckResponse(false, product.getName() + " with variantSku " + variant.getSku() + " out of stock", null));
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
}
