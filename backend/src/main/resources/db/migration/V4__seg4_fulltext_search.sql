-- ============================================================
-- V4: Volltext-Index auf SEG4-Empfehlungen fuer Retrieval
-- ============================================================

-- Volltextindex auf die inhaltlichen Felder der SEG4-Empfehlungen
CREATE INDEX idx_seg4_fulltext ON wb_seg4_recommendations
    USING gin(to_tsvector('german',
        COALESCE(schlagworte,'') || ' ' ||
        COALESCE(problem_erlaeuterung,'') || ' ' ||
        COALESCE(empfehlung,'') || ' ' ||
        COALESCE(entscheidung,'') || ' ' ||
        COALESCE(zusatzhinweis,'')
    ));
