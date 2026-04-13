import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  KnowledgeItemDto,
  KnowledgeItemCreateRequest,
  KnowledgeSubArticleTreeDto,
  KnowledgeType,
  SoftwareProductDto,
  ProductVersionDto
} from '../models/knowledge.model';
import { PageResponse } from '../models/dokument.model';

@Injectable({ providedIn: 'root' })
export class KnowledgeApiService {
  private readonly http = inject(HttpClient);

  // --- KnowledgeItem CRUD ---

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

  create(request: KnowledgeItemCreateRequest): Observable<KnowledgeItemDto> {
    return this.http.post<KnowledgeItemDto>('/api/knowledge', request);
  }

  update(id: number, request: KnowledgeItemCreateRequest): Observable<KnowledgeItemDto> {
    return this.http.put<KnowledgeItemDto>(`/api/knowledge/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/knowledge/${id}`);
  }

  // --- SubArticle-Baum ---

  getSubArticleTree(knowledgeItemId: number): Observable<KnowledgeSubArticleTreeDto[]> {
    return this.http.get<KnowledgeSubArticleTreeDto[]>(`/api/knowledge/${knowledgeItemId}/sections`);
  }

  // --- Softwareprodukte ---

  listProducts(): Observable<SoftwareProductDto[]> {
    return this.http.get<SoftwareProductDto[]>('/api/software-products');
  }

  createProduct(request: Partial<SoftwareProductDto>): Observable<SoftwareProductDto> {
    return this.http.post<SoftwareProductDto>('/api/software-products', request);
  }

  listVersions(productId: number): Observable<ProductVersionDto[]> {
    return this.http.get<ProductVersionDto[]>(`/api/software-products/${productId}/versions`);
  }

  createVersion(productId: number, request: Partial<ProductVersionDto>): Observable<ProductVersionDto> {
    return this.http.post<ProductVersionDto>(`/api/software-products/${productId}/versions`, request);
  }
}
