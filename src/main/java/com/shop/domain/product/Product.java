package com.shop.domain.product;

import com.shop.dto.request.ProductCreate;
import com.shop.exception.NotEnoughStock;
import com.shop.global.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    private String name;

    private int price;

    private int stockQuantity;

    private String description;

    @Enumerated(EnumType.STRING)
    private ProductSellStatus sellStatus;

    @Builder
    public Product(String name, int price, int stockQuantity, String description, ProductSellStatus sellStatus) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.description = description;
        this.sellStatus = sellStatus;
    }

    public Product(ProductCreate request) {
        this.name = request.getName();
        this.price = request.getPrice();
        this.stockQuantity = request.getStockQuantity();
        this.description = request.getDescription();
        this.sellStatus = ProductSellStatus.SELL;
    }

    public void addStock(int quantity) {
        this.stockQuantity += quantity;
    }

    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new NotEnoughStock();
        }
        this.stockQuantity = restStock;
    }
}
