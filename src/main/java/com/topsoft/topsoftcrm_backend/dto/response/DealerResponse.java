package com.topsoft.topsoftcrm_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class DealerResponse {
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
    private String username;
    private Boolean active;
    private String networkId;
    private String networkName;
    private Long totalSubDealers;
    private Long totalCustomers;
    private LocalDateTime createdAt;
}