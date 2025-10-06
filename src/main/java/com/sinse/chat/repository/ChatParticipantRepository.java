
package com.sinse.chat.repository;

import com.sinse.chat.domain.ChatParticipant;
import com.sinse.chat.domain.ChatParticipant.ChatParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {
}
