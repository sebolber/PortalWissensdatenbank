package de.wissensdatenbank.llm;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.entity.Seg4Recommendation;
import de.wissensdatenbank.enums.BindingLevel;
import de.wissensdatenbank.enums.KnowledgeType;
import de.wissensdatenbank.retrieval.KnowledgeCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {

    private PromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new PromptBuilder();
    }

    @Test
    void buildSystemPrompt_containsKeyRules() {
        String prompt = builder.buildSystemPrompt();
        assertTrue(prompt.contains("Kodierexperte"));
        assertTrue(prompt.contains("Keine Halluzination"));
        assertTrue(prompt.contains("lex specialis"));
        assertTrue(prompt.contains("KURZFAZIT"));
        assertTrue(prompt.contains("UNSICHERHEITEN"));
    }

    @Test
    void buildUserPrompt_containsDiagnosenAndMassnahmen() {
        KnowledgeItem item = new KnowledgeItem();
        item.setId(1L);
        item.setTitle("Test");
        item.setKnowledgeType(KnowledgeType.SEG4);
        item.setBindingLevel(BindingLevel.EMPFEHLUNG);
        item.setSeg4Recommendations(new ArrayList<>());
        KnowledgeCandidate c = new KnowledgeCandidate(item);

        String prompt = builder.buildUserPrompt(
                List.of("I50.0 Herzinsuffizienz"),
                List.of("5-377.1 Schrittmacher"),
                "Patient 72J",
                List.of(c)
        );

        assertTrue(prompt.contains("I50.0 Herzinsuffizienz"));
        assertTrue(prompt.contains("5-377.1 Schrittmacher"));
        assertTrue(prompt.contains("Patient 72J"));
        assertTrue(prompt.contains("Quelle 1"));
    }

    @Test
    void buildUserPrompt_includesSeg4Details() {
        KnowledgeItem item = new KnowledgeItem();
        item.setId(1L);
        item.setTitle("SEG4 Test");
        item.setKnowledgeType(KnowledgeType.SEG4);
        item.setBindingLevel(BindingLevel.LEX_SPECIALIS);

        Seg4Recommendation rec = new Seg4Recommendation();
        rec.setRecommendationNumber("042");
        rec.setEmpfehlung("DKR 1001 anwenden");
        rec.setArbitration(true);
        List<Seg4Recommendation> recs = new ArrayList<>();
        recs.add(rec);
        item.setSeg4Recommendations(recs);

        KnowledgeCandidate c = new KnowledgeCandidate(item);

        String prompt = builder.buildUserPrompt(List.of(), List.of(), "Falltext", List.of(c));

        assertTrue(prompt.contains("042"));
        assertTrue(prompt.contains("DKR 1001"));
        assertTrue(prompt.contains("lex specialis"));
    }
}
