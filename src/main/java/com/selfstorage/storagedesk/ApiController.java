package com.selfstorage.storagedesk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@Transactional
public class ApiController {
    private final AppUserRepository users;
    private final ClientRepository clients;
    private final StorageUnitRepository units;
    private final HouseRepository houses;
    private final PaymentRepository payments;
    private final RentPaymentRepository rentPayments;
    private final AuditLogRepository auditLogs;

    public ApiController(AppUserRepository users, ClientRepository clients, StorageUnitRepository units,
                         HouseRepository houses, PaymentRepository payments, RentPaymentRepository rentPayments,
                         AuditLogRepository auditLogs) {
        this.users = users;
        this.clients = clients;
        this.units = units;
        this.houses = houses;
        this.payments = payments;
        this.rentPayments = rentPayments;
        this.auditLogs = auditLogs;
    }

    @GetMapping("/dashboard")
    public DashboardView dashboard(Authentication authentication) {
        AppUser owner = owner(authentication);
        List<ClientView> clientList = clients.findAllByOwnerOrderByNameAsc(owner).stream().map(this::clientView).toList();
        List<UnitView> unitList = units.findAllByOwner(owner).stream().map(this::unitView).toList();
        List<HouseView> houseList = houses.findAllByOwner(owner).stream().map(this::houseView).toList();
        List<PaymentView> paymentList = payments.findTop8ByOwnerOrderByPaidOnDesc(owner).stream().map(this::paymentView).toList();
        List<RentPaymentView> rentPaymentList = rentPayments.findTop8ByOwnerOrderByPaidOnDesc(owner).stream()
                .map(this::rentPaymentView).toList();
        long occupiedUnits = units.countByOwnerAndStatus(owner, "Occupied");
        long occupiedHouses = houses.countByOwnerAndStatus(owner, "Occupied");
        String month = YearMonth.now().toString();
        long expected = occupiedUnits + occupiedHouses;
        long received = payments.countByOwnerAndBillingMonth(owner, month)
                + rentPayments.countByOwnerAndBillingMonth(owner, month);
        BigDecimal storageRevenue = unitList.stream().filter(unit -> "Occupied".equals(unit.status()))
                .map(UnitView::monthlyRate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal houseRevenue = houseList.stream().filter(house -> "Occupied".equals(house.status()))
                .map(HouseView::monthlyRent).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<NotificationView> notifications = Math.max(0, expected - received) == 0
                ? List.of()
                : List.of(new NotificationView("payments-due", "Payments need attention",
                        Math.max(0, expected - received) + " occupied properties have no payment recorded for " + month + "."));
        return new DashboardView(userView(owner), clientList.size(), occupiedUnits, unitList.size(),
                Math.max(0, expected - received), storageRevenue.add(houseRevenue), houseRevenue,
                occupiedHouses, houseList.size(), clientList, unitList, houseList, paymentList,
                rentPaymentList, notifications);
    }

    @GetMapping("/clients")
    public List<ClientView> clients(Authentication authentication) {
        return clients.findAllByOwnerOrderByNameAsc(owner(authentication)).stream().map(this::clientView).toList();
    }

    @PostMapping("/clients")
    public ClientView createClient(@Valid @RequestBody ClientRequest request, Authentication authentication) {
        AppUser owner = owner(authentication);
        Client client = clients.save(new Client(request.name().trim(), request.email().trim().toLowerCase(),
                request.phone().trim(), request.notes(), owner));
        record(owner, "CREATED", "CLIENT", client.getId());
        return clientView(client);
    }

    @PutMapping("/clients/{id}")
    public ClientView updateClient(@PathVariable Long id, @Valid @RequestBody ClientRequest request,
                                   Authentication authentication) {
        AppUser owner = owner(authentication);
        Client client = requireOwnedClient(id, owner);
        client.update(request.name().trim(), request.email().trim().toLowerCase(), request.phone().trim(), request.notes());
        record(owner, "UPDATED", "CLIENT", id);
        return clientView(client);
    }

    @DeleteMapping("/clients/{id}")
    public void deleteClient(@PathVariable Long id, Authentication authentication) {
        AppUser owner = owner(authentication);
        Client client = requireOwnedClient(id, owner);
        if (units.existsByClientId(id) || payments.existsByClientId(id) || houses.existsByTenantId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Remove this client's property assignments and payments first.");
        }
        clients.delete(client);
        record(owner, "DELETED", "CLIENT", id);
    }

    @GetMapping("/units")
    public List<UnitView> units(Authentication authentication) {
        return units.findAllByOwner(owner(authentication)).stream().map(this::unitView).toList();
    }

    @PostMapping("/units")
    public UnitView createUnit(@Valid @RequestBody UnitRequest request, Authentication authentication) {
        AppUser owner = owner(authentication);
        Client client = request.clientId() == null ? null : requireOwnedClient(request.clientId(), owner);
        StorageUnit unit = units.save(new StorageUnit(request.unitNumber().trim(), request.size().trim(),
                request.monthlyRate(), normalizeStatus(request.status(), client != null), client, owner));
        record(owner, "CREATED", "UNIT", unit.getId());
        return unitView(unit);
    }

    @PutMapping("/units/{id}")
    public UnitView updateUnit(@PathVariable Long id, @Valid @RequestBody UnitRequest request,
                               Authentication authentication) {
        AppUser owner = owner(authentication);
        StorageUnit unit = requireOwnedUnit(id, owner);
        Client client = request.clientId() == null ? null : requireOwnedClient(request.clientId(), owner);
        unit.update(request.unitNumber().trim(), request.size().trim(), request.monthlyRate(),
                normalizeStatus(request.status(), client != null), client);
        record(owner, "UPDATED", "UNIT", id);
        return unitView(unit);
    }

    @DeleteMapping("/units/{id}")
    public void deleteUnit(@PathVariable Long id, Authentication authentication) {
        AppUser owner = owner(authentication);
        StorageUnit unit = requireOwnedUnit(id, owner);
        units.delete(unit);
        record(owner, "DELETED", "UNIT", id);
    }

    @GetMapping("/houses")
    public List<HouseView> houses(Authentication authentication) {
        return houses.findAllByOwner(owner(authentication)).stream().map(this::houseView).toList();
    }

    @PostMapping("/houses")
    public HouseView createHouse(@Valid @RequestBody HouseRequest request, Authentication authentication) {
        AppUser owner = owner(authentication);
        Client tenant = request.tenantId() == null ? null : requireOwnedClient(request.tenantId(), owner);
        House house = houses.save(new House(request.address().trim(), request.monthlyRent(),
                normalizeStatus(request.status(), tenant != null), tenant, owner));
        record(owner, "CREATED", "HOUSE", house.getId());
        return houseView(house);
    }

    @PutMapping("/houses/{id}")
    public HouseView updateHouse(@PathVariable Long id, @Valid @RequestBody HouseRequest request,
                                 Authentication authentication) {
        AppUser owner = owner(authentication);
        House house = requireOwnedHouse(id, owner);
        Client tenant = request.tenantId() == null ? null : requireOwnedClient(request.tenantId(), owner);
        house.update(request.address().trim(), request.monthlyRent(), normalizeStatus(request.status(), tenant != null), tenant);
        record(owner, "UPDATED", "HOUSE", id);
        return houseView(house);
    }

    @DeleteMapping("/houses/{id}")
    public void deleteHouse(@PathVariable Long id, Authentication authentication) {
        AppUser owner = owner(authentication);
        House house = requireOwnedHouse(id, owner);
        if (rentPayments.existsByHouseId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Remove this house's rent payments first.");
        }
        houses.delete(house);
        record(owner, "DELETED", "HOUSE", id);
    }

    @GetMapping("/payments")
    public List<PaymentView> payments(Authentication authentication) {
        return payments.findTop8ByOwnerOrderByPaidOnDesc(owner(authentication)).stream().map(this::paymentView).toList();
    }

    @PostMapping("/payments")
    public PaymentView createPayment(@Valid @RequestBody PaymentRequest request, Authentication authentication) {
        AppUser owner = owner(authentication);
        Client client = requireOwnedClient(request.clientId(), owner);
        Payment payment = payments.save(new Payment(client, request.amount(), request.billingMonth(),
                LocalDate.now(), request.method(), owner));
        record(owner, "CREATED", "PAYMENT", payment.getId());
        return paymentView(payment);
    }

    @DeleteMapping("/payments/{id}")
    public void deletePayment(@PathVariable Long id, Authentication authentication) {
        AppUser owner = owner(authentication);
        Payment payment = payments.findById(id).filter(item -> item.getOwner().owns(owner))
                .orElseThrow(() -> notFound("Payment"));
        payments.delete(payment);
        record(owner, "DELETED", "PAYMENT", id);
    }

    @GetMapping("/rent-payments")
    public List<RentPaymentView> rentPayments(Authentication authentication) {
        return rentPayments.findTop8ByOwnerOrderByPaidOnDesc(owner(authentication)).stream()
                .map(this::rentPaymentView).toList();
    }

    @PostMapping("/rent-payments")
    public RentPaymentView createRentPayment(@Valid @RequestBody RentPaymentRequest request,
                                             Authentication authentication) {
        AppUser owner = owner(authentication);
        House house = requireOwnedHouse(request.houseId(), owner);
        RentPayment payment = rentPayments.save(new RentPayment(house, request.amount(), request.billingMonth(),
                LocalDate.now(), request.method(), owner));
        record(owner, "CREATED", "RENT_PAYMENT", payment.getId());
        return rentPaymentView(payment);
    }

    @DeleteMapping("/rent-payments/{id}")
    public void deleteRentPayment(@PathVariable Long id, Authentication authentication) {
        AppUser owner = owner(authentication);
        RentPayment payment = rentPayments.findById(id).filter(item -> item.getOwner().owns(owner))
                .orElseThrow(() -> notFound("Rent payment"));
        rentPayments.delete(payment);
        record(owner, "DELETED", "RENT_PAYMENT", id);
    }

    private AppUser owner(Authentication authentication) {
        return users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow(() -> notFound("Account"));
    }

    private Client requireOwnedClient(Long id, AppUser owner) {
        return clients.findById(id).filter(item -> item.getOwner().owns(owner))
                .orElseThrow(() -> notFound("Client"));
    }

    private StorageUnit requireOwnedUnit(Long id, AppUser owner) {
        return units.findById(id).filter(item -> item.getOwner().owns(owner))
                .orElseThrow(() -> notFound("Storage unit"));
    }

    private House requireOwnedHouse(Long id, AppUser owner) {
        return houses.findById(id).filter(item -> item.getOwner().owns(owner))
                .orElseThrow(() -> notFound("House"));
    }

    private UserView userView(AppUser user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String name = (firstName + " " + lastName).trim();
        return new UserView(user.getUsername(), user.getEmail(), name.isBlank() ? user.getUsername() : name,
                firstName, lastName, user.getProfilePicture());
    }

    private ClientView clientView(Client client) {
        return new ClientView(client.getId(), client.getName(), client.getEmail(), client.getPhone(), client.getNotes());
    }

    private UnitView unitView(StorageUnit unit) {
        return new UnitView(unit.getId(), unit.getUnitNumber(), unit.getSize(), unit.getMonthlyRate(),
                unit.getStatus(), unit.getClient() == null ? null : unit.getClient().getId(),
                unit.getClient() == null ? null : unit.getClient().getName());
    }

    private HouseView houseView(House house) {
        return new HouseView(house.getId(), house.getAddress(), house.getMonthlyRent(), house.getStatus(),
                house.getTenant() == null ? null : house.getTenant().getId(),
                house.getTenant() == null ? null : house.getTenant().getName());
    }

    private PaymentView paymentView(Payment payment) {
        return new PaymentView(payment.getId(), payment.getClient().getId(), payment.getClient().getName(),
                payment.getAmount(), payment.getBillingMonth(), payment.getPaidOn(), payment.getMethod());
    }

    private RentPaymentView rentPaymentView(RentPayment payment) {
        return new RentPaymentView(payment.getId(), payment.getHouse().getId(), payment.getHouse().getAddress(),
                payment.getHouse().getTenant() == null ? null : payment.getHouse().getTenant().getName(),
                payment.getAmount(), payment.getBillingMonth(), payment.getPaidOn(), payment.getMethod());
    }

    private String normalizeStatus(String status, boolean assigned) {
        return assigned ? "Occupied" : "Available";
    }

    private void record(AppUser owner, String action, String targetType, Long targetId) {
        auditLogs.save(new AuditLog(owner.getUsername(), action, targetType, String.valueOf(targetId)));
    }

    private ResponseStatusException notFound(String name) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, name + " not found.");
    }

    public record UserView(String username, String email, String displayName, String firstName,
                           String lastName, String profilePicture) {}
    public record ClientView(Long id, String name, String email, String phone, String notes) {}
    public record UnitView(Long id, String unitNumber, String size, BigDecimal monthlyRate, String status,
                           Long clientId, String clientName) {}
    public record HouseView(Long id, String address, BigDecimal monthlyRent, String status,
                            Long tenantId, String tenantName) {}
    public record PaymentView(Long id, Long clientId, String clientName, BigDecimal amount, String billingMonth,
                              LocalDate paidOn, String method) {}
    public record RentPaymentView(Long id, Long houseId, String houseAddress, String tenantName,
                                  BigDecimal amount, String billingMonth, LocalDate paidOn, String method) {}
    public record NotificationView(String id, String title, String message) {}
    public record DashboardView(UserView user, long clientCount, long occupiedUnits, long totalUnits,
                                long paymentsDue, BigDecimal monthlyRevenue, BigDecimal monthlyRentRevenue,
                                long occupiedHouses, long totalHouses, List<ClientView> clients,
                                List<UnitView> units, List<HouseView> houses, List<PaymentView> payments,
                                List<RentPaymentView> rentPayments, List<NotificationView> notifications) {}
    public record ClientRequest(@NotBlank String name, @Email @NotBlank String email, @NotBlank String phone, String notes) {}
    public record UnitRequest(@NotBlank String unitNumber, @NotBlank String size,
                             @NotNull @PositiveOrZero BigDecimal monthlyRate, String status, Long clientId) {}
    public record HouseRequest(@NotBlank String address, @NotNull @PositiveOrZero BigDecimal monthlyRent,
                               String status, Long tenantId) {}
    public record PaymentRequest(@NotNull Long clientId, @NotNull @PositiveOrZero BigDecimal amount,
                                 @NotBlank String billingMonth, @NotBlank String method) {}
    public record RentPaymentRequest(@NotNull Long houseId, @NotNull @PositiveOrZero BigDecimal amount,
                                     @NotBlank String billingMonth, @NotBlank String method) {}
}
