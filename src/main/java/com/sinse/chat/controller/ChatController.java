
package com.sinse.chat.controller;

import com.sinse.chat.chat.ChatModerationService;
import com.sinse.chat.dto.request.ChatMessageRequest;
import com.sinse.chat.dto.response.ChatMessageResponse;
import com.sinse.chat.security.UserPrincipal;
import com.sinse.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

import static com.sinse.chat.domain.ChatMessage.ContentType.TEXT;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatModerationService moderation;
    private final ChatService chatService;

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

        var result = moderation.moderate(msg.content());
        if (!result.allowed()) throw new MessagingException("CONTENT_BLOCKED");

        var out = new ChatMessageResponse(
                roomId, senderId, nickname, role, result.cleaned(), TEXT, LocalDateTime.now()
        );
        chatService.saveMessage(out);
        return out;
    }

}
