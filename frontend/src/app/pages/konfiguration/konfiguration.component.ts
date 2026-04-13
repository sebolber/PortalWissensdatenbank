import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DokumentService } from '../../services/dokument.service';
import { KnowledgeApiService, HandbuchImportResult } from '../../services/knowledge-api.service';
import { StatistikDto } from '../../models/dokument.model';

@Component({
  selector: 'app-konfiguration',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-header">
      <h1>Konfiguration</h1>
    </div>

    <div class="card">
      <h2 style="font-size:1.125rem; font-weight:600; margin-bottom:1rem">Systemuebersicht</h2>

      <div class="info-grid" *ngIf="statistik()">
        <div class="info-item">
          <div class="info-label">App-Version</div>
          <div class="info-value">1.0.0</div>
        </div>
        <div class="info-item">
          <div class="info-label">Dokumente gesamt</div>
          <div class="info-value">{{ statistik()!.totalDocuments }}</div>
        </div>
        <div class="info-item">
          <div class="info-label">Veroeffentlicht</div>
          <div class="info-value">{{ statistik()!.publishedDocuments }}</div>
        </div>
        <div class="info-item">
          <div class="info-label">Entwuerfe</div>
          <div class="info-value">{{ statistik()!.draftDocuments }}</div>
        </div>
        <div class="info-item">
          <div class="info-label">Archiviert</div>
          <div class="info-value">{{ statistik()!.archivedDocuments }}</div>
        </div>
        <div class="info-item">
          <div class="info-label">Kategorien</div>
          <div class="info-value">{{ statistik()!.totalCategories }}</div>
        </div>
        <div class="info-item">
          <div class="info-label">Tags</div>
          <div class="info-value">{{ statistik()!.totalTags }}</div>
        </div>
      </div>
    </div>

    <!-- Handbuch-Import -->
    <div class="card" style="margin-top:1.5rem">
      <h2 style="font-size:1.125rem; font-weight:600; margin-bottom:1rem">Handbuch-Import</h2>
      <p style="font-size:0.875rem; color:#57534e; margin-bottom:1rem">
        Importiere ein Benutzerhandbuch als strukturierte Wissensartikel.
        Lade eine JSON-Importdatei hoch, die mit <code>tools/parse_handbuch.py</code> erzeugt wurde.
      </p>

      <div class="import-area"
           (dragover)="onDragOver($event)"
           (dragleave)="onDragLeave($event)"
           (drop)="onDrop($event)"
           [class.drag-over]="isDragOver()">
        <div *ngIf="!importing() && !importResult()">
          <p style="margin-bottom:0.75rem">JSON-Datei hierher ziehen oder auswaehlen:</p>
          <input type="file" accept=".json" (change)="onFileSelect($event)" #fileInput style="display:none">
          <button class="btn btn-primary" (click)="fileInput.click()">Datei auswaehlen</button>
          <span *ngIf="selectedFile()" style="margin-left:1rem; font-size:0.875rem">
            {{ selectedFile()!.name }} ({{ (selectedFile()!.size / 1024) | number:'1.0-0' }} KB)
          </span>
        </div>

        <div *ngIf="selectedFile() && !importing() && !importResult()" style="margin-top:1rem">
          <button class="btn btn-primary" (click)="startImport()">Import starten</button>
        </div>

        <div *ngIf="importing()" style="text-align:center; padding:1rem">
          <p>Import laeuft...</p>
        </div>

        <div *ngIf="importResult()" class="import-success">
          <p style="font-weight:600; color:#16a34a">Import erfolgreich!</p>
          <div class="info-grid" style="margin-top:0.75rem">
            <div class="info-item">
              <div class="info-label">Kapitel</div>
              <div class="info-value">{{ importResult()!.knowledgeItemCount }}</div>
            </div>
            <div class="info-item">
              <div class="info-label">Abschnitte</div>
              <div class="info-value">{{ importResult()!.subArticleCount }}</div>
            </div>
          </div>
          <button class="btn btn-secondary" style="margin-top:1rem" (click)="resetImport()">Weiteren Import starten</button>
        </div>

        <div *ngIf="importError()" class="import-error">
          <p style="font-weight:600; color:#dc2626">Import fehlgeschlagen</p>
          <p style="font-size:0.875rem; margin-top:0.5rem">{{ importError() }}</p>
          <button class="btn btn-secondary" style="margin-top:0.75rem" (click)="resetImport()">Erneut versuchen</button>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top:1.5rem">
      <h2 style="font-size:1.125rem; font-weight:600; margin-bottom:1rem">Informationen</h2>
      <p style="font-size:0.875rem; color: #57534e; line-height: 1.7">
        Die Wissensdatenbank wird als Portal-App im Health Portal betrieben.
        Berechtigungen werden ueber das Portal-Gruppensystem verwaltet.
        Dashboard-Widgets koennen ueber die Portal-Konfiguration aktiviert werden.
      </p>
    </div>
  `,
  styles: [`
    .info-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 1rem; }
    .info-item { padding: 1rem; background: #f5f5f4; border-radius: 0.5rem; }
    .info-label { font-size: 0.8125rem; color: #78716c; }
    .info-value { font-size: 1.25rem; font-weight: 600; margin-top: 0.25rem; }
    .import-area {
      border: 2px dashed #d6d3d1; border-radius: 0.5rem; padding: 1.5rem;
      transition: border-color 0.2s, background 0.2s;
    }
    .import-area.drag-over { border-color: #2563eb; background: #eff6ff; }
    .import-success { padding: 0.5rem 0; }
    .import-error { padding: 0.5rem 0; }
    code { background: #f5f5f4; padding: 0.15rem 0.4rem; border-radius: 3px; font-size: 0.8125rem; }
  `]
})
export class KonfigurationComponent implements OnInit {
  private readonly dokumentService = inject(DokumentService);
  private readonly knowledgeApi = inject(KnowledgeApiService);

  readonly statistik = signal<StatistikDto | null>(null);
  readonly selectedFile = signal<File | null>(null);
  readonly importing = signal(false);
  readonly importResult = signal<HandbuchImportResult | null>(null);
  readonly importError = signal<string | null>(null);
  readonly isDragOver = signal(false);

  ngOnInit(): void {
    this.dokumentService.getStatistik().subscribe({
      next: s => this.statistik.set(s),
      error: (err) => console.error('Fehler beim Laden der Statistik', err)
    });
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile.set(input.files[0]);
      this.importError.set(null);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(false);
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      if (file.name.endsWith('.json')) {
        this.selectedFile.set(file);
        this.importError.set(null);
      } else {
        this.importError.set('Bitte eine JSON-Datei auswaehlen.');
      }
    }
  }

  startImport(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.importing.set(true);
    this.importError.set(null);

    const reader = new FileReader();
    reader.onload = () => {
      try {
        const data = JSON.parse(reader.result as string);

        // _meta-Feld entfernen (nicht Teil des Backend-DTOs)
        delete data._meta;

        this.knowledgeApi.importHandbuch(data).subscribe({
          next: result => {
            this.importResult.set(result);
            this.importing.set(false);
          },
          error: err => {
            const msg = err.error?.message || err.message || 'Unbekannter Fehler';
            this.importError.set(msg);
            this.importing.set(false);
          }
        });
      } catch (e) {
        this.importError.set('Ungueltige JSON-Datei.');
        this.importing.set(false);
      }
    };
    reader.readAsText(file);
  }

  resetImport(): void {
    this.selectedFile.set(null);
    this.importResult.set(null);
    this.importError.set(null);
  }
}
