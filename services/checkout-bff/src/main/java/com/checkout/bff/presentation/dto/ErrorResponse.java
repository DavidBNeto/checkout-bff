package com.checkout.bff.presentation.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String displayType;
    private String errorCode;
    private String cartId;
    private FrontendError frontendError;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrontendError {
        private String code;
        private String title;
        private String description;
        private Button primaryButton;
        private Button secondaryButton;

        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Button {
            private String label;
            private String url;
        }
    }
}