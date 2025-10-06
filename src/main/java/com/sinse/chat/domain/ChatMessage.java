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
@Table(name = "chat_message")
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int roomId;

    private int senderId;

    private String content;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    public enum ContentType {
        TEXT,
        IMAGE,
        SYSTEM
    }
}