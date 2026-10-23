package com.selfstorage.storagedesk;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;

@Controller
public class DashboardController {
    private final ClientRepository clientRepository;
    private final StorageUnitRepository storageUnitRepository;
    private final PaymentRepository paymentRepository;
    private final AppUserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final HouseRepository houseRepository;
    private final RentPaymentRepository rentPaymentRepository;

    public DashboardController(ClientRepository clientRepository, StorageUnitRepository storageUnitRepository,
                               PaymentRepository paymentRepository, AppUserRepository userRepository,
                               AuditLogRepository auditLogRepository, HouseRepository houseRepository,
                               RentPaymentRepository rentPaymentRepository) {
        this.clientRepository = clientRepository;
        this.storageUnitRepository = storageUnitRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.houseRepository = houseRepository;
        this.rentPaymentRepository = rentPaymentRepository;
    }

    @GetMapping("/")
    public String dashboard() {
        return "forward:/app/index.html";
    }

    @PostMapping("/clients")
    public String createClient(@Valid @ModelAttribute("newClient") ClientForm form, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) return "redirect:/?error=client";
        AppUser owner = currentUser(authentication);
        Client client = clientRepository.save(new Client(form.name(), form.email(), form.phone(), form.notes(), owner));
        record(owner, "CREATED", "CLIENT", client.getId());
        return "redirect:/?saved=client";
    }

    @PostMapping("/clients/{id}/update")
    public String updateClient(@PathVariable Long id, @Valid @ModelAttribute ClientForm form, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) return "redirect:/?error=client";
        AppUser owner = currentUser(authentication);
        clientRepository.findById(id).filter(client -> client.getOwner().owns(owner))
            .ifPresent(client -> { client.update(form.name(), form.email(), form.phone(), form.notes()); record(owner, "UPDATED", "CLIENT", id); });
        return "redirect:/?saved=client";
    }

    @PostMapping("/clients/{id}/delete")
    public String deleteClient(@PathVariable Long id, Authentication authentication) {
        if (!storageUnitRepository.existsByClientId(id) && !paymentRepository.existsByClientId(id)
                && clientRepository.existsByIdAndOwner(id, currentUser(authentication))) {
            clientRepository.deleteById(id);
            record(currentUser(authentication), "DELETED", "CLIENT", id);
        }
        return "redirect:/?saved=client";
    }

    @PostMapping("/units")
    public String createUnit(@Valid @ModelAttribute("newUnit") UnitForm form, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) return "redirect:/?error=unit";
        AppUser owner = currentUser(authentication);
        Client client = form.clientId() == null ? null : clientRepository.findById(form.clientId()).filter(item -> item.getOwner().owns(owner)).orElseThrow();
        String status = normalizeStatus(form.status(), client != null);
        StorageUnit unit = storageUnitRepository.save(new StorageUnit(form.unitNumber(), form.size(), form.monthlyRate(), status, client, owner));
        record(owner, "CREATED", "UNIT", unit.getId());
        return "redirect:/?saved=unit";
    }

    @PostMapping("/units/{id}/update")
    public String updateUnit(@PathVariable Long id, @Valid @ModelAttribute UnitForm form, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) return "redirect:/?error=unit";
        AppUser owner = currentUser(authentication);
        Client client = form.clientId() == null ? null : clientRepository.findById(form.clientId()).filter(item -> item.getOwner().owns(owner)).orElseThrow();
        String status = normalizeStatus(form.status(), client != null);
        storageUnitRepository.findById(id).filter(unit -> unit.getOwner().owns(owner))
                .ifPresent(unit -> { unit.update(form.unitNumber(), form.size(), form.monthlyRate(), status, client); record(owner, "UPDATED", "UNIT", id); });
        return "redirect:/?saved=unit";
    }

    @PostMapping("/units/{id}/delete")
    public String deleteUnit(@PathVariable Long id, Authentication authentication) {
        storageUnitRepository.findById(id).filter(unit -> unit.getOwner().owns(currentUser(authentication)))
                .ifPresent(unit -> { storageUnitRepository.delete(unit); record(currentUser(authentication), "DELETED", "UNIT", id); });
        return "redirect:/?saved=unit";
    }

    @PostMapping("/payments")
    public String createPayment(@Valid @ModelAttribute("newPayment") PaymentForm form, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) return "redirect:/?error=payment";
        AppUser owner = currentUser(authentication);
        Client client = clientRepository.findById(form.clientId()).filter(item -> item.getOwner().owns(owner)).orElseThrow();
        Payment payment = paymentRepository.save(new Payment(client, form.amount(), form.billingMonth(),
            LocalDate.now(), form.method(), owner));
        record(owner, "CREATED", "PAYMENT", payment.getId());
        return "redirect:/?saved=payment";
    }

    @PostMapping("/payments/{id}/delete")
    public String deletePayment(@PathVariable Long id, Authentication authentication) {
        paymentRepository.findById(id).filter(payment -> payment.getOwner().owns(currentUser(authentication)))
                .ifPresent(payment -> { paymentRepository.delete(payment); record(currentUser(authentication), "DELETED", "PAYMENT", id); });
        return "redirect:/?saved=payment";
    }

        @PostMapping("/houses")
        public String createHouse(@Valid @ModelAttribute("newHouse") HouseForm form, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) return "redirect:/?error=house";
        AppUser owner = currentUser(authentication);
        Client tenant = form.tenantId() == null ? null : clientRepository.findById(form.tenantId())
            .filter(item -> item.getOwner().owns(owner)).orElseThrow();
        String status = normalizeStatus(form.status(), tenant != null);
        House house = houseRepository.save(new House(form.address(), form.monthlyRent(), status, tenant, owner));
        record(owner, "CREATED", "HOUSE", house.getId());
        return "redirect:/?saved=house";
        }

        @PostMapping("/houses/{id}/update")
        public String updateHouse(@PathVariable Long id, @Valid @ModelAttribute HouseForm form, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) return "redirect:/?error=house";
        AppUser owner = currentUser(authentication);
        Client tenant = form.tenantId() == null ? null : clientRepository.findById(form.tenantId())
            .filter(item -> item.getOwner().owns(owner)).orElseThrow();
        String status = normalizeStatus(form.status(), tenant != null);
        houseRepository.findById(id).filter(house -> house.getOwner().owns(owner))
            .ifPresent(house -> { house.update(form.address(), form.monthlyRent(), status, tenant); record(owner, "UPDATED", "HOUSE", id); });
        return "redirect:/?saved=house";
        }

        @PostMapping("/houses/{id}/delete")
        public String deleteHouse(@PathVariable Long id, Authentication authentication) {
        AppUser owner = currentUser(authentication);
        houseRepository.findById(id).filter(house -> house.getOwner().owns(owner))
            .ifPresent(house -> { houseRepository.delete(house); record(owner, "DELETED", "HOUSE", id); });
        return "redirect:/?saved=house";
        }

        @PostMapping("/rent-payments")
        public String createRentPayment(@Valid @ModelAttribute("newRentPayment") RentPaymentForm form,
                        BindingResult result, Authentication authentication) {
        if (result.hasErrors()) return "redirect:/?error=rent-payment";
        AppUser owner = currentUser(authentication);
        House house = houseRepository.findById(form.houseId()).filter(item -> item.getOwner().owns(owner)).orElseThrow();
        RentPayment payment = rentPaymentRepository.save(new RentPayment(house, form.amount(), form.billingMonth(),
            LocalDate.now(), form.method(), owner));
        record(owner, "CREATED", "RENT_PAYMENT", payment.getId());
        return "redirect:/?saved=rent-payment";
        }

        @PostMapping("/rent-payments/{id}/delete")
        public String deleteRentPayment(@PathVariable Long id, Authentication authentication) {
        AppUser owner = currentUser(authentication);
        rentPaymentRepository.findById(id).filter(payment -> payment.getOwner().owns(owner))
            .ifPresent(payment -> { rentPaymentRepository.delete(payment); record(owner, "DELETED", "RENT_PAYMENT", id); });
        return "redirect:/?saved=rent-payment";
        }

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
    }

    private String normalizeStatus(String status, boolean assigned) {
        if (assigned) return "Occupied";
        return "Available";
    }

    private void record(AppUser owner, String action, String targetType, Long targetId) {
        auditLogRepository.save(new AuditLog(owner.getUsername(), action, targetType, String.valueOf(targetId)));
    }

    public record ClientForm(String name, String email, String phone, String notes) {
        public ClientForm() { this("", "", "", ""); }
    }

    public record PaymentForm(Long clientId, BigDecimal amount, String billingMonth, String method) {
        public PaymentForm() { this(null, null, YearMonth.now().toString(), "Card"); }
    }

    public record UnitForm(String unitNumber, String size, BigDecimal monthlyRate, String status, Long clientId) {
        public UnitForm() { this("", "", null, "Available", null); }
    }

    public record HouseForm(String address, BigDecimal monthlyRent, String status, Long tenantId) {
        public HouseForm() { this("", null, "Available", null); }
    }

    public record RentPaymentForm(Long houseId, BigDecimal amount, String billingMonth, String method) {
        public RentPaymentForm() { this(null, null, YearMonth.now().toString(), "ACH"); }
    }
}
