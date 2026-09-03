package com.selfstorage.storagedesk;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Entity
public class StorageUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String unitNumber;

    @NotBlank
    private String size;

    private BigDecimal monthlyRate;
    private String status;

    @ManyToOne
    private Client client;

    @ManyToOne
    private AppUser owner;

    protected StorageUnit() {
    }

    public StorageUnit(String unitNumber, String size, BigDecimal monthlyRate, String status, Client client, AppUser owner) {
        this.unitNumber = unitNumber;
        this.size = size;
        this.monthlyRate = monthlyRate;
        this.status = status;
        this.client = client;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getUnitNumber() { return unitNumber; }
    public String getSize() { return size; }
    public BigDecimal getMonthlyRate() { return monthlyRate; }
    public String getStatus() { return status; }
    public Client getClient() { return client; }
    public AppUser getOwner() { return owner; }
    public void assignOwner(AppUser owner) { this.owner = owner; }
    public void update(String unitNumber, String size, BigDecimal monthlyRate, String status, Client client) {
        this.unitNumber = unitNumber;
        this.size = size;
        this.monthlyRate = monthlyRate;
        this.status = status;
        this.client = client;
    }
}
