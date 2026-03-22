package de.wissensdatenbank.retrieval;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.enums.BindingLevel;
import de.wissensdatenbank.enums.KnowledgeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeMatchEngineTest {

    private KnowledgeMatchEngine engine;

    @BeforeEach
    void setUp() {
        engine = new KnowledgeMatchEngine();
    }

    @Test
    void computeScore_keywordMatch() {
        KnowledgeItem item = createItem("Beatmung und Weaning", "Beatmung, Weaning, Intensiv");
        SearchQuery query = new SearchQuery("Beatmung", List.of(), List.of(), List.of("Beatmung"), "t1");

        double score = engine.computeScore(query, item);
        assertTrue(score > 0, "Score sollte positiv sein bei Keyword-Match");
    }

    @Test
    void computeScore_diagnoseMatch() {
        KnowledgeItem item = createItem("Herzinsuffizienz Kodierung", "Herz, Insuffizienz");
        SearchQuery query = new SearchQuery(null, List.of("Herzinsuffizienz"), List.of(), List.of(), "t1");

        double score = engine.computeScore(query, item);
        assertTrue(score > 0, "Score sollte positiv sein bei Diagnose-Match");
    }

    @Test
    void computeScore_noMatch() {
        KnowledgeItem item = createItem("Orthopaedie Knie", "Knie, Arthroskopie");
        SearchQuery query = new SearchQuery("Kardiologie", List.of("Herzinfarkt"), List.of(), List.of(), "t1");

        double score = engine.computeScore(query, item);
        assertEquals(0.0, score, "Score sollte 0 sein bei keinem Match");
    }

    @Test
    void computeMatchReason_containsMatchType() {
        KnowledgeItem item = createItem("Beatmung", "Beatmung");
        SearchQuery query = new SearchQuery("Beatmung", List.of(), List.of(), List.of("Beatmung"), "t1");

        String reason = engine.computeMatchReason(query, item);
        assertTrue(reason.contains("Keyword-Treffer"));
    }

    private KnowledgeItem createItem(String title, String keywords) {
        KnowledgeItem item = new KnowledgeItem();
        item.setTitle(title);
        item.setKeywords(keywords);
        item.setKnowledgeType(KnowledgeType.SEG4);
        item.setBindingLevel(BindingLevel.EMPFEHLUNG);
        item.setTenantId("t1");
        return item;
    }
}
