import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { KnowledgeItemDto, KnowledgeType } from '../models/knowledge.model';
import { PageResponse } from '../models/dokument.model';

@Injectable({ providedIn: 'root' })
export class KnowledgeApiService {
  private readonly http = inject(HttpClient);

  list(params: { type?: KnowledgeType; page?: number; size?: number } = {}): Observable<PageResponse<KnowledgeItemDto>> {
    let httpParams = new HttpParams();
    if (params.type) httpParams = httpParams.set('type', params.type);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size) httpParams = httpParams.set('size', params.size.toString());
    return this.http.get<PageResponse<KnowledgeItemDto>>('/api/knowledge', { params: httpParams });
  }

  getById(id: number): Observable<KnowledgeItemDto> {
    return this.http.get<KnowledgeItemDto>(`/api/knowledge/${id}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/knowledge/${id}`);
  }
}
