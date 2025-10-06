
package com.sinse.chat.repository;

import com.sinse.chat.domain.ChatModeration;
import com.sinse.chat.domain.ChatModeration.ChatModerationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatModerationRepository extends JpaRepository<ChatModeration, ChatModerationId> {
}
