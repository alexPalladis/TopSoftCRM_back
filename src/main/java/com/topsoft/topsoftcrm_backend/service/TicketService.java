package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.TicketRequest;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.dto.response.TicketResponse;
import com.topsoft.topsoftcrm_backend.exception.ResourceNotFoundException;
import com.topsoft.topsoftcrm_backend.model.Ticket;
import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import com.topsoft.topsoftcrm_backend.model.enums.TicketStatus;
import com.topsoft.topsoftcrm_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final NetworkRepository networkRepository;
    private final DealerRepository dealerRepository;
    private final SubDealerRepository subDealerRepository;
    private final AdminUserRepository adminUserRepository;

    public PageResponse<TicketResponse> getAll(String entityId, int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        Page<Ticket> result = ticketRepository.findByEntity(entityId, pageable);

        return PageResponse.<TicketResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public TicketResponse getById(Integer id) {
        return toResponse(ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Αίτημα δεν βρέθηκε: " + id)));
    }

    @Transactional
    public TicketResponse create(TicketRequest request) {
        Ticket ticket = Ticket.builder()
                .fromType(request.getFromType())
                .fromId(request.getFromId())
                .toType(request.getToType())
                .toId(request.getToId())
                .subject(request.getSubject())
                .body(request.getBody())
                .status(TicketStatus.PENDING)
                .build();

        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse complete(Integer id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Αίτημα δεν βρέθηκε: " + id));
        ticket.setStatus(TicketStatus.DONE);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public void delete(Integer id) {
        ticketRepository.deleteById(id);
    }

    public long countPending(String entityId) {
        return ticketRepository.countPendingByEntityId(entityId);
    }

    private String resolveEntityName(EntityType type, String id) {
        return switch (type) {
            case ADMIN     -> adminUserRepository.findById(id)
                    .map(a -> a.getUsername()).orElse("Admin");
            case NETWORK   -> networkRepository.findById(id)
                    .map(n -> n.getEponymia()).orElse("Network");
            case DEALER    -> dealerRepository.findById(id)
                    .map(d -> d.getEponymia()).orElse("Dealer");
            case SUBDEALER -> subDealerRepository.findById(id)
                    .map(s -> s.getEponymia()).orElse("SubDealer");
        };
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .fromType(t.getFromType())
                .fromId(t.getFromId())
                .fromName(resolveEntityName(t.getFromType(), t.getFromId()))
                .toType(t.getToType())
                .toId(t.getToId())
                .toName(resolveEntityName(t.getToType(), t.getToId()))
                .subject(t.getSubject())
                .body(t.getBody())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
