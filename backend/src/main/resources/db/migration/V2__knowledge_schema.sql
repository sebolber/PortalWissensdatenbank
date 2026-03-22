-- ============================================================
-- V2: Knowledge-Management Schema (KnowledgeItem, SEG4, Audit)
-- ============================================================

-- Wissensobjekte (SEG4, Artikel, Leitlinien)
CREATE TABLE wb_knowledge_items (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(64)  NOT NULL,
    title           VARCHAR(500) NOT NULL,
    summary         TEXT,
    knowledge_type  VARCHAR(30)  NOT NULL,
    binding_level   VARCHAR(30)  NOT NULL,
    keywords        VARCHAR(1000),
    valid_from      TIMESTAMP,
    valid_until     TIMESTAMP,
    source_reference VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_ki_tenant         ON wb_knowledge_items (tenant_id);
CREATE INDEX idx_ki_tenant_type    ON wb_knowledge_items (tenant_id, knowledge_type);
CREATE INDEX idx_ki_tenant_binding ON wb_knowledge_items (tenant_id, binding_level);

-- Volltext-Suche (deutsch) auf Wissensobjekten
CREATE INDEX idx_ki_fulltext ON wb_knowledge_items
    USING gin(to_tsvector('german', COALESCE(title,'') || ' ' || COALESCE(summary,'') || ' ' || COALESCE(keywords,'')));

-- Tags-Zuordnung für Wissensobjekte
CREATE TABLE wb_knowledge_item_tags (
    knowledge_item_id BIGINT NOT NULL REFERENCES wb_knowledge_items(id) ON DELETE CASCADE,
    tag_id            BIGINT NOT NULL REFERENCES wb_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (knowledge_item_id, tag_id)
);

-- SEG4-Kodierempfehlungen (geparsete Felder)
CREATE TABLE wb_seg4_recommendations (
    id                      BIGSERIAL PRIMARY KEY,
    knowledge_item_id       BIGINT NOT NULL REFERENCES wb_knowledge_items(id) ON DELETE CASCADE,
    recommendation_number   VARCHAR(50),
    schlagworte             VARCHAR(1000),
    erstellt_am             DATE,
    aktualisiert_am         DATE,
    problem_erlaeuterung    TEXT,
    empfehlung              TEXT,
    entscheidung            TEXT,
    zusatzhinweis           TEXT,
    is_arbitration          BOOLEAN NOT NULL DEFAULT FALSE,
    original_text           TEXT
);

CREATE INDEX idx_seg4_ki     ON wb_seg4_recommendations (knowledge_item_id);
CREATE INDEX idx_seg4_number ON wb_seg4_recommendations (recommendation_number);

-- Unterartikel eines Wissensobjekts
CREATE TABLE wb_knowledge_sub_articles (
    id                BIGSERIAL PRIMARY KEY,
    knowledge_item_id BIGINT       NOT NULL REFERENCES wb_knowledge_items(id) ON DELETE CASCADE,
    heading           VARCHAR(500) NOT NULL,
    content           TEXT,
    order_index       INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_ksa_ki ON wb_knowledge_sub_articles (knowledge_item_id);

-- Audit-Trail für KI-Kodierempfehlungen
CREATE TABLE wb_suggestion_audit_log (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               VARCHAR(64)  NOT NULL,
    user_id                 VARCHAR(255) NOT NULL,
    input_document_text     TEXT,
    used_knowledge_item_ids VARCHAR(500),
    system_prompt           TEXT,
    user_prompt             TEXT,
    llm_response            TEXT,
    llm_model               VARCHAR(255),
    llm_config_id           VARCHAR(255),
    token_count             INT DEFAULT 0,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sal_tenant         ON wb_suggestion_audit_log (tenant_id);
CREATE INDEX idx_sal_tenant_created ON wb_suggestion_audit_log (tenant_id, created_at);
