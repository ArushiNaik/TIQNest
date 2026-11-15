package com.konate.ithelpdesk.triage.AiLayer;

/** An uploaded image whose real format was verified from its bytes. Kept in memory only, never stored. */
public record ValidatedImage(String mediaType, byte[] bytes) {
}
