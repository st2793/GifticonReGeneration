package com.gifticon.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class Gifticon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String productName;    // title -> productName
    private String detailMessage;  // 상세 메시지 필드 추가
    private String originalImagePath;
    private String processedImagePath;
    private String qrCodePath;
    private String barcodePath;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private String shareCode;
    private String barcodeNumber; // 바코드 숫자 추가
    @Column
    private String productUrl;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }
} 