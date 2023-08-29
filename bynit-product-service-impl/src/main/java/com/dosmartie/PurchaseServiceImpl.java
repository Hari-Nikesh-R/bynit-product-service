package com.dosmartie;

import com.dosmartie.document.Product;
import com.dosmartie.document.mongohelper.Variant;
import com.dosmartie.helper.ResponseMessage;
import com.dosmartie.request.CartProductRequest;
import com.dosmartie.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PurchaseServiceImpl implements PurchaseService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductService productService;
    @Autowired
    private ResponseMessage<Object> responseMessage;

    @Override
    public BaseResponse<?> deductStock(List<CartProductRequest> productRequest) {
        try {
            List<String> quantityDeductionMessage = new ArrayList<>();
            for (CartProductRequest product : productRequest) {
                Optional<Product> optionalProduct = productRepository.findByVariantsSku(product.getSku());
                Optional<Variant> optionalVariant = productRepository.findVariantByVariantsSku(product.getSku());
                if (optionalVariant.isPresent() && optionalProduct.isPresent()) {
                    Variant variant = optionalVariant.get();
                    if (variant.getQuantity() >= product.getQuantity()) {
                        optionalProduct.get().getVariants().remove(variant);
                        variant.setQuantity(variant.getQuantity() - product.getQuantity());
                        optionalProduct.get().getVariants().add(variant);
                        productRepository.save(optionalProduct.get());
                        quantityDeductionMessage.add(optionalProduct.get().getName() + " - Quantity deducted");
                    } else {
                        quantityDeductionMessage.add(optionalProduct.get().getName() + " cannot be deducted, Trying to purchase more than stock");
                        log.warn("Out of stock");
                    }
                }
            }
            return responseMessage.setSuccessResponse("Quantity deducted", quantityDeductionMessage);
        } catch (Exception exception) {
            exception.printStackTrace();
            return responseMessage.setFailureResponse("Unable to deduct quantity", exception);
        }
    }
}
