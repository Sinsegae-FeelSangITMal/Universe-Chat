package com.sinse.chat.service;

import com.sinse.chat.domain.ChatMessage;
import com.sinse.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getRecentMessages(Long roomId) {
        // createdAt 역순으로 가져오므로, 서비스나 프론트에서 다시 순서를 뒤집어줘야 함
        return chatMessageRepository.findTop10ByRoomIdOrderByCreatedAtDesc(roomId);
    }
}
