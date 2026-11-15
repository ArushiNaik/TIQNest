package com.konate.ithelpdesk.triage.AiLayer;

import java.util.List;

/** Backend-only gateway to Claude. Implementations must never log submissions, images or keys. */
public interface ClaudeClient {

    boolean isConfigured();

    /**
     * Classify (and, when images are given, analyze) an employee submission.
     *
     * @param employeeText untrusted free text, may be empty
     * @param images       verified screenshots / sampled frames, may be empty
     * @throws AiUnavailableException when no usable answer could be obtained
     */
    AiTriageOutput triage(String employeeText, List<ValidatedImage> images);
}
