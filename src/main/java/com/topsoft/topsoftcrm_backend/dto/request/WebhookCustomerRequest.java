package com.topsoft.topsoftcrm_backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WebhookCustomerRequest {
    private String afm;
    private String eponymia;
    private String nomimosEkprosopos;
    private String epaggelma;
    private String doy;
    private String address;
    private String city;
    private String tk;
    private String phoneFixed;
    private String phoneMobile;
    private String email;
    private String referralCode;
    private Integer productId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String externalRef;
}
