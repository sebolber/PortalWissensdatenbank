import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Seg4ImportResult } from '../models/knowledge.model';

@Injectable({ providedIn: 'root' })
export class ImportApiService {
  private readonly http = inject(HttpClient);

  importSeg4Pdf(file: File): Observable<Seg4ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Seg4ImportResult>('/api/seg4/import', formData);
  }
}
