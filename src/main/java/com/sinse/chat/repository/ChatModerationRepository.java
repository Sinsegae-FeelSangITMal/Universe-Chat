
package com.sinse.chat.repository;

import com.sinse.chat.domain.ChatModeration;
import com.sinse.chat.domain.ChatModeration.ChatModerationId;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sinse.chat.domain.ChatModeration.ModerationType;

public interface ChatModerationRepository extends JpaRepository<ChatModeration, ChatModerationId> {
    boolean existsById_RoomIdAndId_UserIdAndId_Type(int roomId, int userId, ModerationType type);
}
