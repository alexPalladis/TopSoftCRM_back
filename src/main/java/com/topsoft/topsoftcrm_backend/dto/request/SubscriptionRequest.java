package com.topsoft.topsoftcrm_backend.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SubscriptionRequest {

    @NotNull
    private Integer productId;

    @NotNull
    private Boolean active;

    private LocalDate expiryDate;

    private Integer quantity;

    private BigDecimal costOverride;
}