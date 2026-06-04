package com.topsoft.topsoftcrm_backend.dto.response;

import com.topsoft.topsoftcrm_backend.model.enums.ProductType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class ProductResponse {
    private Integer id;
    private String description;
    private BigDecimal price;
    private ProductType type;
    private Boolean active;
    private Integer sortOrder;
}
