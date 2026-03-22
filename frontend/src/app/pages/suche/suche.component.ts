import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DokumentService } from '../../services/dokument.service';
import { DokumentDto } from '../../models/dokument.model';

@Component({
  selector: 'app-suche',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="page-header">
      <h1>Suche</h1>
    </div>

    <div class="card">
      <div class="search-bar">
        <input type="text" class="form-control" [(ngModel)]="query"
               placeholder="Suchbegriff eingeben..." (keyup.enter)="search()" autofocus>
        <button class="btn btn-primary" (click)="search()" [disabled]="!query.trim()">Suchen</button>
      </div>
    </div>

    <div *ngIf="searched() && results().length === 0" class="card" style="margin-top:1rem">
      <div class="empty-state">
        <p>Keine Ergebnisse fuer "{{ lastQuery }}" gefunden.</p>
      </div>
    </div>

    <div *ngIf="results().length > 0" class="card" style="margin-top:1rem">
      <div class="result-count">{{ totalResults() }} Ergebnisse fuer "{{ lastQuery }}"</div>
      <div *ngFor="let doc of results()" class="result-item">
        <a [routerLink]="'/dokumente/' + doc.id" class="result-title">{{ doc.title }}</a>
        <p class="result-summary" *ngIf="doc.summary">{{ doc.summary }}</p>
        <div class="result-meta">
          <span class="badge" [ngClass]="'badge-' + doc.status.toLowerCase()">{{ doc.status }}</span>
          <span *ngIf="doc.categoryName">{{ doc.categoryName }}</span>
          <span>{{ doc.createdAt | date:'dd.MM.yyyy' }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .search-bar { display: flex; gap: 0.75rem; }
    .search-bar input { flex: 1; }
    .result-count { font-size: 0.875rem; color: #78716c; margin-bottom: 1rem; }
    .result-item { padding: 1rem 0; border-bottom: 1px solid #e7e5e4; }
    .result-item:last-child { border-bottom: none; }
    .result-title { font-size: 1.0625rem; font-weight: 500; }
    .result-summary { margin-top: 0.375rem; font-size: 0.875rem; color: #57534e; }
    .result-meta { display: flex; gap: 0.75rem; margin-top: 0.375rem; font-size: 0.8125rem; color: #78716c; align-items: center; }
  `]
})
export class SucheComponent {
  private readonly dokumentService = inject(DokumentService);

  readonly results = signal<DokumentDto[]>([]);
  readonly totalResults = signal(0);
  readonly searched = signal(false);

  query = '';
  lastQuery = '';

  search(): void {
    if (!this.query.trim()) return;
    this.lastQuery = this.query;
    this.dokumentService.list({ q: this.query, size: 50 }).subscribe(res => {
      this.results.set(res.content);
      this.totalResults.set(res.totalElements);
      this.searched.set(true);
    });
  }
}
