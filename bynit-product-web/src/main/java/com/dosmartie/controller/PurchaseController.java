package com.dosmartie.controller;

import com.dosmartie.PurchaseService;
import com.dosmartie.helper.Urls;
import com.dosmartie.request.CartProductRequest;
import com.dosmartie.response.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = Urls.PURCHASE)
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PutMapping(value = Urls.DEDUCT_STOCK)
    public BaseResponse<?> deductStockAfterPurchase(@RequestBody List<CartProductRequest> productRequest) {
        return purchaseService.deductStock(productRequest);
    }
}
