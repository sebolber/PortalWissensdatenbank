import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DocumentSuggestionDto, SuggestionRequest, SuggestionResponse } from '../models/knowledge.model';

export interface LlmModelDto {
  id: string;
  name: string;
  provider: string;
  model: string;
  isActive: boolean;
}

@Injectable({ providedIn: 'root' })
export class ChatApiService {
  private readonly http = inject(HttpClient);

  generateSuggestion(request: SuggestionRequest): Observable<SuggestionResponse> {
    return this.http.post<SuggestionResponse>('/api/suggestions', request);
  }

  listLlmModels(): Observable<LlmModelDto[]> {
    return this.http.get<LlmModelDto[]>('/api/suggestions/llm-models');
  }

  // --- Document Suggestions ---

  uploadDocumentSuggestion(file: File, modelConfigId?: string): Observable<DocumentSuggestionDto> {
    const formData = new FormData();
    formData.append('file', file);
    if (modelConfigId) formData.append('modelConfigId', modelConfigId);
    return this.http.post<DocumentSuggestionDto>('/api/document-suggestions/upload', formData);
  }

  listDocumentSuggestions(): Observable<DocumentSuggestionDto[]> {
    return this.http.get<DocumentSuggestionDto[]>('/api/document-suggestions');
  }

  getDocumentSuggestion(id: number): Observable<DocumentSuggestionDto> {
    return this.http.get<DocumentSuggestionDto>(`/api/document-suggestions/${id}`);
  }

  startDocumentSuggestion(id: number): Observable<void> {
    return this.http.post<void>(`/api/document-suggestions/${id}/start`, {});
  }

  deleteDocumentSuggestion(id: number): Observable<void> {
    return this.http.delete<void>(`/api/document-suggestions/${id}`);
  }

  downloadResultPdf(id: number): Observable<Blob> {
    return this.http.get(`/api/document-suggestions/${id}/pdf`, { responseType: 'blob' });
  }

  downloadAnnotatedPdf(id: number): Observable<Blob> {
    return this.http.get(`/api/document-suggestions/${id}/annotated-pdf`, { responseType: 'blob' });
  }

  createDocumentFromSuggestion(id: number): Observable<any> {
    return this.http.post<any>(`/api/document-suggestions/${id}/create-document`, {});
  }
}
