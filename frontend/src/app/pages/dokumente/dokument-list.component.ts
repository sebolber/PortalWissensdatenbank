import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DokumentService } from '../../services/dokument.service';
import { DokumentDto, PageResponse, CategoryDto } from '../../models/dokument.model';

@Component({
  selector: 'app-dokument-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="page-header">
      <h1>Dokumente</h1>
      <a routerLink="/dokumente/neu" class="btn btn-primary">Neues Dokument</a>
    </div>

    <!-- Filter -->
    <div class="card filter-bar">
      <input type="text" class="form-control search-input" placeholder="Dokumente durchsuchen..."
             [(ngModel)]="searchQuery" (keyup.enter)="search()">
      <select class="form-control filter-select" [(ngModel)]="statusFilter" (change)="loadDocuments()">
        <option value="">Alle Status</option>
        <option value="DRAFT">Entwurf</option>
        <option value="PUBLISHED">Veroeffentlicht</option>
        <option value="ARCHIVED">Archiviert</option>
      </select>
      <select class="form-control filter-select" [(ngModel)]="categoryFilter" (change)="loadDocuments()">
        <option value="">Alle Kategorien</option>
        <option *ngFor="let cat of categories()" [value]="cat.id">{{ cat.name }}</option>
      </select>
    </div>

    <!-- Dokument-Liste -->
    <div class="card" style="margin-top: 1rem;">
      <div *ngIf="loading()" style="text-align:center; padding: 2rem;">Laden...</div>

      <div *ngIf="!loading() && documents().length === 0" class="empty-state">
        <p>Keine Dokumente gefunden.</p>
        <a routerLink="/dokumente/neu" class="btn btn-primary" style="margin-top:1rem">Erstes Dokument erstellen</a>
      </div>

      <table class="table" *ngIf="!loading() && documents().length > 0">
        <thead>
          <tr>
            <th>Titel</th>
            <th>Status</th>
            <th>Kategorie</th>
            <th>Erstellt</th>
            <th>Aufrufe</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let doc of documents()">
            <td><a [routerLink]="'/dokumente/' + doc.id" style="font-weight:500">{{ doc.title }}</a></td>
            <td><span class="badge" [ngClass]="'badge-' + doc.status.toLowerCase()">{{ doc.status }}</span></td>
            <td>{{ doc.categoryName || '-' }}</td>
            <td>{{ doc.createdAt | date:'dd.MM.yyyy' }}</td>
            <td>{{ doc.viewCount }}</td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="pagination" *ngIf="totalPages() > 1">
        <button class="btn btn-secondary btn-sm" [disabled]="currentPage() === 0" (click)="goToPage(currentPage() - 1)">Zurueck</button>
        <span class="page-info">Seite {{ currentPage() + 1 }} von {{ totalPages() }}</span>
        <button class="btn btn-secondary btn-sm" [disabled]="currentPage() >= totalPages() - 1" (click)="goToPage(currentPage() + 1)">Weiter</button>
      </div>
    </div>
  `,
  styles: [`
    .filter-bar { display: flex; gap: 0.75rem; align-items: center; }
    .search-input { flex: 1; }
    .filter-select { width: 180px; }
    .pagination { display: flex; align-items: center; justify-content: center; gap: 1rem; padding: 1rem 0 0; }
    .page-info { font-size: 0.875rem; color: #78716c; }
  `]
})
export class DokumentListComponent implements OnInit {
  private readonly dokumentService = inject(DokumentService);

  readonly documents = signal<DokumentDto[]>([]);
  readonly categories = signal<CategoryDto[]>([]);
  readonly loading = signal(true);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);

  searchQuery = '';
  statusFilter = '';
  categoryFilter = '';

  ngOnInit(): void {
    this.dokumentService.getCategories().subscribe(c => this.categories.set(c));
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.loading.set(true);
    this.dokumentService.list({
      status: this.statusFilter || undefined,
      categoryId: this.categoryFilter || undefined,
      q: this.searchQuery || undefined,
      page: this.currentPage(),
      size: 20
    }).subscribe({
      next: (res) => {
        this.documents.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  search(): void {
    this.currentPage.set(0);
    this.loadDocuments();
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadDocuments();
  }
}
