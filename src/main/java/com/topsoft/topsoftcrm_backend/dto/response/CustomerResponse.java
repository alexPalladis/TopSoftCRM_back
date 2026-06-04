package com.topsoft.topsoftcrm_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class CustomerResponse {
    private String id;
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
    private Boolean active;
    private String dealerId;
    private String dealerName;
    private String subDealerId;
    private String subDealerName;
    private String networkId;
    private String networkName;
    private String source;
    private String referralCode;
    private LocalDateTime createdAt;
}