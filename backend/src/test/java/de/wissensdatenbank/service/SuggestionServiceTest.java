package de.wissensdatenbank.service;

import de.wissensdatenbank.audit.AuditService;
import de.wissensdatenbank.dto.SuggestionRequest;
import de.wissensdatenbank.dto.SuggestionResponse;
import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.entity.SuggestionAuditLog;
import de.wissensdatenbank.enums.BindingLevel;
import de.wissensdatenbank.enums.KnowledgeType;
import de.wissensdatenbank.llm.*;
import de.wissensdatenbank.retrieval.KnowledgeCandidate;
import de.wissensdatenbank.retrieval.KnowledgeSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {

    @Mock private KnowledgeSearchService searchService;
    @Mock private PromptBuilder promptBuilder;
    @Mock private LlmClient llmClient;
    @Mock private AuditService auditService;

    @InjectMocks
    private SuggestionService suggestionService;

    @Test
    void generateSuggestion_orchestratesFullPipeline() {
        // given
        KnowledgeItem item = new KnowledgeItem();
        item.setId(1L);
        item.setTitle("SEG4 Test");
        item.setBindingLevel(BindingLevel.EMPFEHLUNG);
        item.setKnowledgeType(KnowledgeType.SEG4);

        KnowledgeCandidate candidate = new KnowledgeCandidate(item);
        candidate.setMatchReason("Keyword-Treffer");

        when(searchService.search(any())).thenReturn(List.of(candidate));
        when(promptBuilder.buildSystemPrompt()).thenReturn("system");
        when(promptBuilder.buildUserPrompt(any(), any(), any(), any())).thenReturn("user");
        when(llmClient.chat(any())).thenReturn(new LlmResponse("Empfehlung Text", "gpt-4", "cfg1", 500));

        SuggestionAuditLog auditLog = new SuggestionAuditLog();
        auditLog.setId(99L);
        when(auditService.log(any(), any(), any(), any(), any(), any(), any())).thenReturn(auditLog);

        SuggestionRequest request = new SuggestionRequest("Patient Text", List.of("I50.0"), List.of("5-377.1"), null);

        // when
        SuggestionResponse response = suggestionService.generateSuggestion("t1", "u1", "jwt", request);

        // then
        assertEquals("Empfehlung Text", response.empfehlung());
        assertEquals("gpt-4", response.llmModel());
        assertEquals(500, response.tokenCount());
        assertEquals(99L, response.auditLogId());
        assertEquals(1, response.quellen().size());
        assertEquals("SEG4 Test", response.quellen().get(0).title());

        verify(searchService).search(any());
        verify(llmClient).chat(any());
        verify(auditService).log(any(), any(), any(), any(), any(), any(), any());
    }
}
