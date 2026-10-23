package com.selfstorage.storagedesk;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PortfolioController {
    private final AppUserRepository users;
    private final HouseRepository houses;
    private final StorageUnitRepository units;
    private final ClientRepository clients;
    private final RentPaymentRepository rentPayments;
    private final PaymentRepository payments;

    public PortfolioController(AppUserRepository users, HouseRepository houses, StorageUnitRepository units,
                               ClientRepository clients, RentPaymentRepository rentPayments, PaymentRepository payments) {
        this.users = users;
        this.houses = houses;
        this.units = units;
        this.clients = clients;
        this.rentPayments = rentPayments;
        this.payments = payments;
    }

    @GetMapping("/houses")
    public String houses(Model model, Authentication authentication) {
        AppUser owner = owner(authentication);
        model.addAttribute("accountName", displayName(owner));
        model.addAttribute("accountInitials", initials(owner));
        model.addAttribute("profilePicture", owner.getProfilePicture());
        model.addAttribute("houses", houses.findAllByOwner(owner));
        long totalHouses = houses.countByOwner(owner);
        long occupiedHouses = houses.countByOwnerAndStatus(owner, "Occupied");
        model.addAttribute("occupiedHouseCount", occupiedHouses);
        model.addAttribute("houseOccupancyPercent", totalHouses == 0 ? 0 : occupiedHouses * 100 / totalHouses);
        model.addAttribute("clients", clients.findAllByOwnerOrderByNameAsc(owner));
        model.addAttribute("rentPayments", rentPayments.findTop8ByOwnerOrderByPaidOnDesc(owner));
        model.addAttribute("newHouse", new DashboardController.HouseForm());
        model.addAttribute("newRentPayment", new DashboardController.RentPaymentForm());
        return "houses";
    }

    @GetMapping("/clients")
    public String clients(Model model, Authentication authentication) {
        AppUser owner = owner(authentication);
        model.addAttribute("accountName", displayName(owner));
        model.addAttribute("accountInitials", initials(owner));
        model.addAttribute("profilePicture", owner.getProfilePicture());
        model.addAttribute("clients", clients.findAllByOwnerOrderByNameAsc(owner));
        model.addAttribute("units", units.findAllByOwner(owner));
        model.addAttribute("houses", houses.findAllByOwner(owner));
        model.addAttribute("newClient", new DashboardController.ClientForm());
        return "clients";
    }

    @GetMapping("/units")
    public String units(Model model, Authentication authentication) {
        AppUser owner = owner(authentication);
        model.addAttribute("accountName", displayName(owner));
        model.addAttribute("accountInitials", initials(owner));
        model.addAttribute("profilePicture", owner.getProfilePicture());
        model.addAttribute("units", units.findAllByOwner(owner));
        long totalUnits = units.countByOwner(owner);
        long occupiedUnits = units.countByOwnerAndStatus(owner, "Occupied");
        model.addAttribute("occupiedCount", occupiedUnits);
        model.addAttribute("occupancyPercent", totalUnits == 0 ? 0 : occupiedUnits * 100 / totalUnits);
        model.addAttribute("clients", clients.findAllByOwnerOrderByNameAsc(owner));
        model.addAttribute("newUnit", new DashboardController.UnitForm());
        model.addAttribute("payments", payments.findTop8ByOwnerOrderByPaidOnDesc(owner));
        return "units";
    }

    private AppUser owner(Authentication authentication) {
        return users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
    }

    private String displayName(AppUser owner) {
        String firstName = owner.getFirstName() == null ? "" : owner.getFirstName().trim();
        String lastName = owner.getLastName() == null ? "" : owner.getLastName().trim();
        String name = (firstName + " " + lastName).trim();
        return name.isBlank() ? owner.getUsername() : name;
    }

    private String initials(AppUser owner) {
        String name = displayName(owner).replaceAll("[^A-Za-z ]", "").trim();
        if (name.contains(" ")) return ("" + name.charAt(0) + name.charAt(name.indexOf(' ') + 1)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
