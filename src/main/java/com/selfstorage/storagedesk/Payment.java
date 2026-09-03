package com.selfstorage.storagedesk;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Client client;

    private BigDecimal amount;
    private String billingMonth;
    private LocalDate paidOn;
    private String method;

    @ManyToOne
    private AppUser owner;

    protected Payment() {
    }

    public Payment(Client client, BigDecimal amount, String billingMonth, LocalDate paidOn, String method, AppUser owner) {
        this.client = client;
        this.amount = amount;
        this.billingMonth = billingMonth;
        this.paidOn = paidOn;
        this.method = method;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public Client getClient() { return client; }
    public BigDecimal getAmount() { return amount; }
    public String getBillingMonth() { return billingMonth; }
    public LocalDate getPaidOn() { return paidOn; }
    public String getMethod() { return method; }
    public AppUser getOwner() { return owner; }
    public void assignOwner(AppUser owner) { this.owner = owner; }
}
