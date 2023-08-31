package com.dosmartie.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRatingRequest {
    @NotNull(message = "orderId must not be null")
    private String orderId;
    private @Valid List<RateRequest> rateRequest;
}
