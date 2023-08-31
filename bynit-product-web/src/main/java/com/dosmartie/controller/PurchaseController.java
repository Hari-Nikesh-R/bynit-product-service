package com.dosmartie.controller;

import com.dosmartie.PurchaseService;
import com.dosmartie.helper.Urls;
import com.dosmartie.request.CartProductRequest;
import com.dosmartie.request.OrderRatingRequest;
import com.dosmartie.request.RateRequest;
import com.dosmartie.response.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.dosmartie.helper.Constants.AUTH_ID;

@RestController
@RequestMapping(value = Urls.PURCHASE)
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PutMapping(value = Urls.DEDUCT_STOCK)
    public BaseResponse<?> deductStockAfterPurchase(@RequestBody List<CartProductRequest> productRequest) {
        return purchaseService.deductStock(productRequest);
    }

    @PutMapping(value = Urls.RATE)
    public ResponseEntity<BaseResponse<?>> rateProduct(@RequestBody @Valid OrderRatingRequest orderRatingRequest, @RequestHeader(AUTH_ID) String authId) {
        return purchaseService.rateProduct(orderRatingRequest, authId);
    }

    @GetMapping(value = Urls.RATE)
    public ResponseEntity<BaseResponse<?>> fetchUnratedProductsFromAnOrder(@RequestHeader(AUTH_ID) String authId, @RequestParam("orderId") String orderId) {
        return purchaseService.getUnratedProduct(orderId, authId);
    }
}
