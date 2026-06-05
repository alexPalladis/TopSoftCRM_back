package com.topsoft.topsoftcrm_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class CommissionHistoryResponse {
    private Long   id;
    private LocalDate paymentDate;
    private String productDescription;
    private String customerEponymia;
    private String customerAfm;
    private BigDecimal amount;
    private String dealerId;
    private String dealerName;
    private BigDecimal dealerCommissionPct;
    private BigDecimal dealerCommissionAmount;
    private Boolean paidDealer;
    private String networkId;
    private String networkName;
    private BigDecimal networkCommissionPct;
    private BigDecimal networkCommissionAmount;
    private Boolean paidNetwork;
    private String receipt;
    private String externalRef;
}