package com.sinse.chat.controller;

import com.sinse.chat.chat.ChatModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chatapi/moderation")
@RequiredArgsConstructor
public class ChatModerationController {

    private final ChatModerationService chatModerationService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBanStatus(
            @RequestParam("userId") int userId,
            @RequestParam("roomId") int roomId) {
        
        boolean isBanned = chatModerationService.isBanned(roomId, userId);

        log.debug("userId : " + userId + "roomId  : " + roomId);
        log.debug("isBanned Check : " + isBanned);
        
        Map<String, Object> response = Map.of(
            "roomId", roomId,
            "userId", userId,
            "isBanned", isBanned
        );
        
        return ResponseEntity.ok(response);
    }
}
