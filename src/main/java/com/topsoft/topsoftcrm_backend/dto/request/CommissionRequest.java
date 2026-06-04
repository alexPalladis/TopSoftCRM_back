package com.topsoft.topsoftcrm_backend.dto.request;

import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CommissionRequest {

    @NotNull
    private EntityType entityType;

    @NotNull
    private String entityId;

    @NotNull
    private List<CommissionItem> commissions;

    @Data
    public static class CommissionItem {
        private Integer productId;
        private BigDecimal percentage;
        private BigDecimal salePrice;
    }
}
