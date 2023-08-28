package com.dosmartie.controller;


import com.dosmartie.ProductService;
import com.dosmartie.helper.Urls;
import com.dosmartie.request.ProductCreateRequest;
import com.dosmartie.response.BaseResponse;
import com.dosmartie.response.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<BaseResponse<String>> addProduct(@RequestBody @Valid ProductCreateRequest product) throws IOException {
        return productService.addOrUpdateProduct(product);
    }

    @GetMapping(value = "/{category}")
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getProductViaCategory(@PathVariable("category") String category,@RequestParam(PAGE) int page, @RequestParam(SIZE) int size, @RequestParam(value = MERCHANT, required = false) String merchant) {
        return productService.getAllProductViaCategory(category, page, size, merchant);
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getProduct(@RequestParam(SEARCH_PARAM) String searchParam, @RequestParam(PAGE) int page, @RequestParam(SIZE) int size, @RequestParam(value = MERCHANT, required = false) String merchant, @RequestHeader(AUTH_ID) String authId) {
        return productService.getProduct(searchParam, page, size, merchant, authId);
    }

    @DeleteMapping(value = "/variant")
    public ResponseEntity<BaseResponse<String>> deleteProductVariant(@RequestParam(VARIANT_SKU) String itemSku) {
        return productService.deleteProductVariant(itemSku);
    }

    @DeleteMapping
    public ResponseEntity<BaseResponse<String>> deleteProduct(@RequestParam(DEFAULT_SKU) String itemSku) {
        return productService.deleteProduct(itemSku);
    }

    @DeleteMapping(value = Urls.ALL_PRODUCT)
    public ResponseEntity<BaseResponse<String>> deleteAllProduct() {
        return productService.deleteAllProduct();
    }

    @GetMapping(value = Urls.ALL_PRODUCT)
    public BaseResponse<List<ProductResponse>> getAllProducts(@RequestParam(PAGE) int page, @RequestParam(SIZE) int size, @RequestParam(value = MERCHANT, required = false) String merchant, @RequestHeader(AUTH_ID) String authId) {
        return productService.getAllProductsFromInventory(page, size, merchant, authId);
    }

//    @PostMapping(value = Urls.QUANTITY)
//    public ProductQuantityCheckResponse[] quantityCheck(@RequestBody List<PurchaseRequest> productRequest) {
//        return productService.checkStock(productRequest);
//    }

}
