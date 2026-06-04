package com.topsoft.topsoftcrm_backend.dto.response;

import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class CommissionResponse {
    private EntityType entityType;
    private String entityId;
    private String entityName;
    private List<CommissionItem> commissions;

    @Data @Builder
    public static class CommissionItem {
        private Integer productId;
        private String productDescription;
        private BigDecimal productPrice;
        private BigDecimal percentage;
        private BigDecimal salePrice;
    }
}
