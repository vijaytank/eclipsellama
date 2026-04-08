package com.eclipsellama.plugin.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import com.eclipsellama.plugin.core.ClientProvider;
import com.eclipsellama.plugin.core.OllamaClient;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * AI-powered code completion for Java editor.
 * Uses async completion with timeout to avoid blocking UI.
 */
@SuppressWarnings("restriction")
public class EclipseLlamaProposalComputer implements IJavaCompletionProposalComputer {

    private static final int COMPLETION_TIMEOUT_MS = 3000;
    private static final int MIN_PREFIX_LENGTH = 3;

    @Override
    public List<ICompletionProposal> computeCompletionProposals(
            ContentAssistInvocationContext context,
            IProgressMonitor monitor) {

        List<ICompletionProposal> proposals = new ArrayList<>();

        if (!(context instanceof JavaContentAssistInvocationContext javaContext)) {
            return proposals;
        }

        try {
            IDocument doc = javaContext.getDocument();
            int offset = javaContext.getInvocationOffset();
            String prefix = extractContext(doc, offset);

            // Skip if prefix too short
            if (prefix.length() < MIN_PREFIX_LENGTH) {
                return proposals;
            }

            // Build prompt
            String prompt = buildCompletionPrompt(prefix);

            // Get model
            String model = EclipseLlamaPreferences.getModel();

            // Async completion with timeout
            CompletableFuture<String> future = ClientProvider.getClient().generateAsync(prompt, model);

            String suggestion = future.get(COMPLETION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (suggestion != null && !suggestion.isEmpty() && !suggestion.equals(prefix)) {
                // Clean up the suggestion
                suggestion = cleanSuggestion(suggestion, prefix);

                if (!suggestion.isEmpty()) {
                    ICompletionProposal proposal = new CompletionProposal(
                            suggestion,
                            offset,
                            0,
                            suggestion.length(),
                            null,
                            "🦙 " + truncate(suggestion, 50),
                            null,
                            "AI-generated completion");
                    proposals.add(proposal);
                }
            }
        } catch (Exception e) {
            // Timeout or error - return empty list silently
        }

        return proposals;
    }

    private String extractContext(IDocument doc, int offset) {
        try {
            // Get current line and previous lines for context
            int line = doc.getLineOfOffset(offset);
            int startLine = Math.max(0, line - 5);
            int startOffset = doc.getLineOffset(startLine);
            return doc.get(startOffset, offset - startOffset);
        } catch (Exception e) {
            return "";
        }
    }

    private String buildCompletionPrompt(String context) {
        return "Complete the following Java code. Only provide the completion, no explanation:\n\n"
                + context;
    }

    private String cleanSuggestion(String suggestion, String prefix) {
        // Remove any repeated prefix
        if (suggestion.startsWith(prefix)) {
            suggestion = suggestion.substring(prefix.length());
        }

        // Take only the first meaningful line
        String[] lines = suggestion.split("\n");
        if (lines.length > 0) {
            return lines[0].trim();
        }

        return suggestion.trim();
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    @Override
    public List<IContextInformation> computeContextInformation(
            ContentAssistInvocationContext context,
            IProgressMonitor monitor) {
        return new ArrayList<>();
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public void sessionStarted() {
        // No-op
    }

    @Override
    public void sessionEnded() {
        // No-op
    }
}
