package com.dosmartie;

import com.dosmartie.request.CartProductRequest;
import com.dosmartie.request.RateRequest;
import com.dosmartie.response.CartProductResponse;
import com.dosmartie.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(value = "orderFeign", url = "http://localhost:8045/order")
public interface OrderFeign {
    @PutMapping(value = "/unrated")
    public Map<String, CartProductResponse> getUnratedProducts(@RequestParam("orderId") String orderId, @RequestBody List<RateRequest> requests, @RequestHeader("email") String email);
}
