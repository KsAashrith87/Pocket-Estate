package com.selfstorage.storagedesk;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Controller
public class AccountController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogs;

    public AccountController(AppUserRepository users, PasswordEncoder passwordEncoder, AuditLogRepository auditLogs) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.auditLogs = auditLogs;
    }

    @GetMapping("/settings/account")
    public String accountSettings(Authentication authentication, Model model) {
        AppUser user = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        model.addAttribute("user", user);
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String name = (firstName + " " + lastName).trim();
        String initials = name.isBlank() ? user.getUsername().substring(0, Math.min(2, user.getUsername().length())).toUpperCase()
            : name.substring(0, 1).toUpperCase();
        model.addAttribute("accountInitials", initials);
        model.addAttribute("profileForm", new ProfileForm(user.getFirstName(), user.getLastName(), user.getEmail(), null));
        return "account-settings";
    }

    @PostMapping(value = "/settings/account", consumes = "multipart/form-data")
    public String updateProfile(@ModelAttribute ProfileForm form, Authentication authentication) throws IOException {
        AppUser user = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        if (form.email() == null || !form.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return "redirect:/settings/account?error=email";
        }
        String email = form.email().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).filter(other -> !other.owns(user)).isPresent()) {
            return "redirect:/settings/account?error=email-taken";
        }
        String photo = saveProfilePicture(form.profilePicture());
        if (form.firstName() == null || form.firstName().isBlank() || form.lastName() == null || form.lastName().isBlank()) {
            return "redirect:/settings/account?error=name";
        }
        user.updateProfile(form.firstName().trim(), form.lastName().trim(), email, photo);
        users.save(user);
        auditLogs.save(new AuditLog(user.getUsername(), "UPDATED_PROFILE", "ACCOUNT", user.getUsername()));
        return "redirect:/settings/account?saved=true";
    }

    private String saveProfilePicture(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) throw new IllegalArgumentException("Profile picture must be an image.");
        if (file.getSize() > 5_000_000) throw new IllegalArgumentException("Profile picture must be smaller than 5 MB.");
        Path directory = Path.of("data", "profile-photos");
        Files.createDirectories(directory);
        String extension = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.')) : ".img";
        String filename = UUID.randomUUID() + extension.toLowerCase();
        Files.write(directory.resolve(filename), file.getBytes());
        return "/profile-photos/" + filename;
    }

    @PostMapping("/account/password")
    public String changePassword(@ModelAttribute PasswordForm form, Authentication authentication, Model model) {
        AppUser user = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        if (!passwordEncoder.matches(form.currentPassword(), user.getPassword())) {
            return "redirect:/?accountError=current-password";
        }
        if (form.newPassword() == null || form.newPassword().length() < 8
                || !form.newPassword().equals(form.confirmPassword())) {
            return "redirect:/?accountError=password-rules";
        }
        user.changePassword(passwordEncoder.encode(form.newPassword()));
        users.save(user);
        auditLogs.save(new AuditLog(user.getUsername(), "CHANGED_PASSWORD", "ACCOUNT", user.getUsername()));
        return "redirect:/?accountSaved=password";
    }

    public record PasswordForm(String currentPassword, String newPassword, String confirmPassword) {
        public PasswordForm() { this("", "", ""); }
    }

    public record ProfileForm(String firstName, String lastName, String email, MultipartFile profilePicture) {}
}
