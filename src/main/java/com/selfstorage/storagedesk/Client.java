package com.selfstorage.storagedesk;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String phone;

    private String notes;

    @ManyToOne
    private AppUser owner;

    protected Client() {
    }

    public Client(String name, String email, String phone, String notes, AppUser owner) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.notes = notes;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getNotes() { return notes; }
    public AppUser getOwner() { return owner; }
    public void assignOwner(AppUser owner) { this.owner = owner; }
    public void update(String name, String email, String phone, String notes) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.notes = notes;
    }
}
