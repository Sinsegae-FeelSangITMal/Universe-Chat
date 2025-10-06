package com.sinse.chat.service;

import com.sinse.chat.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CHAT_ROOM_PREFIX = "CHAT_ROOM:";

    public void saveMessage(ChatMessageResponse messageResponse) {

        String key = CHAT_ROOM_PREFIX + messageResponse.roomId();
        log.debug("key : " + key);
        redisTemplate.opsForList().rightPush(key, messageResponse);
    }

    public List<Object> getRecentMessages(int roomId, long count) {
        String key = CHAT_ROOM_PREFIX + roomId;
        return redisTemplate.opsForList().range(key, -count, -1);
    }
}