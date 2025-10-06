
package com.sinse.chat.chat;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final long WINDOW_MS = 5_000; // 5초
    private final int MAX = 8;            // 5초에 8개

    public boolean allow(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) q.pollFirst();
            if (q.size() >= MAX) return false;
            q.addLast(now);
            return true;
        }
    }
}
