package com.checkout.bff.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_payment_method")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false, insertable = false, updatable = false)
    private Long cartId;

    @Column(name = "public_cart_id", nullable = false)
    private java.util.UUID publicCartId;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_method_name", nullable = false, length = 100)
    private String paymentMethodName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", referencedColumnName = "id", nullable = false)
    private ShoppingCart shoppingCart;
}