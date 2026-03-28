import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DokumentService } from '../../services/dokument.service';
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
  `]
})
export class KonfigurationComponent implements OnInit {
  private readonly dokumentService = inject(DokumentService);
  readonly statistik = signal<StatistikDto | null>(null);

  ngOnInit(): void {
    this.dokumentService.getStatistik().subscribe({
      next: s => this.statistik.set(s),
      error: (err) => console.error('Fehler beim Laden der Statistik', err)
    });
  }
}
