package com.konate.ithelpdesk.triage.AiLayer;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.beta.AnthropicBeta;
import com.anthropic.models.beta.messages.BetaBase64ImageSource;
import com.anthropic.models.beta.messages.BetaContentBlockParam;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.anthropic.models.beta.messages.StructuredMessage;
import com.anthropic.models.beta.messages.StructuredMessageCreateParams;
import com.anthropic.models.beta.messages.StructuredTextBlock;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationCatalog;
import com.konate.ithelpdesk.triage.AiLayer.AiUnavailableException.Reason;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Claude via the official Anthropic Java SDK (beta Messages endpoint for structured outputs plus
 * server-side refusal fallbacks). The API key is read from ANTHROPIC_API_KEY on the server only.
 * Only metadata (image count, latency, outcome) is logged, never submissions, images or keys.
 */
@Slf4j
@Component
public class AnthropicClaudeClient implements ClaudeClient {

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;
    private final String systemPrompt;

    public AnthropicClaudeClient(
            RemediationCatalog catalog,
            @Value("${tiqnest.ai.api-key:}") String apiKey,
            @Value("${tiqnest.ai.model:claude-opus-5-5}") String model,
            @Value("${tiqnest.ai.timeout-seconds:45}") long timeoutSeconds,
            @Value("${tiqnest.ai.max-retries:2}") int maxRetries,
            @Value("${tiqnest.ai.max-tokens:8000}") long maxTokens) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.systemPrompt = TriagePrompts.system(catalog.all().stream()
                .map(r -> new TriagePrompts.CatalogEntry(r.id(), r.title()))
                .toList());
        this.client = apiKey == null || apiKey.isBlank() ? null : AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(maxRetries)
                .build();
        log.info("TiqVision AI: {} (model {})", client == null ? "not configured" : "configured", model);
    }

    @Override
    public boolean isConfigured() {
        return client != null;
    }

    @Override
    public AiTriageOutput triage(String employeeText, List<ValidatedImage> images) {
        if (client == null) {
            throw new AiUnavailableException(Reason.NOT_CONFIGURED, "AI analysis is not configured");
        }
        List<BetaContentBlockParam> blocks = new ArrayList<>();
        for (ValidatedImage image : images) {
            blocks.add(BetaContentBlockParam.ofImage(BetaBase64ImageSource.builder()
                    .mediaType(BetaBase64ImageSource.MediaType.of(image.mediaType()))
                    .data(Base64.getEncoder().encodeToString(image.bytes()))
                    .build()));
        }
        blocks.add(BetaContentBlockParam.ofText(TriagePrompts.userText(employeeText, images.size())));

        StructuredMessageCreateParams<AiTriageOutput> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(systemPrompt)
                .addBeta(AnthropicBeta.SERVER_SIDE_FALLBACK_2026_07_01)
                .fallbacksDefault()
                .outputConfig(AiTriageOutput.class,
                        images.isEmpty() ? BetaOutputConfig.Effort.LOW : BetaOutputConfig.Effort.MEDIUM,
                        JsonSchemaLocalValidation.YES)
                .addUserMessageOfContentBlockParams(blocks)
                .build();

        long start = System.nanoTime();
        try {
            StructuredMessage<AiTriageOutput> message = client.beta().messages().create(params);
            BetaStopReason stop = message.stopReason().orElse(null);
            if (BetaStopReason.REFUSAL.equals(stop)) {
                throw new AiUnavailableException(Reason.REFUSED, "The AI declined to analyze this submission");
            }
            if (BetaStopReason.MAX_TOKENS.equals(stop)) {
                throw new AiUnavailableException(Reason.MALFORMED_OUTPUT, "The AI response was cut off");
            }
            AiTriageOutput output = message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(StructuredTextBlock::text)
                    .findFirst()
                    .orElseThrow(() -> new AiUnavailableException(Reason.MALFORMED_OUTPUT,
                            "The AI returned no structured result"));
            log.info("TiqVision AI: {} image(s) classified as {} in {} ms", images.size(),
                    output.classification(), (System.nanoTime() - start) / 1_000_000);
            return output;
        } catch (AiUnavailableException e) {
            throw e;
        } catch (UnauthorizedException | PermissionDeniedException e) {
            throw fail(Reason.AUTHENTICATION, "The AI provider rejected the server's credentials", e);
        } catch (RateLimitException e) {
            throw fail(Reason.RATE_LIMITED, "The AI provider is rate limiting requests", e);
        } catch (AnthropicServiceException e) {
            throw fail(Reason.PROVIDER_ERROR, "The AI provider returned an error (" + e.statusCode() + ")", e);
        } catch (AnthropicIoException e) {
            throw fail(Reason.TIMEOUT, "The AI provider did not respond in time", e);
        } catch (AnthropicInvalidDataException e) {
            throw fail(Reason.MALFORMED_OUTPUT, "The AI returned an unreadable result", e);
        } catch (RuntimeException e) {
            throw fail(Reason.PROVIDER_ERROR, "Unexpected AI error", e);
        }
    }

    private static AiUnavailableException fail(Reason reason, String message, Exception cause) {
        // Log the class only: provider messages can echo request content.
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        log.warn("TiqVision AI unavailable: {} ({}, root cause {})", reason,
                cause.getClass().getSimpleName(), root.getClass().getSimpleName());
        return new AiUnavailableException(reason, message, cause);
    }

    @PreDestroy
    void close() {
        if (client != null) client.close();
    }
}
