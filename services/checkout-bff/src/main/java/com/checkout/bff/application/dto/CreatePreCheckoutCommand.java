package com.checkout.bff.application.dto;

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
public class CreatePreCheckoutCommand {

    @NotNull(message = "Products list is required")
    @NotEmpty(message = "At least one product is required")
    @Size(max = 50, message = "Maximum 50 products allowed")
    private List<ProductSelection> products;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSelection {
        @NotNull(message = "Product slug is required")
        @Size(min = 1, max = 100, message = "Slug must be between 1 and 100 characters")
        private String slug;
    }
}