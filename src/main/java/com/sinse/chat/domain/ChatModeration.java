package com.sinse.chat.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_moderation")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatModeration {

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name = "roomId", column = @Column(name = "room_id", nullable = false)),
            @AttributeOverride(name = "userId", column = @Column(name = "user_id", nullable = false)),
            @AttributeOverride(name = "type",   column = @Column(name = "type",    nullable = false))
    })
    private ChatModerationId id;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "until_at")
    private LocalDateTime untilAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum ModerationType {
        MUTE, BAN
    }

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ChatModerationId implements Serializable {
        private Long roomId;
        private Long userId;

        @Enumerated(EnumType.STRING)
        private ModerationType type; // ← 여기서만 매핑 (엔티티 본문에는 없음)
    }
}
