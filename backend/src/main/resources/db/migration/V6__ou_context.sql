-- Optionaler Organisationseinheiten-Kontext fuer Wissensdatenbank-Inhalte

ALTER TABLE wb_documents ADD COLUMN organization_unit_id VARCHAR(36);
ALTER TABLE wb_categories ADD COLUMN organization_unit_id VARCHAR(36);

CREATE INDEX idx_wb_documents_ou ON wb_documents(organization_unit_id);
CREATE INDEX idx_wb_categories_ou ON wb_categories(organization_unit_id);
