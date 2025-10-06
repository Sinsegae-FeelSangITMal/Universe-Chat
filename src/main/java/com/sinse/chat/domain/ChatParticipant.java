
package com.sinse.chat.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_participant")
@Data
public class ChatParticipant {

    @EmbeddedId
    private ChatParticipantId id;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime joinedAt;

    public enum Role {
        USER,
        PARTNER,
        ADMIN
    }

    // EmbeddedId class
    @Data
    public static class ChatParticipantId implements Serializable {
        private Integer roomId;
        private Integer userId;
    }
}
