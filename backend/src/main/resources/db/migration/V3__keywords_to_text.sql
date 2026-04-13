-- V3: keywords-Spalte von VARCHAR(1000) auf TEXT erweitern,
-- damit SEG4-Importe mit vielen Schlagworten nicht fehlschlagen.
ALTER TABLE wb_knowledge_items ALTER COLUMN keywords TYPE TEXT;
