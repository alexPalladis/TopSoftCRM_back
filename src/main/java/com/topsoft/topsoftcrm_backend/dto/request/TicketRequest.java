package com.topsoft.topsoftcrm_backend.dto.request;

import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketRequest {

    @NotNull
    private EntityType fromType;

    @NotBlank
    private String fromId;

    @NotNull
    private EntityType toType;

    @NotBlank
    private String toId;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;
}
