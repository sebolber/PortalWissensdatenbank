import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DokumentDto, PageResponse, StatistikDto, DokumentVersionDto, CategoryDto, TagDto } from '../models/dokument.model';

@Injectable({ providedIn: 'root' })
export class DokumentService {
  private readonly http = inject(HttpClient);

  list(params: {
    status?: string;
    categoryId?: string;
    q?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  } = {}): Observable<PageResponse<DokumentDto>> {
    let httpParams = new HttpParams();
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.categoryId) httpParams = httpParams.set('categoryId', params.categoryId);
    if (params.q) httpParams = httpParams.set('q', params.q);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size) httpParams = httpParams.set('size', params.size.toString());
    if (params.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);
    if (params.sortDir) httpParams = httpParams.set('sortDir', params.sortDir);
    return this.http.get<PageResponse<DokumentDto>>('/api/dokumente', { params: httpParams });
  }

  getById(id: string): Observable<DokumentDto> {
    return this.http.get<DokumentDto>(`/api/dokumente/${id}`);
  }

  create(data: { title: string; content: string; summary?: string; categoryId?: string; tagIds?: string[]; publicWithinTenant?: boolean }): Observable<DokumentDto> {
    return this.http.post<DokumentDto>('/api/dokumente', data);
  }

  update(id: string, data: { title: string; content: string; summary?: string; categoryId?: string; tagIds?: string[]; publicWithinTenant?: boolean; changeNote?: string }): Observable<DokumentDto> {
    return this.http.put<DokumentDto>(`/api/dokumente/${id}`, data);
  }

  publish(id: string): Observable<DokumentDto> {
    return this.http.put<DokumentDto>(`/api/dokumente/${id}/publish`, {});
  }

  archive(id: string): Observable<DokumentDto> {
    return this.http.put<DokumentDto>(`/api/dokumente/${id}/archive`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/dokumente/${id}`);
  }

  getVersions(id: string): Observable<DokumentVersionDto[]> {
    return this.http.get<DokumentVersionDto[]>(`/api/dokumente/${id}/versionen`);
  }

  submitFeedback(id: string, rating: number, comment?: string): Observable<void> {
    return this.http.post<void>(`/api/dokumente/${id}/feedback`, { rating, comment });
  }

  getNewest(limit: number = 5): Observable<DokumentDto[]> {
    return this.http.get<DokumentDto[]>(`/api/dokumente/neueste?limit=${limit}`);
  }

  getPopular(limit: number = 5): Observable<DokumentDto[]> {
    return this.http.get<DokumentDto[]>(`/api/dokumente/beliebt?limit=${limit}`);
  }

  getStatistik(): Observable<StatistikDto> {
    return this.http.get<StatistikDto>('/api/dokumente/statistik');
  }

  getCategories(): Observable<CategoryDto[]> {
    return this.http.get<CategoryDto[]>('/api/kategorien');
  }

  createCategory(name: string, description?: string, parentId?: string): Observable<CategoryDto> {
    return this.http.post<CategoryDto>('/api/kategorien', { name, description, parentId });
  }

  updateCategory(id: string, name: string, description?: string): Observable<CategoryDto> {
    return this.http.put<CategoryDto>(`/api/kategorien/${id}`, { name, description });
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`/api/kategorien/${id}`);
  }

  getTags(): Observable<TagDto[]> {
    return this.http.get<TagDto[]>('/api/tags');
  }

  createTag(name: string): Observable<TagDto> {
    return this.http.post<TagDto>('/api/tags', { name });
  }

  deleteTag(id: string): Observable<void> {
    return this.http.delete<void>(`/api/tags/${id}`);
  }
}
