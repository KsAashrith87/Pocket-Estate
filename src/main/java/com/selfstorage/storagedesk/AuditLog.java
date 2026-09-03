package com.selfstorage.storagedesk;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String action;
    private String targetType;
    private String targetId;
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(String username, String action, String targetType, String targetId) {
        this.username = username;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.createdAt = Instant.now();
    }

    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public Instant getCreatedAt() { return createdAt; }
}
