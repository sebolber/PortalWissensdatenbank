package de.wissensdatenbank.llm;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.entity.Seg4Recommendation;
import de.wissensdatenbank.enums.KnowledgeType;
import de.wissensdatenbank.retrieval.KnowledgeCandidate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Baut System- und User-Prompt für die Kodierempfehlung.
 * LLM bekommt NUR strukturierte Daten – kein rohes Dokument.
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            Du bist ein medizinischer Kodierexperte fuer das deutsche DRG-System.

            REGELN:
            - Nutze ausschliesslich die bereitgestellten Quellen.
            - Keine Halluzination: Wenn du etwas nicht weisst, sage es.
            - Keine Generalisierung von lex specialis Faellen.
            - Kennzeichne Unsicherheiten explizit.
            - Antworte immer strukturiert.

            ANTWORTSTRUKTUR:
            1. KURZFAZIT: Eine kurze Zusammenfassung der Empfehlung (2-3 Saetze).
            2. RELEVANTE KODIEREMPFEHLUNGEN: Liste der anwendbaren Kodierempfehlungen mit Nummer.
            3. BEGRUENDUNG: Detaillierte Begruendung, warum diese Empfehlungen zutreffen.
            4. QUELLEN: Exakte Referenz auf die verwendeten Wissensobjekte (Nummer, Titel).
            5. VERBINDLICHKEIT: Angabe ob VERBINDLICH, EMPFEHLUNG oder LEX_SPECIALIS.
            6. UNSICHERHEITEN: Offene Punkte oder Einschraenkungen der Empfehlung.
            """;

    /**
     * Gibt den festen Systemprompt zurück.
     */
    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * Baut den User-Prompt aus extrahierten Fakten und gerankten Wissensobjekten.
     *
     * @param diagnosen   extrahierte Diagnosen aus dem Behandlungsdokument
     * @param massnahmen  extrahierte Maßnahmen
     * @param fakten      relevante Textstellen aus dem Dokument
     * @param candidates  Top-N gerankte Wissensobjekte
     * @return strukturierter User-Prompt
     */
    public String buildUserPrompt(List<String> diagnosen, List<String> massnahmen,
                                   String fakten, List<KnowledgeCandidate> candidates) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== FALLBESCHREIBUNG ===\n\n");

        if (diagnosen != null && !diagnosen.isEmpty()) {
            sb.append("DIAGNOSEN:\n");
            diagnosen.forEach(d -> sb.append("- ").append(d).append('\n'));
            sb.append('\n');
        }

        if (massnahmen != null && !massnahmen.isEmpty()) {
            sb.append("MASSNAHMEN/PROZEDUREN:\n");
            massnahmen.forEach(m -> sb.append("- ").append(m).append('\n'));
            sb.append('\n');
        }

        if (fakten != null && !fakten.isBlank()) {
            sb.append("RELEVANTE TEXTSTELLEN:\n");
            sb.append(fakten).append("\n\n");
        }

        sb.append("=== VERFUEGBARE WISSENSOBJEKTE ===\n\n");

        for (int i = 0; i < candidates.size(); i++) {
            KnowledgeCandidate c = candidates.get(i);
            KnowledgeItem item = c.getItem();

            sb.append("--- Quelle ").append(i + 1).append(" ---\n");
            sb.append("Titel: ").append(item.getTitle()).append('\n');
            sb.append("Typ: ").append(item.getKnowledgeType()).append('\n');
            sb.append("Verbindlichkeit: ").append(item.getBindingLevel()).append('\n');

            if (item.getKeywords() != null) {
                sb.append("Schlagworte: ").append(item.getKeywords()).append('\n');
            }

            // Nur die relevanten (per Suche getroffenen) SEG4-Empfehlungen einbeziehen
            List<Seg4Recommendation> recsToShow = c.getMatchedRecommendations();
            if (recsToShow.isEmpty() && item.getKnowledgeType() != KnowledgeType.SEG4) {
                // Fuer Nicht-SEG4-Items: alle Empfehlungen zeigen (normalerweise 0)
                recsToShow = item.getSeg4Recommendations();
            }
            appendSeg4Recommendations(sb, recsToShow);

            if (item.getSummary() != null) {
                sb.append("Zusammenfassung: ").append(item.getSummary()).append('\n');
            }

            sb.append('\n');
        }

        sb.append("=== AUFGABE ===\n");
        sb.append("Erstelle eine strukturierte Kodierempfehlung basierend auf den obigen Quellen.\n");

        return sb.toString();
    }

    private void appendSeg4Recommendations(StringBuilder sb, List<Seg4Recommendation> recs) {
        if (recs == null || recs.isEmpty()) return;

        for (Seg4Recommendation rec : recs) {
            sb.append("\n  Kodierempfehlung Nr. ").append(rec.getRecommendationNumber()).append(":\n");

            if (rec.getProblemErlaeuterung() != null) {
                sb.append("    Problem: ").append(rec.getProblemErlaeuterung()).append('\n');
            }
            if (rec.getEmpfehlung() != null) {
                sb.append("    Empfehlung: ").append(rec.getEmpfehlung()).append('\n');
            }
            if (rec.getEntscheidung() != null) {
                sb.append("    Entscheidung: ").append(rec.getEntscheidung()).append('\n');
            }
            if (rec.isArbitration()) {
                sb.append("    HINWEIS: Schlichtungsentscheidung (lex specialis)\n");
            }
        }
    }
}
