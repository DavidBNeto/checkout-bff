package com.checkout.bff.application.mapper;

import com.checkout.bff.application.dto.CreateSyncCheckoutCommand;
import com.checkout.bff.application.dto.SyncCheckoutResponse;
import com.checkout.bff.domain.entity.CartItem;
import com.checkout.bff.domain.entity.ShoppingCart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SyncCheckoutMapper {

    public SyncCheckoutResponse toResponse(ShoppingCart cart, CreateSyncCheckoutCommand.PaymentMethodData paymentMethod) {
        SyncCheckoutResponse response = SyncCheckoutResponse.builder()
                .cartId(cart.getPublicId())
                .status("APPROVED")
                .paidAt(cart.getPaidAt())
                .products(mapProducts(cart.getItems()))
                .payment(buildPaymentDetails(cart, paymentMethod))
                .build();

        return response;
    }

    private List<SyncCheckoutResponse.ProductDetail> mapProducts(List<CartItem> items) {
        return items.stream()
                .map(item -> SyncCheckoutResponse.ProductDetail.builder()
                        .slug(item.getProduct().getSlug())
                        .name(item.getProduct().getName())
                        .price(item.getProduct().getPrice().getAmount())
                        .build())
                .toList();
    }

    private SyncCheckoutResponse.PaymentDetails buildPaymentDetails(ShoppingCart cart, CreateSyncCheckoutCommand.PaymentMethodData paymentMethod) {
        String type = paymentMethod.getType().toUpperCase();

        String displayName;
        if ("CREDIT_CARD".equals(type)) {
            displayName = "Cartão de Crédito";
        } else if ("DEBIT_CARD".equals(type)) {
            displayName = "Cartão de Débito";
        } else {
            displayName = "PIX";
        }

        SyncCheckoutResponse.PaymentDetails.PaymentDetailsBuilder builder = SyncCheckoutResponse.PaymentDetails.builder()
                .method(displayName)
                .amount(cart.getPrice().getAmount())
                .currency("BRL");

        if ("CREDIT_CARD".equals(type) || "DEBIT_CARD".equals(type)) {
            String lastDigits = getLastDigits(paymentMethod.getCard().getNumber());
            builder.lastDigits(lastDigits);
        } else if ("PIX".equals(type)) {
            String pixCode = generatePixCode(cart, paymentMethod.getPixPayer() != null ? paymentMethod.getPixPayer().getCpf() : "");
            builder.pixCode(pixCode);
        }

        return builder.build();
    }

    private String getLastDigits(String cardNumber) {
        String clean = cardNumber.replaceAll("\\s", "");
        if (clean.length() >= 4) {
            return clean.substring(clean.length() - 4);
        }
        return clean;
    }

    private String generatePixCode(ShoppingCart cart, String pixKey) {
        // Generate a simulated BR Code (PIX copy-paste code)
        // Format: 00020126580014br.gov.bcb.pix... (simplified)
        String amount = cart.getPrice().getAmount().toPlainString();
        // Simulated PIX code - in real implementation this would follow BR Code specification
        return String.format("00020126580014br.gov.bcb.pix0136%s5204000053039865802BR5925CHECKOUT BFF6009SAO PAULO62070503***6304%s",
                pixKey.length() > 36 ? pixKey.substring(0, 36) : pixKey,
                generateCrc16("00020126580014br.gov.bcb.pix0136" + pixKey + "5204000053039865802BR5925CHECKOUT BFF6009SAO PAULO62070503***"));
    }

    private String generateCrc16(String payload) {
        // Simplified CRC16 calculation for PIX BR Code
        int crc = 0xFFFF;
        for (byte b : payload.getBytes()) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
            }
        }
        return String.format("%04X", crc & 0xFFFF);
    }
}