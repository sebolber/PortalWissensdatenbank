-- ============================================================
-- V7: Erweiterungen für Benutzerhandbuch-/Manual-Wissen
-- ============================================================

-- 1. Softwareprodukte (z.B. a1dlg.exe, a1rw.exe)
CREATE TABLE wb_software_products (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(64)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    executable_name VARCHAR(100),
    publisher       VARCHAR(300),
    description     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    UNIQUE (tenant_id, name)
);

CREATE INDEX idx_wb_sp_tenant ON wb_software_products(tenant_id);

-- 2. Produktversionen (z.B. 152.0)
CREATE TABLE wb_product_versions (
    id              BIGSERIAL    PRIMARY KEY,
    product_id      BIGINT       NOT NULL REFERENCES wb_software_products(id) ON DELETE CASCADE,
    version_label   VARCHAR(100) NOT NULL,
    release_date    DATE,
    change_summary  TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, version_label)
);

CREATE INDEX idx_wb_pv_product ON wb_product_versions(product_id);

-- 3. KnowledgeItem: optionale Zuordnung zu einer Produktversion
ALTER TABLE wb_knowledge_items
    ADD COLUMN product_version_id BIGINT REFERENCES wb_product_versions(id) ON DELETE SET NULL;

CREATE INDEX idx_ki_product_version ON wb_knowledge_items(product_version_id);

-- 4. KnowledgeSubArticle: hierarchisch machen
ALTER TABLE wb_knowledge_sub_articles
    ADD COLUMN parent_id       BIGINT REFERENCES wb_knowledge_sub_articles(id) ON DELETE CASCADE;

ALTER TABLE wb_knowledge_sub_articles
    ADD COLUMN section_number  VARCHAR(50);

ALTER TABLE wb_knowledge_sub_articles
    ADD COLUMN depth           INT NOT NULL DEFAULT 0;

ALTER TABLE wb_knowledge_sub_articles
    ADD COLUMN path            TEXT;

CREATE INDEX idx_ksa_parent  ON wb_knowledge_sub_articles(parent_id);
CREATE INDEX idx_ksa_section ON wb_knowledge_sub_articles(section_number);
CREATE INDEX idx_ksa_path    ON wb_knowledge_sub_articles USING btree(path text_pattern_ops);

-- 5. Querverweise zwischen Wissensartikeln / Abschnitten
CREATE TABLE wb_knowledge_cross_references (
    id                    BIGSERIAL    PRIMARY KEY,
    source_item_id        BIGINT       NOT NULL REFERENCES wb_knowledge_items(id) ON DELETE CASCADE,
    source_sub_article_id BIGINT       REFERENCES wb_knowledge_sub_articles(id) ON DELETE CASCADE,
    target_item_id        BIGINT       NOT NULL REFERENCES wb_knowledge_items(id) ON DELETE CASCADE,
    target_sub_article_id BIGINT       REFERENCES wb_knowledge_sub_articles(id) ON DELETE SET NULL,
    reference_text        VARCHAR(500),
    reference_type        VARCHAR(30)  NOT NULL DEFAULT 'SEE_ALSO'
);

CREATE INDEX idx_kcr_source     ON wb_knowledge_cross_references(source_item_id);
CREATE INDEX idx_kcr_target     ON wb_knowledge_cross_references(target_item_id);
CREATE INDEX idx_kcr_source_sub ON wb_knowledge_cross_references(source_sub_article_id);

-- 6. Volltext-Suche auf SubArticle-Inhalten (deutsch)
CREATE INDEX idx_ksa_fulltext ON wb_knowledge_sub_articles
    USING gin(to_tsvector('german', COALESCE(heading,'') || ' ' || COALESCE(content,'')));
