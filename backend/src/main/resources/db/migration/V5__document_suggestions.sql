-- ============================================================
-- V5: Tabelle fuer Dokument-basierte KI-Kodierempfehlungen
-- ============================================================

CREATE TABLE wb_document_suggestions (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(64)     NOT NULL,
    user_id         VARCHAR(255)    NOT NULL,
    file_name       VARCHAR(500)    NOT NULL,
    file_content_type VARCHAR(255),
    file_data       BYTEA,
    extracted_text  TEXT,
    status          VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    empfehlungen    TEXT,
    llm_model       VARCHAR(255),
    token_count     INT             DEFAULT 0,
    quellen_json    TEXT,
    audit_log_id    BIGINT          REFERENCES wb_suggestion_audit_log(id),
    model_config_id VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX idx_ds_tenant ON wb_document_suggestions(tenant_id);
CREATE INDEX idx_ds_tenant_status ON wb_document_suggestions(tenant_id, status);
CREATE INDEX idx_ds_tenant_created ON wb_document_suggestions(tenant_id, created_at DESC);
