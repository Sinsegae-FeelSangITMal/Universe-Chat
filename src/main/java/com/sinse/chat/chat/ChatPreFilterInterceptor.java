
package com.sinse.chat.chat;

import com.sinse.chat.domain.ChatModeration;
import com.sinse.chat.repository.ChatModerationRepository;
import com.sinse.chat.security.JwtTokenProvider;
import com.sinse.chat.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ChatPreFilterInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final RateLimiterService rateLimiter;
    private final HtmlSanitizer sanitizer;
    private final ChatModerationService moderationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ChatModerationRepository moderationRepository;

    @Autowired
    @Lazy
    private SimpMessagingTemplate messagingTemplate;

    public ChatPreFilterInterceptor(JwtTokenProvider jwtTokenProvider, RateLimiterService rateLimiter, HtmlSanitizer sanitizer, ChatModerationService moderationService, RedisTemplate<String, String> redisTemplate, ChatModerationRepository moderationRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.rateLimiter = rateLimiter;
        this.sanitizer = sanitizer;
        this.moderationService = moderationService;
        this.redisTemplate = redisTemplate;
        this.moderationRepository = moderationRepository;
    }

    private static final String OFFENSE_KEY_PREFIX = "OFFENSE:";
    private static final String MUTE_KEY_PREFIX = "MUTE:";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand cmd = accessor.getCommand();

        // For every message, rehydrate the user principal from session attributes into the message header.
        // This ensures accessor.getUser() works consistently in downstream components like argument resolvers.
        if (!StompCommand.CONNECT.equals(cmd)) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && accessor.getUser() == null) {
                UserPrincipal principal = (UserPrincipal) sessionAttributes.get("userPrincipal");
                if (principal != null) {
                    accessor.setUser(principal);
                }
            }
        }

        if (StompCommand.CONNECT.equals(cmd)) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
            if (token == null || !jwtTokenProvider.validateToken(token)) {
                throw new MessagingException("INVALID_TOKEN");
            }
            Integer userId  = jwtTokenProvider.getUserId(token);
            String nickname = jwtTokenProvider.getNickname(token);
            String role     = jwtTokenProvider.getRole(token);

            // 1. Create and set the principal on the current message
            UserPrincipal principal = new UserPrincipal(String.valueOf(userId), nickname, role);
            accessor.setUser(principal);

            // 2. Store the principal in the session attributes for later rehydration
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put("userPrincipal", principal);
            }
            
            accessor.setLeaveMutable(true);

            // 3. Return a new message with the updated headers
            return org.springframework.messaging.support.MessageBuilder
                    .createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (StompCommand.SEND.equals(cmd)) {
            // Thanks to the rehydration logic above, accessor.getUser() now works correctly.
            var principal = accessor.getUser();
            if (!(principal instanceof UserPrincipal up)) {
                throw new MessagingException("NOT_AUTHENTICATED");
            }

            String roomId = extractRoomId(accessor.getDestination());
            String userId = up.getName();

            // mute 체크
            if (Boolean.TRUE.equals(redisTemplate.hasKey("MUTE:" + roomId + ":" + userId))) {
                throw new MessagingException("MUTED");
            }

            if (!rateLimiter.allow(userId)) throw new MessagingException("RATE_LIMITED");

            String raw  = new String((byte[]) message.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
            String safe = sanitizer.clean(raw);
            var result  = moderationService.moderate(safe);

            if (!result.allowed()) {
                handleOffense(roomId, userId, up.getNickname());
                throw new MessagingException("BLOCKED_CONTENT");
            }

            // ✅ SEND도 mutable 열고, 정제된 페이로드로 교체
            accessor.setLeaveMutable(true);
            return org.springframework.messaging.support.MessageBuilder
                    .createMessage(result.cleaned().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            accessor.getMessageHeaders());
        }

        return message;
    }

    private String extractRoomId(String dest) {
        if (dest == null) return null;
        int i = dest.lastIndexOf('/');
        return i >= 0 ? dest.substring(i + 1) : null;
    }

    private void handleOffense(String roomId, String userId, String nickname) {
        String offenseKey = OFFENSE_KEY_PREFIX + roomId + ":" + userId;
        Long offenseCount = redisTemplate.opsForValue().increment(offenseKey);

        if (offenseCount == null) offenseCount = 1L;

        if (offenseCount >= 3) {
            // Apply Ban
            log.warn("User {} in room {} has been banned after {} offenses.", nickname, roomId, offenseCount);
            ChatModeration.ChatModerationId banId = new ChatModeration.ChatModerationId(Long.parseLong(roomId), Long.parseLong(userId), ChatModeration.ModerationType.BAN);
            ChatModeration ban = new ChatModeration(banId, "Exceeded offense limit", null, LocalDateTime.now());
            moderationRepository.save(ban);
            messagingTemplate.convertAndSendToUser(userId, "/queue/system", "You have been banned from this chat.");
        } else {
            // Apply Mute
            log.warn("User {} in room {} has been muted for 30 seconds (offense #{})", nickname, roomId, offenseCount);
            String muteKey = MUTE_KEY_PREFIX + roomId + ":" + userId;
            redisTemplate.opsForValue().set(muteKey, "true", 30, TimeUnit.SECONDS);
            // Save mute record to DB as well for tracking
            ChatModeration.ChatModerationId muteId = new ChatModeration.ChatModerationId(Long.parseLong(roomId), Long.parseLong(userId), ChatModeration.ModerationType.MUTE);
            ChatModeration mute = new ChatModeration(muteId, "Inappropriate language", LocalDateTime.now().plusSeconds(30), LocalDateTime.now());
            moderationRepository.save(mute);
            messagingTemplate.convertAndSendToUser(userId, "/queue/system", "Inappropriate language detected. You are muted for 30 seconds.");
        }
    }

    private String getRoomIdFromDestination(String destination) {
        if (destination == null) return null;
        try {
            return destination.substring(destination.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return null;
        }
    }
}
