package com.dosmartie;


import com.dosmartie.request.CartProductRequest;
import com.dosmartie.request.ProductCreateRequest;
import com.dosmartie.response.BaseResponse;
import com.dosmartie.response.ProductQuantityCheckResponse;
import com.dosmartie.response.ProductResponse;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

public interface ProductService {

    ResponseEntity<BaseResponse<String>> addOrUpdateProduct(ProductCreateRequest product) throws IOException;

    ResponseEntity<BaseResponse<List<ProductResponse>>> getAllProductViaCategory(String category, int page, int size, String merchant);

    ResponseEntity<BaseResponse<List<ProductResponse>>> getProduct(String searchParam, int page, int size, String merchant, String authId);

//    ResponseEntity<BaseResponse<Map<String, List<Report>>>> generateReport();

    ResponseEntity<BaseResponse<String>> deleteProductVariant(String itemSku);

    ResponseEntity<BaseResponse<String>> deleteProduct(String defaultSku);

    ResponseEntity<BaseResponse<String>> deleteAllProduct();

    BaseResponse<List<ProductResponse>> getAllProductsFromInventory(int page, int size, String isMerchant, String authId);

    ProductQuantityCheckResponse[] checkStock(List<CartProductRequest> productRequest);
}
