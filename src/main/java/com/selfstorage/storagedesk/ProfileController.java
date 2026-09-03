package com.selfstorage.storagedesk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ProfileController {
    private final AppUserRepository users;
    private final AuditLogRepository auditLogs;

    public ProfileController(AppUserRepository users, AuditLogRepository auditLogs) {
        this.users = users;
        this.auditLogs = auditLogs;
    }

    @PostMapping("/account/profile")
    public String updateProfile(@ModelAttribute ProfileForm form, Authentication authentication) throws IOException {
        AppUser user = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        String email = form.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCaseAndUsernameNot(email, user.getUsername())) {
            return "redirect:/?accountError=email";
        }
        String picture = saveProfilePicture(form.profilePicture());
        user.updateProfile(form.firstName().trim(), form.lastName().trim(), email, picture);
        users.save(user);
        auditLogs.save(new AuditLog(user.getUsername(), "UPDATED_PROFILE", "ACCOUNT", user.getUsername()));
        return "redirect:/?accountSaved=profile";
    }

    private String saveProfilePicture(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Profile picture must be an image.");
        }
        if (file.getSize() > 5_000_000) throw new IllegalArgumentException("Profile picture must be smaller than 5 MB.");
        Path directory = Path.of("data", "profile-photos");
        Files.createDirectories(directory);
        String extension = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.')) : ".img";
        String filename = UUID.randomUUID() + extension.toLowerCase();
        Files.write(directory.resolve(filename), file.getBytes());
        return "/profile-photos/" + filename;
    }

    public record ProfileForm(String firstName, String lastName, String email, MultipartFile profilePicture) {
        public ProfileForm() { this("", "", "", null); }
    }
}
