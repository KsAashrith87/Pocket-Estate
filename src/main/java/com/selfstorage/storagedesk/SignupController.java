package com.selfstorage.storagedesk;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Controller
public class SignupController {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public SignupController(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "signup";
    }

    @PostMapping(value = "/signup", consumes = "multipart/form-data")
    public String createAccount(@ModelAttribute SignupForm form, Model model) throws IOException {
        if (form.firstName() == null || form.firstName().trim().isEmpty()
                || form.lastName() == null || form.lastName().trim().isEmpty()) {
            return signupError(model, "First and last name are required.", form);
        }
        if (form.username() == null || form.username().trim().length() < 3) {
            return signupError(model, "Username must be at least 3 characters.", form);
        }
        if (form.email() == null || !form.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return signupError(model, "Enter a valid email address.", form);
        }
        if (form.password() == null || form.password().length() < 8) {
            return signupError(model, "Password must be at least 8 characters.", form);
        }
        if (!form.password().equals(form.confirmPassword())) {
            return signupError(model, "Passwords do not match.", form);
        }
        String username = form.username().trim().toLowerCase();
        String email = form.email().trim().toLowerCase();
        if (users.findByUsernameIgnoreCase(username).isPresent() || users.findByEmailIgnoreCase(email).isPresent()) {
            return signupError(model, "That username is already in use.", form);
        }
        String profilePicture = saveProfilePicture(form.profilePicture());
        users.save(new AppUser(username, email, form.firstName().trim(), form.lastName().trim(), profilePicture,
            passwordEncoder.encode(form.password()), "MANAGER"));
        return "redirect:/login?created";
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

    private String signupError(Model model, String message, SignupForm form) {
        model.addAttribute("errorMessage", message);
        model.addAttribute("signupForm", form);
        return "signup";
    }

    public record SignupForm(String firstName, String lastName, String username, String email,
                             MultipartFile profilePicture, String password, String confirmPassword) {
        public SignupForm() { this("", "", "", "", null, "", ""); }
    }
}
