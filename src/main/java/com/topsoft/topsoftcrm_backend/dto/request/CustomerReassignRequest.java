package com.topsoft.topsoftcrm_backend.dto.request;

import lombok.Data;

/**
 * Payload for PATCH /api/customers/{id}/reassign
 *
 * subDealerId may be null — passing null removes the subdealer link (admin only).
 */
@Data
public class CustomerReassignRequest {
    // nullable — passing null clears the subdealer (admin only)
    private String subDealerId;
}