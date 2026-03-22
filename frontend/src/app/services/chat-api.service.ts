import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SuggestionRequest, SuggestionResponse } from '../models/knowledge.model';

@Injectable({ providedIn: 'root' })
export class ChatApiService {
  private readonly http = inject(HttpClient);

  generateSuggestion(request: SuggestionRequest): Observable<SuggestionResponse> {
    return this.http.post<SuggestionResponse>('/api/suggestions', request);
  }
}
