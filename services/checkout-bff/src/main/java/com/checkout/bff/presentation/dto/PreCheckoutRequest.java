package com.checkout.bff.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreCheckoutRequest {

    @NotNull(message = "Products list is required")
    @NotEmpty(message = "At least one product is required")
    @Size(max = 50, message = "Maximum 50 products allowed")
    @Valid
    private List<ProductSelection> products;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSelection {
        @NotNull(message = "Product slug is required")
        @Size(min = 1, max = 100, message = "Slug must be between 1 and 100 characters")
        @jakarta.validation.constraints.Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
        private String slug;
    }
}