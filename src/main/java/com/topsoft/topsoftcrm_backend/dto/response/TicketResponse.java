package com.topsoft.topsoftcrm_backend.dto.response;

import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import com.topsoft.topsoftcrm_backend.model.enums.TicketStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class TicketResponse {
    private Integer id;
    private EntityType fromType;
    private String fromId;
    private String fromName;
    private EntityType toType;
    private String toId;
    private String toName;
    private String subject;
    private String body;
    private TicketStatus status;
    private LocalDateTime createdAt;
}
