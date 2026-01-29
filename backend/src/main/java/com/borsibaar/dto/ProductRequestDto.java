package com.borsibaar.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequestDto(
                @NotBlank(message = "Product name is required") @Size(max = 120, message = "Product name must not exceed 120 characters") String name,

                @Size(max = 1000, message = "Description must not exceed 1000 characters") String description,

                @NotNull(message = "Current price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0") BigDecimal currentPrice,

                @NotNull(message = "Min price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Min price must be greater than 0") BigDecimal minPrice,

                @NotNull(message = "Max price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Max price must be greater than 0") BigDecimal maxPrice,

                @NotNull(message = "Category ID is required") Long categoryId) {
}
public record ProductRequestDto(
        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal currentPrice,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal minPrice,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal maxPrice,

        @NotNull
        Long categoryId
) {

    @AssertTrue(message = "Current price must be between min price and max price")
    public boolean isPriceRangeValid() {
        if (currentPrice == null || minPrice == null || maxPrice == null) {
            return true; // handled by @NotNull
        }

        return minPrice.compareTo(maxPrice) <= 0
                && currentPrice.compareTo(minPrice) >= 0
                && currentPrice.compareTo(maxPrice) <= 0;
    }
}
