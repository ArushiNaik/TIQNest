package com.konate.ithelpdesk.triage.BusinessLayer;

import com.konate.ithelpdesk.config.CodedApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory sliding-window limit on AI requests per user, to bound cost and abuse. */
@Component
public class TriageRateLimiter {

    private final int maxPerWindow;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Deque<Long>> calls = new ConcurrentHashMap<>();

    public TriageRateLimiter(@Value("${tiqnest.triage.max-requests-per-minute:12}") int maxPerWindow) {
        this.maxPerWindow = maxPerWindow;
        this.windowMillis = 60_000;
        this.clock = Clock.systemUTC();
    }

    public void check(String userKey) {
        long now = clock.millis();
        Deque<Long> q = calls.computeIfAbsent(userKey.toLowerCase(Locale.ROOT), k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMillis) q.pollFirst();
            if (q.size() >= maxPerWindow) {
                throw new CodedApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                        "Too many analysis requests. Please wait a minute and try again.");
            }
            q.addLast(now);
        }
    }
}
