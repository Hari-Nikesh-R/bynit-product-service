package com.dosmartie;

import com.dosmartie.request.CartProductRequest;
import com.dosmartie.response.BaseResponse;

import java.util.List;

public interface PurchaseService {
    BaseResponse<?> deductStock(List<CartProductRequest> productRequest);

}
