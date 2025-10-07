package com.sinse.chat.repository;

import com.sinse.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 방의 메시지를 최신 순으로 10개 조회
     */
    List<ChatMessage> findTop10ByRoomIdOrderByCreatedAtDesc(Long roomId);

}