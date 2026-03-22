package de.wissensdatenbank.retrieval;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.enums.BindingLevel;
import de.wissensdatenbank.enums.KnowledgeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeRankingServiceTest {

    private KnowledgeRankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new KnowledgeRankingService();
    }

    @Test
    void rank_returnsTopFive() {
        SearchQuery query = new SearchQuery("test", List.of(), List.of(), List.of(), "t1");
        List<KnowledgeCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            KnowledgeItem item = new KnowledgeItem();
            item.setId((long) i);
            item.setTenantId("t1");
            item.setBindingLevel(BindingLevel.EMPFEHLUNG);
            item.setKnowledgeType(KnowledgeType.ARTICLE);
            KnowledgeCandidate c = new KnowledgeCandidate(item);
            c.setScore(i * 1.0);
            candidates.add(c);
        }

        List<KnowledgeCandidate> ranked = rankingService.rank(candidates, query);

        assertTrue(ranked.size() <= 5);
        assertTrue(ranked.get(0).getScore() >= ranked.get(1).getScore());
    }

    @Test
    void rank_filtersZeroScore() {
        SearchQuery query = new SearchQuery("test", List.of(), List.of(), List.of(), "t1");
        KnowledgeItem item = new KnowledgeItem();
        item.setId(1L);
        item.setBindingLevel(BindingLevel.INFORMATIV);
        item.setKnowledgeType(KnowledgeType.ARTICLE);
        KnowledgeCandidate c = new KnowledgeCandidate(item);
        c.setScore(0.0);

        List<KnowledgeCandidate> ranked = rankingService.rank(List.of(c), query);

        // Score 0 + INFORMATIV bonus 0.5 > 0, so it should be included
        assertEquals(1, ranked.size());
    }

    @Test
    void rank_verbindlichScoresHigher() {
        SearchQuery query = new SearchQuery("test", List.of(), List.of(), List.of(), "t1");

        KnowledgeItem itemV = new KnowledgeItem();
        itemV.setId(1L);
        itemV.setBindingLevel(BindingLevel.VERBINDLICH);
        itemV.setKnowledgeType(KnowledgeType.SEG4);
        KnowledgeCandidate cV = new KnowledgeCandidate(itemV);
        cV.setScore(3.0);

        KnowledgeItem itemE = new KnowledgeItem();
        itemE.setId(2L);
        itemE.setBindingLevel(BindingLevel.EMPFEHLUNG);
        itemE.setKnowledgeType(KnowledgeType.SEG4);
        KnowledgeCandidate cE = new KnowledgeCandidate(itemE);
        cE.setScore(3.0);

        List<KnowledgeCandidate> ranked = rankingService.rank(List.of(cE, cV), query);

        assertEquals(itemV.getId(), ranked.get(0).getItem().getId());
    }
}
