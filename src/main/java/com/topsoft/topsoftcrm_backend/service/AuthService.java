package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.dto.request.LoginRequest;
import com.topsoft.topsoftcrm_backend.dto.response.LoginResponse;
import com.topsoft.topsoftcrm_backend.model.enums.UserRole;
import com.topsoft.topsoftcrm_backend.repository.AdminUserRepository;
import com.topsoft.topsoftcrm_backend.repository.DealerRepository;
import com.topsoft.topsoftcrm_backend.repository.NetworkRepository;
import com.topsoft.topsoftcrm_backend.repository.SubDealerRepository;
import com.topsoft.topsoftcrm_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final NetworkRepository networkRepository;
    private final DealerRepository dealerRepository;
    private final SubDealerRepository subDealerRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------ LOGIN
    public LoginResponse login(LoginRequest request) {
        String id = request.getId();
        if (id == null || id.length() != 8) {
            throw new RuntimeException("Μη έγκυρο ID");
        }

        char prefix = id.charAt(0);
        return switch (prefix) {
            case '0' -> loginAdmin(request);
            case '1' -> loginNetwork(request);
            case '2' -> loginDealer(request);
            case '3' -> loginSubDealer(request);
            default  -> throw new RuntimeException("Μη έγκυρο ID");
        };
    }

    // ----------------------------------------------- CALLED BY CONTROLLER
    // Allows the controller to generate a token after login without coupling
    // the token string to the LoginResponse DTO.
    public String generateTokenForUser(String id, String username, String role) {
        return jwtUtil.generateToken(id, username, role);
    }

    // ----------------------------------------- USED BY REFRESH ENDPOINT
    // Returns true if the user still exists and is active (not deactivated).
    // Admin accounts are always considered active.
    public boolean isUserActive(String id, String role) {
        try {
            return switch (role) {
                case "ADMIN" -> adminUserRepository.findById(id).isPresent();
                case "NETWORK" -> networkRepository.findById(id)
                        .map(n -> Boolean.TRUE.equals(n.getActive()))
                        .orElse(false);
                case "DEALER" -> dealerRepository.findById(id)
                        .map(d -> Boolean.TRUE.equals(d.getActive()))
                        .orElse(false);
                case "SUBDEALER" -> subDealerRepository.findById(id)
                        .map(s -> Boolean.TRUE.equals(s.getActive()))
                        .orElse(false);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------- PRIVATE LOGIN HELPERS
    private LoginResponse loginAdmin(LoginRequest req) {
        var admin = adminUserRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Λάθος στοιχεία"));

        if (!admin.getId().equals(req.getId()))
            throw new RuntimeException("Λάθος στοιχεία");

        if (!passwordEncoder.matches(req.getPassword(), admin.getPasswordHash()))
            throw new RuntimeException("Λάθος στοιχεία");

        return LoginResponse.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .role(UserRole.ADMIN)
                .build();
    }

    private LoginResponse loginNetwork(LoginRequest req) {
        var network = networkRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Λάθος στοιχεία"));

        if (!network.getId().equals(req.getId()))
            throw new RuntimeException("Λάθος στοιχεία");

        if (!passwordEncoder.matches(req.getPassword(), network.getPasswordHash()))
            throw new RuntimeException("Λάθος στοιχεία");

        if (!network.getActive())
            throw new RuntimeException("Ο λογαριασμός είναι ανενεργός");

        return LoginResponse.builder()
                .id(network.getId())
                .username(network.getUsername())
                .role(UserRole.NETWORK)
                .build();
    }

    private LoginResponse loginDealer(LoginRequest req) {
        var dealer = dealerRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Λάθος στοιχεία"));

        if (!dealer.getId().equals(req.getId()))
            throw new RuntimeException("Λάθος στοιχεία");

        if (!passwordEncoder.matches(req.getPassword(), dealer.getPasswordHash()))
            throw new RuntimeException("Λάθος στοιχεία");

        if (!dealer.getActive())
            throw new RuntimeException("Ο λογαριασμός είναι ανενεργός");

        return LoginResponse.builder()
                .id(dealer.getId())
                .username(dealer.getUsername())
                .role(UserRole.DEALER)
                .build();
    }

    private LoginResponse loginSubDealer(LoginRequest req) {
        var subDealer = subDealerRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Λάθος στοιχεία"));

        if (!subDealer.getId().equals(req.getId()))
            throw new RuntimeException("Λάθος στοιχεία");

        if (!passwordEncoder.matches(req.getPassword(), subDealer.getPasswordHash()))
            throw new RuntimeException("Λάθος στοιχεία");

        if (!subDealer.getActive())
            throw new RuntimeException("Ο λογαριασμός είναι ανενεργός");

        return LoginResponse.builder()
                .id(subDealer.getId())
                .username(subDealer.getUsername())
                .role(UserRole.SUBDEALER)
                .build();
    }
}