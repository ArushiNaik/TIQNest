package com.konate.ithelpdesk.triage;

import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationCatalog;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher;
import com.konate.ithelpdesk.triage.AiLayer.AiTriageOutput;
import com.konate.ithelpdesk.triage.AiLayer.AiUnavailableException;
import com.konate.ithelpdesk.triage.AiLayer.ClaudeClient;
import com.konate.ithelpdesk.triage.AiLayer.ValidatedImage;
import com.konate.ithelpdesk.triage.BusinessLayer.AiScopeClassifier;
import com.konate.ithelpdesk.triage.BusinessLayer.RuleBasedScopeClassifier;
import com.konate.ithelpdesk.triage.BusinessLayer.ScopeClassification;
import com.konate.ithelpdesk.triage.BusinessLayer.ScopeResult;
import com.konate.ithelpdesk.triage.BusinessLayer.ScopeSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.konate.ithelpdesk.triage.BusinessLayer.ScopeClassification.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** IT-scope classification: deterministic rules, and Claude results passed through server-side validation. */
class ScopeClassificationTest {

    private final RuleBasedScopeClassifier rules =
            new RuleBasedScopeClassifier(new RemediationMatcher(new RemediationCatalog()));

    /** Fake Claude: no network. */
    static final class FakeClaude implements ClaudeClient {
        final Function<String, AiTriageOutput> answer;
        final AtomicInteger calls = new AtomicInteger();

        FakeClaude(Function<String, AiTriageOutput> answer) {
            this.answer = answer;
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public AiTriageOutput triage(String employeeText, List<ValidatedImage> images) {
            calls.incrementAndGet();
            return answer.apply(employeeText);
        }
    }

    static AiTriageOutput ai(String classification) {
        return new AiTriageOutput(classification, 0.9, "reason", "", "", "", "", "", "", "",
                List.of(), List.of(), false);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "My VPN is disconnected                         | IT_SUPPORTED",
            "My printer is offline                          | IT_SUPPORTED",
            "Microsoft Outlook shows error 0x800CCC0E       | IT_SUPPORTED",
            "My laptop won't connect to Wi-Fi               | IT_SUPPORTED",
            "Windows blue screen after update               | IT_SUPPORTED",
            "My company account is locked                   | IT_SUPPORTED",
            "My browser shows an authentication error       | IT_SUPPORTED",
            "My laptop is stolen                            | SECURITY_RISK",
            "I clicked a phishing link                      | SECURITY_RISK",
            "I think my computer has ransomware             | SECURITY_RISK",
            "I think our company account was compromised    | SECURITY_RISK",
            "My hard drive is damaged                       | IT_UNSUPPORTED",
            "My laptop motherboard is dead                  | IT_UNSUPPORTED",
            "Can you make me pizza?                         | NON_IT",
            "Who won the football game?                     | NON_IT",
            "Write me a poem                                | NON_IT",
            "What is the capital of France?                 | NON_IT",
            "Help me plan my birthday party                 | NON_IT",
            "What's the weather tomorrow?                   | NON_IT",
            "How do I fix my office chair's broken leg?     | NON_IT",
            "Can you tell me how to cook pasta?             | NON_IT",
            "My screen is broken                            | UNCERTAIN",
            "Help!                                          | INSUFFICIENT_INFO",
            "It doesn't work.                               | INSUFFICIENT_INFO",
            "VPN is broken; also tell me a joke             | IT_SUPPORTED",
    })
    void rulesClassifyKnownExamples(String text, ScopeClassification expected) {
        assertThat(rules.classify(text).classification()).isEqualTo(expected);
    }

    @Test
    void uncertainAndInsufficientAskAUsefulQuestion() {
        assertThat(rules.classify("My screen is broken").clarifyingQuestion())
                .isEqualTo(RuleBasedScopeClassifier.CLARIFY_SCREEN);
        assertThat(rules.classify("Help!").clarifyingQuestion())
                .contains("device, application or system");
    }

