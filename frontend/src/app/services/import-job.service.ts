import { Injectable, inject, signal, computed } from '@angular/core';
import { ImportApiService } from './import-api.service';
import { Seg4ImportResult } from '../models/knowledge.model';

export type ImportJobStatus = 'uploading' | 'done' | 'error';

export interface ImportJob {
  id: number;
  fileName: string;
  status: ImportJobStatus;
  result?: Seg4ImportResult;
  errorMessage?: string;
}

let nextId = 1;

/**
 * Globaler Service fuer Hintergrund-Importe.
 * Lebt unabhaengig von der aktuellen Route – Seitenwechsel bricht den Import nicht ab.
 */
@Injectable({ providedIn: 'root' })
export class ImportJobService {
  private readonly importApi = inject(ImportApiService);

  readonly jobs = signal<ImportJob[]>([]);
  readonly activeJobs = computed(() => this.jobs().filter(j => j.status === 'uploading'));
  readonly hasActiveJobs = computed(() => this.activeJobs().length > 0);
  readonly recentDoneJobs = computed(() => this.jobs().filter(j => j.status !== 'uploading'));

  startImport(file: File): void {
    const job: ImportJob = {
      id: nextId++,
      fileName: file.name,
      status: 'uploading',
    };
    this.jobs.update(jobs => [...jobs, job]);

    this.importApi.importSeg4Pdf(file).subscribe({
      next: res => this.updateJob(job.id, { status: 'done', result: res }),
      error: err => this.updateJob(job.id, {
        status: 'error',
        errorMessage: err.error?.message || 'Import fehlgeschlagen',
      }),
    });
  }

  dismissJob(jobId: number): void {
    this.jobs.update(jobs => jobs.filter(j => j.id !== jobId));
  }

  private updateJob(jobId: number, patch: Partial<ImportJob>): void {
    this.jobs.update(jobs =>
      jobs.map(j => j.id === jobId ? { ...j, ...patch } : j)
    );
  }
}
