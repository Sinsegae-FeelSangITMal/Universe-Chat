package com.sinse.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sinse.chat.domain.ChatMessage.ContentType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        int roomId,
        int senderId,
        String nickname,
        String role, // 역할 추가
        String content,
        ContentType contentType,
        LocalDateTime createdAt
) {}