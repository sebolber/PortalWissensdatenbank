export type KnowledgeType = 'SEG4' | 'ARTICLE' | 'GUIDELINE';
export type BindingLevel = 'VERBINDLICH' | 'EMPFEHLUNG' | 'LEX_SPECIALIS' | 'INFORMATIV';

export interface KnowledgeItemDto {
  id: number;
  title: string;
  summary: string | null;
  knowledgeType: KnowledgeType;
  bindingLevel: BindingLevel;
  keywords: string | null;
  validFrom: string | null;
  validUntil: string | null;
  sourceReference: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  tags: string[];
  seg4RecommendationCount: number;
}

export interface SuggestionRequest {
  dokumentText: string;
  diagnosen: string[];
  massnahmen: string[];
}

export interface UsedSource {
  id: number;
  title: string;
  bindingLevel: string;
  matchReason: string;
}

export interface SuggestionResponse {
  empfehlung: string;
  llmModel: string;
  tokenCount: number;
  quellen: UsedSource[];
  auditLogId: number;
}

export interface Seg4ImportResult {
  id: number;
  title: string;
  recommendationCount: number;
  message: string;
}
