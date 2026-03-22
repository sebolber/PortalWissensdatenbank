-- =====================================================
-- Wissensdatenbank Schema
-- Prefix: wb_ (um Konflikte mit PortalCore zu vermeiden)
-- =====================================================

-- Kategorien
CREATE TABLE IF NOT EXISTS wb_categories (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    parent_id       VARCHAR(36)  REFERENCES wb_categories(id) ON DELETE SET NULL,
    order_index     INT          DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    UNIQUE (tenant_id, name, parent_id)
);
CREATE INDEX IF NOT EXISTS idx_wb_categories_tenant ON wb_categories(tenant_id);

-- Tags
CREATE TABLE IF NOT EXISTS wb_tags (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS idx_wb_tags_tenant ON wb_tags(tenant_id);

-- Dokumente
CREATE TABLE IF NOT EXISTS wb_documents (
    id                      VARCHAR(36)  PRIMARY KEY,
    tenant_id               VARCHAR(36)  NOT NULL,
    title                   VARCHAR(500) NOT NULL,
    content                 TEXT         NOT NULL,
    summary                 VARCHAR(2000),
    status                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    category_id             VARCHAR(36)  REFERENCES wb_categories(id) ON DELETE SET NULL,
    created_by              VARCHAR(36)  NOT NULL,
    updated_by              VARCHAR(36),
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    version                 INT          NOT NULL DEFAULT 1,
    is_public_within_tenant BOOLEAN      NOT NULL DEFAULT TRUE,
    view_count              INT          NOT NULL DEFAULT 0,
    rating_sum              DOUBLE PRECISION NOT NULL DEFAULT 0,
    rating_count            INT          NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_wb_documents_tenant ON wb_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_wb_documents_status ON wb_documents(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_wb_documents_category ON wb_documents(category_id);
CREATE INDEX IF NOT EXISTS idx_wb_documents_created ON wb_documents(tenant_id, created_at DESC);

-- Volltextsuche-Index
CREATE INDEX IF NOT EXISTS idx_wb_documents_fulltext
    ON wb_documents USING gin(to_tsvector('german', coalesce(title,'') || ' ' || coalesce(content,'') || ' ' || coalesce(summary,'')));

-- Dokument-Tags (Many-to-Many)
CREATE TABLE IF NOT EXISTS wb_document_tags (
    document_id     VARCHAR(36)  NOT NULL REFERENCES wb_documents(id) ON DELETE CASCADE,
    tag_id          VARCHAR(36)  NOT NULL REFERENCES wb_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, tag_id)
);

-- Dokumentversionen
CREATE TABLE IF NOT EXISTS wb_document_versions (
    id              VARCHAR(36)  PRIMARY KEY,
    document_id     VARCHAR(36)  NOT NULL REFERENCES wb_documents(id) ON DELETE CASCADE,
    version         INT          NOT NULL,
    title           VARCHAR(500) NOT NULL,
    content         TEXT         NOT NULL,
    summary         VARCHAR(2000),
    changed_by      VARCHAR(36)  NOT NULL,
    changed_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    change_note     VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_wb_versions_document ON wb_document_versions(document_id, version DESC);

-- Feedback
CREATE TABLE IF NOT EXISTS wb_feedback (
    id              VARCHAR(36)  PRIMARY KEY,
    document_id     VARCHAR(36)  NOT NULL REFERENCES wb_documents(id) ON DELETE CASCADE,
    user_id         VARCHAR(36)  NOT NULL,
    tenant_id       VARCHAR(36)  NOT NULL,
    rating          INT          NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_wb_feedback_document ON wb_feedback(document_id);
