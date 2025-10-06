
package com.sinse.chat.listener;

import com.sinse.chat.service.ChatService;
import com.sinse.chat.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompSubscriptionListener implements ApplicationListener<SessionSubscribeEvent> {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final Pattern chatRoomPattern = Pattern.compile("/topic/public/(\\d+)");

    @Override
    public void onApplicationEvent(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();

        if (destination == null) return;

        Matcher matcher = chatRoomPattern.matcher(destination);
        if (matcher.matches()) {
            int roomId = Integer.parseInt(matcher.group(1));
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

            if (sessionAttributes == null || !sessionAttributes.containsKey("userPrincipal")) {
                log.warn("Cannot send chat history to non-authenticated user (principal not found in session).");
                return;
            }

            UserPrincipal principal = (UserPrincipal) sessionAttributes.get("userPrincipal");
            log.info("User {} subscribed to room {}. Sending chat history.", principal.getName(), roomId);

            // Get recent messages from Redis
            List<Object> recentMessages = chatService.getRecentMessages(roomId, 10);

            // Send messages directly to the user
            messagingTemplate.convertAndSendToUser(
                principal.getName(), // Send to the specific user ID
                "/queue/history",
                recentMessages
            );
        }
    }
}
