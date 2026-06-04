package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.CommissionRequest;
import com.topsoft.topsoftcrm_backend.dto.response.CommissionResponse;
import com.topsoft.topsoftcrm_backend.model.Commission;
import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import com.topsoft.topsoftcrm_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommissionService {

    private final CommissionRepository commissionRepository;
    private final ProductRepository productRepository;
    private final NetworkRepository networkRepository;
    private final DealerRepository dealerRepository;
    private final SubDealerRepository subDealerRepository;

    public List<CommissionResponse> getAllByType(EntityType entityType) {
        return switch (entityType) {
            case NETWORK   -> networkRepository.findAll().stream()
                    .map(n -> buildResponse(EntityType.NETWORK, n.getId(), n.getEponymia()))
                    .toList();
            case DEALER    -> dealerRepository.findAll().stream()
                    .map(d -> buildResponse(EntityType.DEALER, d.getId(), d.getEponymia()))
                    .toList();
            case SUBDEALER -> subDealerRepository.findAll().stream()
                    .map(s -> buildResponse(EntityType.SUBDEALER, s.getId(), s.getEponymia()))
                    .toList();
            default -> List.of();
        };
    }

    public CommissionResponse getByEntity(EntityType entityType, String entityId) {
        String name = resolveEntityName(entityType, entityId);
        return buildResponse(entityType, entityId, name);
    }

    @Transactional
    public CommissionResponse save(CommissionRequest request) {
        var products = productRepository.findAll();

        for (CommissionRequest.CommissionItem item : request.getCommissions()) {
            var existing = commissionRepository
                    .findByEntityTypeAndEntityIdAndProductId(
                            request.getEntityType(), request.getEntityId(), item.getProductId());

            if (existing.isPresent()) {
                Commission c = existing.get();
                c.setPercentage(item.getPercentage());
                c.setSalePrice(item.getSalePrice());
                commissionRepository.save(c);
            } else {
                Commission c = Commission.builder()
                        .entityType(request.getEntityType())
                        .entityId(request.getEntityId())
                        .productId(item.getProductId())
                        .percentage(item.getPercentage() != null ? item.getPercentage() : BigDecimal.ZERO)
                        .salePrice(item.getSalePrice())
                        .build();
                commissionRepository.save(c);
            }
        }

        String name = resolveEntityName(request.getEntityType(), request.getEntityId());
        return buildResponse(request.getEntityType(), request.getEntityId(), name);
    }

    private CommissionResponse buildResponse(EntityType type, String entityId, String entityName) {
        var products     = productRepository.findByActiveTrueOrderBySortOrderAsc();
        var commissions  = commissionRepository.findByEntityTypeAndEntityId(type, entityId);

        var items = products.stream().map(p -> {
            var comm = commissions.stream()
                    .filter(c -> c.getProductId().equals(p.getId()))
                    .findFirst();

            return CommissionResponse.CommissionItem.builder()
                    .productId(p.getId())
                    .productDescription(p.getDescription())
                    .productPrice(p.getPrice())
                    .percentage(comm.map(Commission::getPercentage).orElse(BigDecimal.ZERO))
                    .salePrice(comm.map(Commission::getSalePrice).orElse(null))
                    .build();
        }).toList();

        return CommissionResponse.builder()
                .entityType(type)
                .entityId(entityId)
                .entityName(entityName)
                .commissions(items)
                .build();
    }

    private String resolveEntityName(EntityType type, String id) {
        return switch (type) {
            case NETWORK   -> networkRepository.findById(id).map(n -> n.getEponymia()).orElse("—");
            case DEALER    -> dealerRepository.findById(id).map(d -> d.getEponymia()).orElse("—");
            case SUBDEALER -> subDealerRepository.findById(id).map(s -> s.getEponymia()).orElse("—");
            default        -> "Admin";
        };
    }
}