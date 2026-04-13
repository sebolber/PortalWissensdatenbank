import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { KnowledgeApiService } from '../../services/knowledge-api.service';
import { KnowledgeItemDto, KnowledgeType } from '../../models/knowledge.model';

@Component({
  selector: 'app-knowledge-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="page-header">
      <h1>Wissensdatenbank</h1>
      <div style="display:flex;gap:0.5rem">
        <a routerLink="/seg4-import" class="btn btn-secondary">SEG4 importieren</a>
        <a routerLink="/kodierempfehlung" class="btn btn-primary">KI-Empfehlung</a>
      </div>
    </div>

    <div class="card filter-bar">
      <select class="form-control filter-select" [(ngModel)]="typeFilter" (change)="load()">
        <option value="">Alle Typen</option>
        <option value="SEG4">SEG4</option>
        <option value="ARTICLE">Artikel</option>
        <option value="GUIDELINE">Leitlinie</option>
        <option value="HANDBUCH">Handbuch</option>
      </select>
    </div>

    <div class="card" style="margin-top:1rem">
      <div *ngIf="loading()" style="text-align:center;padding:2rem">Laden...</div>
      <div *ngIf="!loading() && items().length === 0" class="empty-state">
        <p>Keine Wissensobjekte vorhanden.</p>
      </div>
      <table class="table" *ngIf="!loading() && items().length > 0">
        <thead>
          <tr>
            <th>Titel</th>
            <th>Typ</th>
            <th>Verbindlichkeit</th>
            <th>Produkt</th>
            <th>SEG4</th>
            <th>Erstellt</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let item of items()">
            <td><a [routerLink]="'/wissen/' + item.id" style="font-weight:500">{{ item.title }}</a></td>
            <td><span class="badge">{{ typeLabel(item.knowledgeType) }}</span></td>
            <td><span class="badge" [ngClass]="'badge-' + item.bindingLevel.toLowerCase()">{{ item.bindingLevel }}</span></td>
            <td>{{ item.productName ? item.productName + ' ' + (item.productVersionLabel || '') : '-' }}</td>
            <td>{{ item.seg4RecommendationCount || '-' }}</td>
            <td>{{ item.createdAt | date:'dd.MM.yyyy' }}</td>
          </tr>
        </tbody>
      </table>
      <div class="pagination" *ngIf="totalPages() > 1">
        <button class="btn btn-secondary btn-sm" [disabled]="page() === 0" (click)="goToPage(page() - 1)">Zurueck</button>
        <span class="page-info">Seite {{ page() + 1 }} von {{ totalPages() }}</span>
        <button class="btn btn-secondary btn-sm" [disabled]="page() >= totalPages() - 1" (click)="goToPage(page() + 1)">Weiter</button>
      </div>
    </div>
  `,
  styles: [`
    .filter-bar { display:flex; gap:0.75rem; align-items:center; }
    .filter-select { width:180px; }
    .pagination { display:flex; align-items:center; justify-content:center; gap:1rem; padding:1rem 0 0; }
    .page-info { font-size:0.875rem; color:#78716c; }
  `]
})
export class KnowledgeListComponent implements OnInit {
  private readonly api = inject(KnowledgeApiService);

  readonly items = signal<KnowledgeItemDto[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  typeFilter = '';

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.api.list({
      type: (this.typeFilter as KnowledgeType) || undefined,
      page: this.page(),
      size: 20
    }).subscribe({
      next: res => { this.items.set(res.content); this.totalPages.set(res.totalPages); this.loading.set(false); },
      error: (err) => { console.error('Fehler beim Laden der Wissensobjekte', err); this.loading.set(false); }
    });
  }

  goToPage(p: number): void { this.page.set(p); this.load(); }

  typeLabel(type: KnowledgeType): string {
    switch (type) {
      case 'SEG4': return 'SEG4';
      case 'ARTICLE': return 'Artikel';
      case 'GUIDELINE': return 'Leitlinie';
      case 'HANDBUCH': return 'Handbuch';
      default: return type;
    }
  }
}
