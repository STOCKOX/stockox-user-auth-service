package com.stockox.data;

import com.stockox.entity.Role;
import com.stockox.enums.UserRole;
import com.stockox.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor

public class DataSeeder implements CommandLineRunner {
    private final RoleRepository roleRepository;

    private static final Map<UserRole, String> ROLE_DESCRIPTIONS = Map.of(
            UserRole.SUPER_ADMIN, "Platform super administrator — manages all companies",
            UserRole.ADMIN,       "Company administrator — manages users, settings, full access",
            UserRole.MANAGER,     "Inventory manager — manages stock, orders, warehouses",
            UserRole.STAFF,       "Staff member — basic access to view and record transactions"
    );

    @Override
    public void run(String... args) {
        log.info("Running DataSeeder — checking and seeding roles...");
        int seeded = 0;
        for(UserRole roleEnum : UserRole.values()) {
            //insert only if role not exist in db
            if(roleRepository.findByName(roleEnum).isEmpty()) {
                Role role = Role.builder()
                        .name(roleEnum)
                        .description(ROLE_DESCRIPTIONS.get(roleEnum))
                        .build();

                roleRepository.save(role);
                log.info("Seeded role: {}", roleEnum.name());
                seeded++;
            }
        }

        if (seeded == 0) {
            log.info("  All roles already exist — skipping.");
        } else {
            log.info("DataSeeder complete — {} role(s) seeded.", seeded);
        }
    }
}
