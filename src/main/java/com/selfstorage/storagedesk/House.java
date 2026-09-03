package com.selfstorage.storagedesk;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Entity
public class House {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String address;
    private BigDecimal monthlyRent;
    private String status;
    @ManyToOne
    private Client tenant;
    @ManyToOne
    private AppUser owner;

    protected House() {}

    public House(String address, BigDecimal monthlyRent, String status, Client tenant, AppUser owner) {
        this.address = address;
        this.monthlyRent = monthlyRent;
        this.status = status;
        this.tenant = tenant;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getAddress() { return address; }
    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public String getStatus() { return status; }
    public Client getTenant() { return tenant; }
    public AppUser getOwner() { return owner; }
    public void update(String address, BigDecimal monthlyRent, String status, Client tenant) {
        this.address = address;
        this.monthlyRent = monthlyRent;
        this.status = status;
        this.tenant = tenant;
    }
}
