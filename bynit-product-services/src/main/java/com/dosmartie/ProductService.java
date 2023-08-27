package com.dosmartie;


import com.dosmartie.request.ProductCreateRequest;
import com.dosmartie.response.BaseResponse;
import com.dosmartie.response.ProductResponse;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

public interface ProductService {

    ResponseEntity<BaseResponse<String>> addOrUpdateProduct(ProductCreateRequest product) throws IOException;

    ResponseEntity<BaseResponse<ProductResponse>> getProduct(String productName);

//    ResponseEntity<BaseResponse<Map<String, List<Report>>>> generateReport();

    ResponseEntity<BaseResponse<String>> deleteProduct(String productName);
    ResponseEntity<BaseResponse<String>> deleteAllProduct();
    BaseResponse<List<ProductResponse>> getAllProductsFromInventory();
//    ProductQuantityCheckResponse[] checkStock(List<PurchaseRequest> productRequest);
}
