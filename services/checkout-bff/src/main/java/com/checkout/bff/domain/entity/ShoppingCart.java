package com.checkout.bff.domain.entity;

import com.checkout.bff.domain.valueobject.Money;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "shopping_cart")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false, precision = 10, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money price;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "shoppingCart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CartItem> items = new LinkedHashSet<>();

    @OneToMany(mappedBy = "shoppingCart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartPaymentMethod> availablePaymentMethods = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void addItem(CartItem item) {
        items.add(item);
        item.setShoppingCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setShoppingCart(null);
    }

    public List<CartItem> getItems() {
        return new ArrayList<>(items);
    }

    public void addAvailablePaymentMethod(CartPaymentMethod cartPaymentMethod) {
        availablePaymentMethods.add(cartPaymentMethod);
        cartPaymentMethod.setShoppingCart(this);
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public boolean isExpired() {
        return "EXPIRED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }
}