import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DokumentService } from '../../services/dokument.service';
import { DokumentDto, StatistikDto } from '../../models/dokument.model';

@Component({
  selector: 'app-startseite',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page-header">
      <h1>Wissensdatenbank</h1>
      <a routerLink="/dokumente/neu" class="btn btn-primary">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        Neues Dokument
      </a>
    </div>

    <!-- Statistik-Karten -->
    <div class="stats-grid" *ngIf="statistik()">
      <div class="stat-card">
        <div class="stat-value">{{ statistik()!.totalDocuments }}</div>
        <div class="stat-label">Dokumente gesamt</div>
      </div>
      <div class="stat-card">
        <div class="stat-value published">{{ statistik()!.publishedDocuments }}</div>
        <div class="stat-label">Veroeffentlicht</div>
      </div>
      <div class="stat-card">
        <div class="stat-value draft">{{ statistik()!.draftDocuments }}</div>
        <div class="stat-label">Entwuerfe</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ statistik()!.totalCategories }}</div>
        <div class="stat-label">Kategorien</div>
      </div>
    </div>

    <!-- Neueste Dokumente -->
    <div class="section">
      <div class="section-header">
        <h2>Neueste Dokumente</h2>
        <a routerLink="/dokumente" class="btn btn-secondary btn-sm">Alle anzeigen</a>
      </div>
      <div class="card">
        <div *ngIf="newest().length === 0" class="empty-state">
          <p>Noch keine Dokumente vorhanden.</p>
        </div>
        <div *ngFor="let doc of newest()" class="doc-item">
          <a [routerLink]="'/dokumente/' + doc.id" class="doc-title">{{ doc.title }}</a>
          <div class="doc-meta">
            <span class="badge" [ngClass]="'badge-' + doc.status.toLowerCase()">{{ doc.status }}</span>
            <span>{{ doc.createdAt | date:'dd.MM.yyyy HH:mm' }}</span>
            <span *ngIf="doc.categoryName">{{ doc.categoryName }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Beliebte Dokumente -->
    <div class="section" *ngIf="popular().length > 0">
      <div class="section-header">
        <h2>Beliebte Dokumente</h2>
      </div>
      <div class="card">
        <div *ngFor="let doc of popular()" class="doc-item">
          <a [routerLink]="'/dokumente/' + doc.id" class="doc-title">{{ doc.title }}</a>
          <div class="doc-meta">
            <span>{{ doc.viewCount }} Aufrufe</span>
            <span *ngIf="doc.averageRating > 0">Bewertung: {{ doc.averageRating | number:'1.1-1' }}/5</span>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 2rem; }
    .stat-card { background: #fff; border: 1px solid #e7e5e4; border-radius: 0.5rem; padding: 1.25rem; }
    .stat-value { font-size: 2rem; font-weight: 700; color: #1c1917; }
    .stat-value.published { color: #16a34a; }
    .stat-value.draft { color: #d97706; }
    .stat-label { font-size: 0.8125rem; color: #78716c; margin-top: 0.25rem; }

    .section { margin-bottom: 2rem; }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.75rem; }
    .section-header h2 { font-size: 1.125rem; font-weight: 600; }

    .doc-item { padding: 0.75rem 0; border-bottom: 1px solid #e7e5e4; }
    .doc-item:last-child { border-bottom: none; }
    .doc-title { font-weight: 500; font-size: 0.9375rem; }
    .doc-meta { display: flex; gap: 0.75rem; margin-top: 0.375rem; font-size: 0.8125rem; color: #78716c; align-items: center; }

    @media (max-width: 768px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
  `]
})
export class StartseiteComponent implements OnInit {
  private readonly dokumentService = inject(DokumentService);

  readonly statistik = signal<StatistikDto | null>(null);
  readonly newest = signal<DokumentDto[]>([]);
  readonly popular = signal<DokumentDto[]>([]);

  ngOnInit(): void {
    this.dokumentService.getStatistik().subscribe(s => this.statistik.set(s));
    this.dokumentService.getNewest(5).subscribe(d => this.newest.set(d));
    this.dokumentService.getPopular(5).subscribe(d => this.popular.set(d));
  }
}
