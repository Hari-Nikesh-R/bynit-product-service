package com.dosmartie.helper;

import org.springframework.stereotype.Component;

import java.util.Random;

public class DefaultSkuGenerator {
    private static final int MAX_RANDOM_SUFFIX = 9999; // Maximum random number for suffix
    private static final int RANDOM_SUFFIX_DIGITS = 4; // Number of digits in the random suffix

    public static String generateDefaultSku(String productName) {
        String sanitizedProductName = sanitizeProductName(productName);
        String randomSuffix = generateRandomSuffix();
        return sanitizedProductName + "-" + randomSuffix;
    }

    private static String sanitizeProductName(String productName) {
        return productName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private static String generateRandomSuffix() {
        Random random = new Random();
        int randomSuffix = random.nextInt(MAX_RANDOM_SUFFIX + 1);
        return String.format("%0" + RANDOM_SUFFIX_DIGITS + "d", randomSuffix);
    }
}
