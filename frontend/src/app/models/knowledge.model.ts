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
  modelConfigId?: string | null;
}

export interface UsedSource {
  id: number;
  title: string;
  bindingLevel: string;
  matchReason: string;
}

export interface SuggestionResponse {
  empfehlungen: string[];
  llmModel: string;
  tokenCount: number;
  quellen: UsedSource[];
  auditLogId: number;
}

export interface DocumentSuggestionDto {
  id: number;
  fileName: string;
  fileContentType: string | null;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'ERROR';
  errorMessage: string | null;
  empfehlungen: string[];
  llmModel: string | null;
  tokenCount: number;
  quellen: UsedSource[];
  auditLogId: number | null;
  modelConfigId: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface Seg4ImportResult {
  id: number;
  title: string;
  recommendationCount: number;
  message: string;
}
