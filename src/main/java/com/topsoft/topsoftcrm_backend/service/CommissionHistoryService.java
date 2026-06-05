package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.response.CommissionHistoryResponse;
import com.topsoft.topsoftcrm_backend.dto.response.PageResponse;
import com.topsoft.topsoftcrm_backend.model.*;
import com.topsoft.topsoftcrm_backend.model.enums.EntityType;
import com.topsoft.topsoftcrm_backend.repository.CommissionHistoryRepository;
import com.topsoft.topsoftcrm_backend.repository.CommissionRepository;
import com.topsoft.topsoftcrm_backend.repository.CustomerRepository;
import com.topsoft.topsoftcrm_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommissionHistoryService {

    private final CommissionHistoryRepository historyRepository;
    private final CommissionRepository commissionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public PageResponse<CommissionHistoryResponse> getAll(
            LocalDate dateFrom, LocalDate dateTo,
            Integer productId, String networkId, String dealerId,
            int page, int size) {

        var pageable = PageRequest.of(page, size, Sort.by("paymentDate").descending());
        var result   = historyRepository.findWithFilters(dateFrom, dateTo, productId, networkId, dealerId, pageable);

        return PageResponse.<CommissionHistoryResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    // Καλείται από WebhookService όταν πληρώσει πελάτης
    @Transactional
    public CommissionHistory createFromPayment(
            String customerAfm, Integer productId,
            BigDecimal amount, LocalDate paymentDate, String externalRef) {

        Customer customer = customerRepository.findByAfm(customerAfm)
                .orElseThrow(() -> new RuntimeException("Πελάτης δεν βρέθηκε: " + customerAfm));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Προϊόν δεν βρέθηκε: " + productId));

        Dealer dealer  = customer.getDealer();
        Network network = dealer.getNetwork();

        // Βρες προμήθεια dealer για αυτό το προϊόν
        BigDecimal dealerPct    = findCommissionPct(EntityType.DEALER, dealer.getId(), productId);
        BigDecimal dealerAmount = amount.multiply(dealerPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Βρες προμήθεια network (αν υπάρχει)
        BigDecimal networkPct    = BigDecimal.ZERO;
        BigDecimal networkAmount = BigDecimal.ZERO;
        if (network != null) {
            networkPct    = findCommissionPct(EntityType.NETWORK, network.getId(), productId);
            networkAmount = amount.multiply(networkPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        CommissionHistory history = CommissionHistory.builder()
                .paymentDate(paymentDate != null ? paymentDate : LocalDate.now())
                .product(product)
                .customer(customer)
                .amount(amount)
                .dealer(dealer)
                .dealerCommissionPct(dealerPct)
                .dealerCommissionAmount(dealerAmount)
                .paidDealer(false)
                .network(network)
                .networkCommissionPct(network != null ? networkPct : null)
                .networkCommissionAmount(network != null ? networkAmount : null)
                .paidNetwork(false)
                .externalRef(externalRef)
                .build();

        return historyRepository.save(history);
    }

    @Transactional
    public CommissionHistoryResponse togglePaidDealer(Long id) {
        var h = historyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Εγγραφή δεν βρέθηκε"));
        h.setPaidDealer(!h.getPaidDealer());
        return toResponse(historyRepository.save(h));
    }

    @Transactional
    public CommissionHistoryResponse togglePaidNetwork(Long id) {
        var h = historyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Εγγραφή δεν βρέθηκε"));
        h.setPaidNetwork(!h.getPaidNetwork());
        return toResponse(historyRepository.save(h));
    }

    @Transactional
    public CommissionHistoryResponse updateReceipt(Long id, String receipt) {
        var h = historyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Εγγραφή δεν βρέθηκε"));
        h.setReceipt(receipt);
        return toResponse(historyRepository.save(h));
    }

    @Transactional
    public void delete(Long id) {
        historyRepository.deleteById(id);
    }

    private BigDecimal findCommissionPct(EntityType type, String entityId, Integer productId) {
        return commissionRepository
                .findByEntityTypeAndEntityIdAndProductId(type, entityId, productId)
                .map(Commission::getPercentage)
                .orElse(BigDecimal.ZERO);
    }

    private CommissionHistoryResponse toResponse(CommissionHistory h) {
        return CommissionHistoryResponse.builder()
                .id(h.getId())
                .paymentDate(h.getPaymentDate())
                .productDescription(h.getProduct().getDescription())
                .customerEponymia(h.getCustomer().getEponymia())
                .customerAfm(h.getCustomer().getAfm())
                .amount(h.getAmount())
                .dealerId(h.getDealer().getId())
                .dealerName(h.getDealer().getEponymia())
                .dealerCommissionPct(h.getDealerCommissionPct())
                .dealerCommissionAmount(h.getDealerCommissionAmount())
                .paidDealer(h.getPaidDealer())
                .networkId(h.getNetwork() != null ? h.getNetwork().getId() : null)
                .networkName(h.getNetwork() != null ? h.getNetwork().getEponymia() : null)
                .networkCommissionPct(h.getNetworkCommissionPct())
                .networkCommissionAmount(h.getNetworkCommissionAmount())
                .paidNetwork(h.getPaidNetwork())
                .receipt(h.getReceipt())
                .externalRef(h.getExternalRef())
                .build();
    }
}