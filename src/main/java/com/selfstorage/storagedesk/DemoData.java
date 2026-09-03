package com.selfstorage.storagedesk;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class DemoData {
    @Value("${app.demo-data-enabled:false}")
    private boolean demoDataEnabled;
    @Value("${app.demo-username:demo}")
    private String demoUsername;
    @Value("${app.demo-password:}")
    private String demoPassword;

    @Bean
    CommandLineRunner seed(ClientRepository clients, StorageUnitRepository units, PaymentRepository payments,
                           AppUserRepository users, org.springframework.security.crypto.password.PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                AppUser firstUser = users.findAll().get(0);
                clients.findAll().stream().filter(item -> item.getOwner() == null).forEach(item -> {
                    item.assignOwner(firstUser);
                    clients.save(item);
                });
                units.findAll().stream().filter(item -> item.getOwner() == null).forEach(item -> {
                    item.assignOwner(firstUser);
                    units.save(item);
                });
                payments.findAll().stream().filter(item -> item.getOwner() == null).forEach(item -> {
                    item.assignOwner(firstUser);
                    payments.save(item);
                });
            }
            if (!demoDataEnabled) return;
                if (demoPassword == null || demoPassword.length() < 8) {
                throw new IllegalStateException("APP_DEMO_DATA_ENABLED requires APP_DEMO_PASSWORD with at least 8 characters");
                }
                AppUser demoOwner = users.findByUsernameIgnoreCase(demoUsername)
                    .orElseGet(() -> users.save(new AppUser(demoUsername, encoder.encode(demoPassword), "MANAGER")));
            if (clients.count() > 0) return;
                Client maya = clients.save(new Client("Maya Chen", "maya.chen@example.com", "(415) 555-0148", "Business inventory", demoOwner));
                Client jordan = clients.save(new Client("Jordan Alvarez", "jordan.a@example.com", "(415) 555-0192", "", demoOwner));
                Client priya = clients.save(new Client("Priya Shah", "priya.shah@example.com", "(415) 555-0126", "Moving house", demoOwner));

                units.save(new StorageUnit("A-104", "10 × 15", new BigDecimal("189.00"), "Occupied", maya, demoOwner));
                units.save(new StorageUnit("B-207", "10 × 10", new BigDecimal("129.00"), "Occupied", jordan, demoOwner));
                units.save(new StorageUnit("C-018", "5 × 10", new BigDecimal("89.00"), "Occupied", priya, demoOwner));
                units.save(new StorageUnit("A-112", "10 × 20", new BigDecimal("249.00"), "Available", null, demoOwner));

                payments.save(new Payment(maya, new BigDecimal("189.00"), YearMonth.now().toString(), LocalDate.now().minusDays(1), "Card", demoOwner));
                payments.save(new Payment(jordan, new BigDecimal("129.00"), YearMonth.now().toString(), LocalDate.now().minusDays(2), "ACH", demoOwner));
                payments.save(new Payment(priya, new BigDecimal("89.00"), YearMonth.now().toString(), LocalDate.now().minusDays(4), "Card", demoOwner));
        };
    }
}
