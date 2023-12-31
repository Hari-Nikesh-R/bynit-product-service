package com.dosmartie;

import com.dosmartie.request.CartProductRequest;
import com.dosmartie.request.OrderRatingRequest;
import com.dosmartie.request.RateRequest;
import com.dosmartie.response.BaseResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PurchaseService {
    BaseResponse<?> deductStock(List<CartProductRequest> productRequest);
    ResponseEntity<BaseResponse<?>> rateProduct(OrderRatingRequest orderRatingRequest, String authId);
    ResponseEntity<BaseResponse<?>> getUnratedProduct(String orderId, String authId);

}
