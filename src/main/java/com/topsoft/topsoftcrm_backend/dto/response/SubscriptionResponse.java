package com.topsoft.topsoftcrm_backend.dto.response;

import com.topsoft.topsoftcrm_backend.model.enums.ProductType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class SubscriptionResponse {
    private Integer id;
    private Integer productId;
    private String productDescription;
    private ProductType productType;
    private Boolean active;
    private LocalDate expiryDate;
    private Integer quantity;
    private BigDecimal cost;
    private LocalDateTime activatedAt;
}
