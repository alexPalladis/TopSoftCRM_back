package com.topsoft.topsoftcrm_backend.dto.request;

import lombok.Data;

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
}
