import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { KnowledgeApiService } from '../../services/knowledge-api.service';
import { ChatApiService } from '../../services/chat-api.service';
import { KnowledgeItemDto, DocumentSuggestionDto } from '../../models/knowledge.model';

@Component({
  selector: 'app-startseite',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page-header">
      <h1>Wissensdatenbank</h1>
      <p class="subtitle">Ueberblick ueber SEG4-Importe und Dokument-Kodierungen</p>
    </div>

    <!-- Statistik-Karten -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-value">{{ seg4Count() }}</div>
        <div class="stat-label">SEG4-Wissensobjekte</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ totalKnowledge() }}</div>
        <div class="stat-label">Wissensobjekte gesamt</div>
      </div>
      <div class="stat-card">
        <div class="stat-value completed">{{ completedKodierungen() }}</div>
        <div class="stat-label">Kodierungen abgeschlossen</div>
      </div>
      <div class="stat-card">
        <div class="stat-value processing">{{ processingKodierungen() }}</div>
        <div class="stat-label">Kodierungen in Bearbeitung</div>
      </div>
    </div>

    <!-- Zwei-Spalten Layout -->
    <div class="two-col">

      <!-- SEG4-Importe -->
      <div class="section">
        <div class="section-header">
          <h2>Neueste SEG4-Wissensobjekte</h2>
          <a routerLink="/wissen" class="btn btn-secondary btn-sm">Alle anzeigen</a>
        </div>
        <div class="card">
          <div *ngIf="seg4Items().length === 0" class="empty-hint">
            Noch keine SEG4-Importe vorhanden.
            <a routerLink="/seg4-import" class="link">Jetzt importieren</a>
          </div>
          <div *ngFor="let item of seg4Items()" class="list-item">
            <a [routerLink]="'/wissen/' + item.id" class="item-title">{{ item.title }}</a>
            <div class="item-meta">
              <span class="badge" [class]="'binding-' + item.bindingLevel.toLowerCase()">{{ item.bindingLevel }}</span>
              <span>{{ item.seg4RecommendationCount }} Empfehlungen</span>
              <span>{{ item.createdAt | date:'dd.MM.yyyy' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Dokument-Kodierungen -->
      <div class="section">
        <div class="section-header">
          <h2>Dokument-Kodierungen</h2>
          <a routerLink="/dokument-kodierung" class="btn btn-secondary btn-sm">Alle anzeigen</a>
        </div>
        <div class="card">
          <div *ngIf="kodierungen().length === 0" class="empty-hint">
            Noch keine Dokument-Kodierungen vorhanden.
            <a routerLink="/dokument-kodierung" class="link">Jetzt hochladen</a>
          </div>
          <div *ngFor="let doc of kodierungen()" class="list-item">
            <div class="item-title-row">
              <span class="item-title">{{ doc.fileName }}</span>
              <span class="status-badge" [class]="'status-' + doc.status.toLowerCase()">
                {{ statusLabel(doc.status) }}
              </span>
            </div>
            <div class="item-meta">
              <span>{{ doc.createdAt | date:'dd.MM.yyyy HH:mm' }}</span>
              <span *ngIf="doc.empfehlungen.length > 0">{{ doc.empfehlungen.length }} Empfehlungen</span>
              <span *ngIf="doc.llmModel">{{ doc.llmModel }}</span>
            </div>
            <div class="item-actions" *ngIf="doc.status === 'COMPLETED'">
              <a routerLink="/dokument-kodierung" class="link-sm">Ergebnis anzeigen</a>
            </div>
            <div class="item-error" *ngIf="doc.status === 'ERROR' && doc.errorMessage">
              {{ doc.errorMessage }}
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- Schnellzugriff -->
    <div class="section">
      <h2>Schnellzugriff</h2>
      <div class="quick-links">
        <a routerLink="/seg4-import" class="quick-link">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          <span>SEG4 importieren</span>
        </a>
        <a routerLink="/dokument-kodierung" class="quick-link">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="9" y1="15" x2="15" y2="15"/><line x1="12" y1="12" x2="12" y2="18"/></svg>
          <span>Dokument kodieren</span>
        </a>
        <a routerLink="/kodierempfehlung" class="quick-link">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          <span>KI-Kodierempfehlung</span>
        </a>
        <a routerLink="/wissen" class="quick-link">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z"/><path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z"/></svg>
          <span>Wissensobjekte</span>
        </a>
      </div>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 1.5rem; }
    .page-header h1 { font-size: 1.5rem; font-weight: 700; color: #1f2937; margin: 0; }
    .subtitle { color: #6b7280; margin: 0.25rem 0 0; font-size: 0.875rem; }

    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 2rem; }
    .stat-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 0.75rem; padding: 1.25rem; }
    .stat-value { font-size: 2rem; font-weight: 700; color: #1f2937; }
    .stat-value.completed { color: #059669; }
    .stat-value.processing { color: #2563eb; }
    .stat-label { font-size: 0.8125rem; color: #6b7280; margin-top: 0.25rem; }

    .two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; margin-bottom: 2rem; }

    .section { margin-bottom: 1.5rem; }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.75rem; }
    .section-header h2 { font-size: 1.125rem; font-weight: 600; margin: 0; }
    .section h2 { font-size: 1.125rem; font-weight: 600; margin: 0 0 0.75rem; }

    .card {
      background: #fff; border: 1px solid #e5e7eb; border-radius: 0.75rem; padding: 1rem 1.25rem;
    }

    .list-item { padding: 0.75rem 0; border-bottom: 1px solid #f3f4f6; }
    .list-item:last-child { border-bottom: none; }
    .item-title { font-weight: 600; font-size: 0.9375rem; color: #1f2937; text-decoration: none; }
    a.item-title:hover { color: #006EC7; }
    .item-title-row { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; }
    .item-meta { display: flex; gap: 0.75rem; margin-top: 0.375rem; font-size: 0.8125rem; color: #6b7280; align-items: center; }
    .item-actions { margin-top: 0.25rem; }
    .item-error { color: #ef4444; font-size: 0.8125rem; margin-top: 0.25rem; }

    .empty-hint { color: #9ca3af; font-size: 0.875rem; padding: 1rem 0; text-align: center; }

    .badge {
      display: inline-block; padding: 0.125rem 0.375rem; border-radius: 0.25rem;
      font-size: 0.6875rem; font-weight: 700;
    }
    .binding-verbindlich { background: #fee2e2; color: #991b1b; }
    .binding-empfehlung { background: #dbeafe; color: #1e40af; }
    .binding-lex_specialis { background: #fef3c7; color: #92400e; }
    .binding-informativ { background: #e5e7eb; color: #374151; }

    .status-badge {
      display: inline-block; padding: 0.125rem 0.5rem; border-radius: 9999px;
      font-size: 0.6875rem; font-weight: 600;
    }
    .status-pending { background: #fef3c7; color: #92400e; }
    .status-processing { background: #dbeafe; color: #1e40af; }
    .status-completed { background: #d1fae5; color: #065f46; }
    .status-error { background: #fee2e2; color: #991b1b; }

    .link { color: #006EC7; font-weight: 600; text-decoration: underline; }
    .link-sm { color: #006EC7; font-size: 0.8125rem; font-weight: 600; text-decoration: underline; }

    .btn { display: inline-flex; align-items: center; gap: 0.25rem; border: none; border-radius: 0.5rem; font-weight: 600; cursor: pointer; font-size: 0.8125rem; padding: 0.5rem 1rem; transition: all 0.15s; text-decoration: none; }
    .btn-sm { padding: 0.375rem 0.75rem; }
    .btn-primary { background: #006EC7; color: #fff; }
    .btn-primary:hover { background: #004a8a; }
    .btn-secondary { background: #f3f4f6; color: #374151; }
    .btn-secondary:hover { background: #e5e7eb; }

    .quick-links { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; }
    .quick-link {
      display: flex; flex-direction: column; align-items: center; gap: 0.5rem;
      background: #fff; border: 1px solid #e5e7eb; border-radius: 0.75rem;
      padding: 1.25rem; text-decoration: none; color: #374151; transition: all 0.15s;
      text-align: center;
    }
    .quick-link:hover { border-color: #006EC7; background: #eff6ff; color: #006EC7; }
    .quick-link svg { width: 28px; height: 28px; color: #6b7280; }
    .quick-link:hover svg { color: #006EC7; }
    .quick-link span { font-size: 0.8125rem; font-weight: 600; }

    @media (max-width: 900px) {
      .stats-grid { grid-template-columns: repeat(2, 1fr); }
      .two-col { grid-template-columns: 1fr; }
      .quick-links { grid-template-columns: repeat(2, 1fr); }
    }
  `]
})
export class StartseiteComponent implements OnInit {
  private readonly knowledgeApi = inject(KnowledgeApiService);
  private readonly chatApi = inject(ChatApiService);

  readonly seg4Items = signal<KnowledgeItemDto[]>([]);
  readonly seg4Count = signal(0);
  readonly totalKnowledge = signal(0);
  readonly kodierungen = signal<DocumentSuggestionDto[]>([]);

  readonly completedKodierungen = signal(0);
  readonly processingKodierungen = signal(0);

  ngOnInit(): void {
    // SEG4-Wissensobjekte laden
    this.knowledgeApi.list({ type: 'SEG4', size: 5 }).subscribe({
      next: page => {
        this.seg4Items.set(page.content);
        this.seg4Count.set(page.totalElements);
      },
      error: () => {}
    });

    // Gesamt-Wissensobjekte
    this.knowledgeApi.list({ size: 1 }).subscribe({
      next: page => this.totalKnowledge.set(page.totalElements),
      error: () => {}
    });

    // Dokument-Kodierungen laden
    this.chatApi.listDocumentSuggestions().subscribe({
      next: list => {
        this.kodierungen.set(list.slice(0, 5));
        this.completedKodierungen.set(list.filter(d => d.status === 'COMPLETED').length);
        this.processingKodierungen.set(list.filter(d => d.status === 'PROCESSING').length);
      },
      error: () => {}
    });
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'Ausstehend';
      case 'PROCESSING': return 'In Bearbeitung';
      case 'COMPLETED': return 'Abgeschlossen';
      case 'ERROR': return 'Fehler';
      default: return status;
    }
  }
}