    @Test
    void securityIsItRelatedNeverNonIt() {
        ScopeResult r = rules.classify("I clicked a suspicious link and my computer might be infected.");
        assertThat(r.classification()).isEqualTo(SECURITY_RISK);
        assertThat(r.isITRelated()).isTrue();
    }

    @Test
    void rulesNeverAnswerNonItForUnknownText() {
        assertThat(rules.classify("The quarterly synergy dashboard behaves unexpectedly today").classification())
                .isEqualTo(UNCERTAIN);
    }

    @Test
    void aiResultIsUsedWhenValid() {
        AiScopeClassifier classifier = new AiScopeClassifier(rules, new FakeClaude(t -> ai("NON_IT")));
        ScopeResult r = classifier.classify("Can you order me pizza?");
        assertThat(r.classification()).isEqualTo(NON_IT);
        assertThat(r.source()).isEqualTo(ScopeSource.AI);
    }

    @Test
    void securityRulesOverrideTheAi() {
        FakeClaude claude = new FakeClaude(t -> ai("IT_SUPPORTED"));
        ScopeResult r = new AiScopeClassifier(rules, claude).classify("My laptop was stolen at the airport");
        assertThat(r.classification()).isEqualTo(SECURITY_RISK);
        assertThat(claude.calls.get()).isZero();
    }

    @Test
    void aiSayingNonItForAGenuineItProblemBecomesAClarification() {
        AiScopeClassifier classifier = new AiScopeClassifier(rules, new FakeClaude(t -> ai("NON_IT")));
        ScopeResult r = classifier.classify("VPN is broken; also tell me a joke");
        assertThat(r.classification()).isEqualTo(UNCERTAIN);
        assertThat(r.clarifyingQuestion()).isNotBlank();
    }

    @Test
    void aiFailureFallsBackToRulesWithoutRejectingUnknownProblems() {
        AiScopeClassifier classifier = new AiScopeClassifier(rules, new FakeClaude(t -> {
            throw new AiUnavailableException(AiUnavailableException.Reason.TIMEOUT, "timeout");
        }));
        ScopeResult vpn = classifier.classify("My VPN is disconnected");
        assertThat(vpn.classification()).isEqualTo(IT_SUPPORTED);

        ScopeResult unknown = classifier.classify("The quarterly synergy dashboard behaves unexpectedly today");
        assertThat(unknown.classification()).isEqualTo(UNCERTAIN);
        assertThat(unknown.source()).isEqualTo(ScopeSource.RULES_AI_UNAVAILABLE);
    }

    @Test
    void malformedAiClassificationIsNotTrusted() {
        AiScopeClassifier classifier = new AiScopeClassifier(rules, new FakeClaude(t -> ai("DEFINITELY_FINE")));
        ScopeResult r = classifier.classify("The quarterly synergy dashboard behaves unexpectedly today");
        assertThat(r.source()).isEqualTo(ScopeSource.RULES_AI_UNAVAILABLE);
        assertThat(r.classification()).isEqualTo(UNCERTAIN);

        assertThatThrownBy(() -> AiScopeClassifier.fromAi(ai("pwned")))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void invalidAiCategoryAndConfidenceAreSanitized() {
        ScopeResult r = AiScopeClassifier.fromAi(new AiTriageOutput("IT_SUPPORTED", 7.5, "r", "", "", "",
                "QUANTUM", "", "", "", List.of(), List.of(), false));
        assertThat(r.category()).isNull();
        assertThat(r.confidence()).isEqualTo(1.0);
    }

    @Test
    void gateSkipsTheAiForClearItProblems() {
        FakeClaude claude = new FakeClaude(t -> ai("NON_IT"));
        AiScopeClassifier classifier = new AiScopeClassifier(rules, claude);
        assertThat(classifier.classifyForGate("My printer is offline").classification()).isEqualTo(IT_SUPPORTED);
        assertThat(claude.calls.get()).isZero();
    }
}
