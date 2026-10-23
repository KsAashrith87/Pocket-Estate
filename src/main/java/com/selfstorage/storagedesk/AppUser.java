package com.selfstorage.storagedesk;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @jakarta.persistence.Column(unique = true, nullable = false)
    private String username;
    @Column(unique = true)
    private String email;
    @Column(length = 120, nullable = false)
    private String firstName;
    @Column(length = 120, nullable = false)
    private String lastName;
    @Column(length = 500)
    private String profilePicture;
    private String password;
    private String role;

    protected AppUser() {
    }

    public AppUser(String username, String password, String role) {
        this(username, null, password, role);
    }

    public AppUser(String username, String email, String password, String role) {
        this(username, email, "", "", null, password, role);
    }

    public AppUser(String username, String email, String firstName, String lastName,
                   String profilePicture, String password, String role) {
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePicture = profilePicture;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getProfilePicture() { return profilePicture; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public void changePassword(String encodedPassword) { this.password = encodedPassword; }
    public void updateProfile(String firstName, String lastName, String email, String profilePicture) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        if (profilePicture != null) this.profilePicture = profilePicture;
    }

    public boolean owns(AppUser other) {
        return other != null && id != null && id.equals(other.id);
    }
}
