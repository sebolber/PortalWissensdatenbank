import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ImportJobService } from '../../services/import-job.service';

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
      <p class="hint">Der Import laeuft im Hintergrund – Sie koennen waehrend des Uploads andere Seiten nutzen.</p>

      <div class="upload-area" (dragover)="$event.preventDefault()" (drop)="onDrop($event)">
        <input type="file" accept=".pdf" (change)="onFileSelect($event)" #fileInput style="display:none">
        <button class="btn btn-primary" (click)="fileInput.click()">PDF auswaehlen</button>
        <p *ngIf="selectedFile()" style="margin-top:0.5rem">{{ selectedFile()!.name }}</p>
      </div>

      <button *ngIf="selectedFile()" class="btn btn-primary" style="margin-top:1rem" (click)="upload()">Importieren</button>
      <div *ngIf="submitted()" class="info-msg" style="margin-top:1rem">
        Import wurde gestartet. Sie koennen diese Seite verlassen – der Fortschritt wird oben angezeigt.
      </div>
    </div>

    <!-- Abgeschlossene / fehlerhafte Jobs anzeigen -->
    <div *ngFor="let job of importJobs.recentDoneJobs()" class="card" style="margin-top:1rem"
         [class.success-card]="job.status === 'done'" [class.error-card]="job.status === 'error'">
      <div style="display:flex;justify-content:space-between;align-items:start">
        <div>
          <h3 *ngIf="job.status === 'done'">Import erfolgreich</h3>
          <h3 *ngIf="job.status === 'error'">Import fehlgeschlagen</h3>
          <p>{{ job.fileName }}</p>
          <p *ngIf="job.result"><strong>{{ job.result.recommendationCount }}</strong> Kodierempfehlungen erkannt.</p>
          <p *ngIf="job.errorMessage" class="error-text">{{ job.errorMessage }}</p>
          <a *ngIf="job.result" [routerLink]="'/wissen/' + job.result.id" class="btn btn-primary" style="margin-top:0.5rem">Zum Wissensobjekt</a>
        </div>
        <button class="btn-dismiss" (click)="importJobs.dismissJob(job.id)" title="Schliessen">&times;</button>
      </div>
    </div>
  `,
  styles: [`
    .upload-area { border:2px dashed #d6d3d1; border-radius:8px; padding:2rem; text-align:center; }
    .hint { color:#6b7280; font-size:0.85rem; }
    .info-msg { color:#1d4ed8; padding:0.75rem; background:#eff6ff; border-radius:6px; }
    .success-card { background:#f0fdf4; border:1px solid #86efac; }
    .error-card { background:#fef2f2; border:1px solid #fca5a5; }
    .error-text { color:#dc2626; }
    .btn-dismiss { background:none; border:none; font-size:1.25rem; cursor:pointer; color:#9ca3af; padding:0.25rem; }
    .btn-dismiss:hover { color:#1f2937; }
  `]
})
export class Seg4ImportComponent {
  readonly importJobs = inject(ImportJobService);

  readonly selectedFile = signal<File | null>(null);
  readonly submitted = signal(false);

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.selectedFile.set(input.files[0]);
      this.submitted.set(false);
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer?.files.length) {
      this.selectedFile.set(event.dataTransfer.files[0]);
      this.submitted.set(false);
    }
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.importJobs.startImport(file);
    this.selectedFile.set(null);
    this.submitted.set(true);
  }
}
