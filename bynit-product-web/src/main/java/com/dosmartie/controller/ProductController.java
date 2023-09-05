package com.dosmartie.controller;


import com.dosmartie.ProductService;
import com.dosmartie.helper.Urls;
import com.dosmartie.request.CartProductRequest;
import com.dosmartie.request.ProductCreateRequest;
import com.dosmartie.response.BaseResponse;
import com.dosmartie.response.ProductQuantityCheckResponse;
import com.dosmartie.response.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework
        .http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.dosmartie.helper.Constants.*;


@RestController
@RequestMapping(value = Urls.PRODUCT)
public class ProductController {

    @Autowired
    private ProductService productService;

//    @GetMapping(value = Urls.GENERATE_REPORT)
//    public ResponseEntity<BaseResponse<Map<String, List<Report>>>> generateReport() {
//        return productService.generateReport();
//    }

    @PostMapping
    public ResponseEntity<BaseResponse<String>> addProduct(@RequestBody @Valid ProductCreateRequest product, @RequestHeader(value = "merchant", required = false) String email) throws IOException {
        return productService.addOrUpdateProduct(product, email);
    }

    @GetMapping(value = "/{category}")
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getProductViaCategory(@PathVariable("category") String category, @RequestParam(PAGE) int page, @RequestParam(SIZE) int size, @RequestHeader(value = MERCHANT, required = false) String merchant ) {
        return productService.getAllProductViaCategory(category, page, size, merchant);
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getProduct(@RequestParam(SEARCH_PARAM) String searchParam, @RequestParam(PAGE) int page, @RequestParam(SIZE) int size, @RequestHeader(value = MERCHANT, required = false) String merchant ) {
        return productService.getProduct(searchParam, page, size, merchant);
    }

    @DeleteMapping(value = "/variant")
    public ResponseEntity<BaseResponse<String>> deleteProductVariant(@RequestParam(VARIANT_SKU) String itemSku,  @RequestHeader(MERCHANT) String email) {
        return productService.deleteProductVariant(itemSku, email);
    }

    @DeleteMapping
    public ResponseEntity<BaseResponse<String>> deleteProduct(@RequestParam(DEFAULT_SKU) String itemSku,  @RequestHeader(MERCHANT) String email) {
        return productService.deleteProduct(itemSku, email);
    }

    @DeleteMapping(value = Urls.ALL_PRODUCT)
    public ResponseEntity<BaseResponse<String>> deleteAllProduct(@RequestHeader(MERCHANT) String email) {
        return productService.deleteAllProduct( email);
    }

    @GetMapping(value = Urls.ALL_PRODUCT)
    public BaseResponse<List<ProductResponse>> getAllProducts(@RequestParam(PAGE) int page, @RequestParam(SIZE) int size, @RequestHeader(value = MERCHANT, required = false) String merchant) {
        return productService.getAllProductsFromInventory(page, size, merchant);
    }

    @PostMapping(value = Urls.QUANTITY)
    public ProductQuantityCheckResponse[] quantityCheck(@RequestBody List<CartProductRequest> productRequest) {
        return productService.checkStock(productRequest);
    }
    @PostMapping(value = Urls.STOCK)
    public ResponseEntity<BaseResponse<String>> updateStock(@RequestBody CartProductRequest cartProductRequest ) {
        return productService.updateStock(cartProductRequest);
    }

}
