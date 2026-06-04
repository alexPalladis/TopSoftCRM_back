package com.topsoft.topsoftcrm_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank @Size(min = 9, max = 9)
    private String afm;

    @NotBlank @Size(max = 200)
    private String eponymia;

    @Size(max = 200)
    private String nomimosEkprosopos;

    @NotBlank @Size(max = 100)
    private String epaggelma;

    @NotBlank @Size(max = 100)
    private String doy;

    @NotBlank @Size(max = 300)
    private String address;

    @NotBlank @Size(max = 100)
    private String city;

    @NotBlank @Size(min = 5, max = 5)
    private String tk;

    @Size(max = 20)
    private String phoneFixed;

    @NotBlank @Size(max = 20)
    private String phoneMobile;

    @NotBlank @Email @Size(max = 150)
    private String email;

    private Boolean active = true;

    @NotBlank
    private String dealerId;

    private String subDealerId;

    private String referralCode;
}
