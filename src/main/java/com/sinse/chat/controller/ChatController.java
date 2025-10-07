
package com.sinse.chat.controller;

import com.sinse.chat.dto.request.ChatMessageRequest;
import com.sinse.chat.dto.response.ChatMessageResponse;
import com.sinse.chat.security.UserPrincipal;
import com.sinse.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static com.sinse.chat.domain.ChatMessage.ContentType.TEXT;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService; // Only dependency needed

    @MessageMapping("/live/{roomId}")
    @SendTo("/topic/public/{roomId}")
    public ChatMessageResponse sendMessage(@DestinationVariable int roomId,
                                           ChatMessageRequest msg,
                                           Principal principal) {
        if (msg == null || msg.content() == null || msg.content().isBlank()) {
            throw new MessagingException("INVALID_PAYLOAD");
        }
        if (!(principal instanceof UserPrincipal up)) {
            throw new MessagingException("NOT_AUTHENTICATED");
        }

        int senderId   = Integer.parseInt(up.getName());
        String nickname = up.getNickname();
        String role     = up.getRoleName();

        // Interceptor handles moderation. Controller just saves and broadcasts.
        var out = new ChatMessageResponse(
                roomId, senderId, nickname, role, msg.content(), TEXT, LocalDateTime.now()
        );
        chatService.saveMessage(out); // Saves to Redis
        return out;
    }

    @GetMapping("/chatapi/rooms/{roomId}/messages")
    public ResponseEntity<List<Object>> getRecentMessages(@PathVariable Long roomId) {
        List<Object> messages = chatService.getRecentMessages(roomId.intValue(), 10);
        return ResponseEntity.ok(messages);
    }
}
