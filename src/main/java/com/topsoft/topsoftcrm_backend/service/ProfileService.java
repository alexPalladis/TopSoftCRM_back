package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.exception.ResourceNotFoundException;
import com.topsoft.topsoftcrm_backend.repository.AdminUserRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
import com.topsoft.topsoftcrm_backend.repository.NetworkRepository;
import com.topsoft.topsoftcrm_backend.repository.SubDealerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AdminUserRepository adminUserRepository;
    private final NetworkRepository networkRepository;
    private final DealerRepository dealerRepository;
    private final SubDealerRepository subDealerRepository;
    private final PasswordEncoder      passwordEncoder;

    @Transactional
    public void changePassword(String id, String role, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6)
            throw new RuntimeException("Το νέο password πρέπει να έχει τουλάχιστον 6 χαρακτήρες");

        switch (role) {
            case "ADMIN" -> {
                var admin = adminUserRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Admin δεν βρέθηκε"));
                if (!passwordEncoder.matches(currentPassword, admin.getPasswordHash()))
                    throw new RuntimeException("Λάθος τρέχον password");
                admin.setPasswordHash(passwordEncoder.encode(newPassword));
                adminUserRepository.save(admin);
            }
            case "NETWORK" -> {
                var network = networkRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Network δεν βρέθηκε"));
                if (!passwordEncoder.matches(currentPassword, network.getPasswordHash()))
                    throw new RuntimeException("Λάθος τρέχον password");
                network.setPasswordHash(passwordEncoder.encode(newPassword));
                networkRepository.save(network);
            }
            case "DEALER" -> {
                var dealer = dealerRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Dealer δεν βρέθηκε"));
                if (!passwordEncoder.matches(currentPassword, dealer.getPasswordHash()))
                    throw new RuntimeException("Λάθος τρέχον password");
                dealer.setPasswordHash(passwordEncoder.encode(newPassword));
                dealerRepository.save(dealer);
            }
            case "SUBDEALER" -> {
                var sub = subDealerRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("SubDealer δεν βρέθηκε"));
                if (!passwordEncoder.matches(currentPassword, sub.getPasswordHash()))
                    throw new RuntimeException("Λάθος τρέχον password");
                sub.setPasswordHash(passwordEncoder.encode(newPassword));
                subDealerRepository.save(sub);
            }
            default -> throw new RuntimeException("Άγνωστος ρόλος");
        }
    }
}
