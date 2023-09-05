package com.dosmartie;

import com.dosmartie.document.Product;
import com.dosmartie.document.mongohelper.Variant;
import com.dosmartie.helper.ResponseMessage;
import com.dosmartie.request.CartProductRequest;
import com.dosmartie.request.OrderRatingRequest;
import com.dosmartie.request.RateRequest;
import com.dosmartie.response.BaseResponse;
import com.dosmartie.response.CartProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class PurchaseServiceImpl implements PurchaseService {

    private final ProductService productService;

    private final ResponseMessage<Object> responseMessage;
    private final ProductDao productDao;
    private final OrderFeign orderFeign;

    @Autowired
    public PurchaseServiceImpl(ProductService productService, ResponseMessage<Object> responseMessage, ProductDao productDao, OrderFeign orderFeign) {
        this.productService = productService;
        this.responseMessage = responseMessage;
        this.productDao = productDao;
        this.orderFeign = orderFeign;
    }

    @Override
    public synchronized BaseResponse<?> deductStock(List<CartProductRequest> productRequest) {
        try {
            List<String> quantityDeductionMessage = new ArrayList<>();
            for (CartProductRequest product : productRequest) {
                Optional<Product> optionalProduct = productDao.getProductByVariantSku(product.getSku());
                if (optionalProduct.isPresent()) {
                    Product prod = optionalProduct.get();
                    Variant variant = prod.getVariants().stream().filter(prodVariant -> prodVariant.getSku().equals(product.getSku())).findFirst().orElseGet(() -> null);
                    if (Objects.nonNull(variant)) {
                        if (variant.getQuantity() >= product.getQuantity()) {
                            optionalProduct.get().getVariants().remove(variant);
                            variant.setQuantity(variant.getQuantity() - product.getQuantity());
                            optionalProduct.get().getVariants().add(variant);
                            productDao.saveProduct(optionalProduct.get());
                            quantityDeductionMessage.add(optionalProduct.get().getName() + " - Quantity deducted");
                        } else {
                            quantityDeductionMessage.add(optionalProduct.get().getName() + " cannot be deducted, Trying to purchase more than stock");
                            log.warn("Out of stock");
                        }
                    } else {
                        return responseMessage.setFailureResponse("No Variant present Unable to deduct");
                    }
                }
            }
            return responseMessage.setSuccessResponse("Quantity deducted", quantityDeductionMessage);
        } catch (Exception exception) {
            exception.printStackTrace();
            return responseMessage.setFailureResponse("Unable to deduct quantity", exception);
        }
    }

    @Override
    public ResponseEntity<BaseResponse<?>> rateProduct(OrderRatingRequest orderRatingRequest, String email) {
        try {
                Map<String, CartProductResponse> cartProductResponses = getUnratedProduct(orderRatingRequest.getOrderId(), orderRatingRequest.getRateRequest(), email);
                if (validateRatedVsUnratedProduct(orderRatingRequest.getRateRequest(), cartProductResponses)) {
                    orderRatingRequest.getRateRequest().forEach(rateRequest -> {
                        if (Objects.nonNull(cartProductResponses.get(rateRequest.getItemSku()))) {
                            rateProducts(rateRequest, cartProductResponses.get(rateRequest.getItemSku()));
                        }
                    });
                    return ResponseEntity.ok(responseMessage.setSuccessResponse("Rated Products", null));
                }
                return ResponseEntity.ok(responseMessage.setFailureResponse("All Products already Rated"));

        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to rate product", exception));
        }
    }

    @Override
    public ResponseEntity<BaseResponse<?>> getUnratedProduct(String orderId, String email) {
        try {
                return ResponseEntity.ok(responseMessage.setSuccessResponse("Fetched result", getUnratedProduct(orderId, new ArrayList<>(), email)));
        } catch (Exception exception) {
            return ResponseEntity.ok(responseMessage.setFailureResponse("Unable to fetch result", exception));
        }

    }

    private boolean validateRatedVsUnratedProduct(List<RateRequest> requests, Map<String, CartProductResponse> unratedProducts) {
        for (RateRequest request : requests) {
           return unratedProducts.get(request.getItemSku()) != null && unratedProducts.get(request.getItemSku()).isRated();
        }
        return false;
    }

    private void rateProducts(RateRequest rateRequest, CartProductResponse cartProductResponse) {
        Optional<Product> optionalProduct = productDao.getProductByVariantSku(rateRequest.getItemSku());
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            Variant prodVariant = product.getVariants().stream()
                    .filter(variant -> variant.getSku().equals(rateRequest.getItemSku()))
                    .findFirst().orElseGet(() -> null);
            if (Objects.nonNull(prodVariant)) {
                addRatingAndReview(product, prodVariant, rateRequest, cartProductResponse);
            }
        }
    }

    private void addRatingAndReview(Product product, Variant prodVariant, RateRequest rateRequest, CartProductResponse cartProductResponse) {
        product.getVariants().remove(prodVariant);
        prodVariant.setRating(getAverageRating(prodVariant.getRating(), rateRequest.getRate(), prodVariant.getTotalNumberOfRatingProvided() + 1));
        prodVariant.setTotalNumberOfRatingProvided(prodVariant.getTotalNumberOfRatingProvided() + 1);
        if (Objects.isNull(prodVariant.getReviews())) {
            prodVariant.setReviews(new ArrayList<>());
        }
        prodVariant.getReviews().add(cartProductResponse.getReviews());
        product.getVariants().add(prodVariant);
        product.setOverAllRating(updateOverAllRating(product));
        productDao.saveProduct(product);
    }

    private double updateOverAllRating(Product product) {
        try {
            double totalRating = product.getVariants().stream().map(Variant::getRating).mapToDouble(Double::doubleValue).sum();
            return totalRating / product.getVariants().size();
        } catch (Exception exception) {
            exception.printStackTrace();
            return 0.0;
        }
    }

    private double getAverageRating(double oldRating, double newRating, int orderPlaced) {
        return (oldRating + newRating) / orderPlaced;
    }

    private Map<String, CartProductResponse> getUnratedProduct(String orderId, List<RateRequest> requests, String email) {
        return orderFeign.getUnratedProducts(orderId, requests, email);
    }
}
