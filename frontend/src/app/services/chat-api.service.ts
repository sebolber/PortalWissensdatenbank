import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SuggestionRequest, SuggestionResponse } from '../models/knowledge.model';

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
}
