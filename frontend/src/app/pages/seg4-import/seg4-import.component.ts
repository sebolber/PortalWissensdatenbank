import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ImportApiService } from '../../services/import-api.service';
import { Seg4ImportResult } from '../../models/knowledge.model';

@Component({
  selector: 'app-seg4-import',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page-header">
      <h1>SEG4 Import</h1>
      <a routerLink="/wissen" class="btn btn-secondary">Zurueck</a>
    </div>

    <div class="card">
      <h3>SEG4-PDF hochladen</h3>
      <p>Laden Sie ein SEG4-PDF mit Kodierempfehlungen hoch. Die Empfehlungen werden automatisch erkannt und gespeichert.</p>

      <div class="upload-area" (dragover)="$event.preventDefault()" (drop)="onDrop($event)">
        <input type="file" accept=".pdf" (change)="onFileSelect($event)" #fileInput style="display:none">
        <button class="btn btn-primary" (click)="fileInput.click()" [disabled]="uploading()">PDF auswaehlen</button>
        <p *ngIf="selectedFile()" style="margin-top:0.5rem">{{ selectedFile()!.name }}</p>
      </div>

      <button *ngIf="selectedFile() && !uploading()" class="btn btn-primary" style="margin-top:1rem" (click)="upload()">Importieren</button>
      <div *ngIf="uploading()" style="margin-top:1rem">Import laeuft...</div>

      <div *ngIf="error()" class="error-msg" style="margin-top:1rem">{{ error() }}</div>

      <div *ngIf="result()" class="card success-card" style="margin-top:1rem">
        <h3>Import erfolgreich</h3>
        <p>{{ result()!.message }}</p>
        <p><strong>{{ result()!.recommendationCount }}</strong> Kodierempfehlungen erkannt.</p>
        <a [routerLink]="'/wissen/' + result()!.id" class="btn btn-primary">Zum Wissensobjekt</a>
      </div>
    </div>
  `,
  styles: [`
    .upload-area { border:2px dashed #d6d3d1; border-radius:8px; padding:2rem; text-align:center; }
    .error-msg { color:#dc2626; padding:0.75rem; background:#fef2f2; border-radius:6px; }
    .success-card { background:#f0fdf4; border:1px solid #86efac; }
  `]
})
export class Seg4ImportComponent {
  private readonly importApi = inject(ImportApiService);
  private readonly router = inject(Router);

  readonly selectedFile = signal<File | null>(null);
  readonly uploading = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<Seg4ImportResult | null>(null);

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) this.selectedFile.set(input.files[0]);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer?.files.length) this.selectedFile.set(event.dataTransfer.files[0]);
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.uploading.set(true);
    this.error.set(null);
    this.result.set(null);
    this.importApi.importSeg4Pdf(file).subscribe({
      next: res => { this.result.set(res); this.uploading.set(false); },
      error: err => { this.error.set(err.error?.message || 'Import fehlgeschlagen'); this.uploading.set(false); }
    });
  }
}
