
package com.sinse.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinse.chat.domain.ChatMessage;
import com.sinse.chat.dto.response.ChatMessageResponse;
import com.sinse.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisToMySqlBatchService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private static final String CHAT_ROOM_PREFIX = "CHAT_ROOM:";

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void transferChatMessages() {
        log.info("Starting chat message transfer from Redis to MySQL...");

        Set<String> roomKeys = redisTemplate.keys(CHAT_ROOM_PREFIX + "*");
        if (roomKeys == null || roomKeys.isEmpty()) {
            log.info("No active chat rooms found in Redis. Nothing to transfer.");
            return;
        }

        for (String key : roomKeys) {
            List<Object> dtoList = redisTemplate.opsForList().range(key, 0, -1);
            if (dtoList == null || dtoList.isEmpty()) {
                continue;
            }

            List<ChatMessage> entitiesToSave = dtoList.stream()
                .map(dto -> objectMapper.convertValue(dto, ChatMessageResponse.class))
                .map(response -> ChatMessage.builder()
                    .roomId(response.roomId())
                    .senderId(response.senderId())
                    .content(response.content())
                    .contentType(response.contentType())
                    .createdAt(response.createdAt())
                    .build())
                .collect(Collectors.toList());

            try {
                chatMessageRepository.saveAll(entitiesToSave);
                // Clear the list in Redis after successful save
                redisTemplate.delete(key);
                log.info("Successfully transferred {} messages for room key {}.", entitiesToSave.size(), key);
            } catch (Exception e) {
                log.error("Error transferring chat messages for room key {}: {}", key, e.getMessage());
            }
        }
        log.info("Chat message transfer finished.");
    }
}
