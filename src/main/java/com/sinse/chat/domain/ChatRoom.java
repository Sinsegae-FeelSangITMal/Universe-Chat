
package com.sinse.chat.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room")
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Data
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    @Enumerated(EnumType.STRING)
    private RoomType type;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum RoomType {
        LIVE,
        DM,
        GROUP
    }
}
