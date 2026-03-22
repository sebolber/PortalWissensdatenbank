import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DokumentService } from '../../services/dokument.service';
import { CategoryDto, TagDto } from '../../models/dokument.model';

@Component({
  selector: 'app-kategorien',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <h1>Kategorien & Tags</h1>
    </div>

    <!-- Kategorien -->
    <div class="card">
      <h2 style="font-size:1.125rem; font-weight:600; margin-bottom:1rem">Kategorien</h2>

      <div class="add-form">
        <input type="text" class="form-control" [(ngModel)]="newCategoryName" placeholder="Neue Kategorie">
        <input type="text" class="form-control" [(ngModel)]="newCategoryDesc" placeholder="Beschreibung (optional)">
        <button class="btn btn-primary btn-sm" (click)="addCategory()" [disabled]="!newCategoryName.trim()">Hinzufuegen</button>
      </div>

      <div *ngIf="categories().length === 0" class="empty-state">
        <p>Noch keine Kategorien vorhanden.</p>
      </div>

      <table class="table" *ngIf="categories().length > 0" style="margin-top:1rem">
        <thead>
          <tr><th>Name</th><th>Beschreibung</th><th style="width:80px"></th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let cat of categories()">
            <td>{{ cat.name }}</td>
            <td>{{ cat.description || '-' }}</td>
            <td>
              <button class="btn btn-danger btn-sm" (click)="deleteCategory(cat.id)">X</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Tags -->
    <div class="card" style="margin-top:1.5rem">
      <h2 style="font-size:1.125rem; font-weight:600; margin-bottom:1rem">Tags</h2>

      <div class="add-form">
        <input type="text" class="form-control" [(ngModel)]="newTagName" placeholder="Neuer Tag">
        <button class="btn btn-primary btn-sm" (click)="addTag()" [disabled]="!newTagName.trim()">Hinzufuegen</button>
      </div>

      <div class="tags-list" *ngIf="tags().length > 0">
        <div *ngFor="let tag of tags()" class="tag-item">
          <span>{{ tag.name }}</span>
          <button class="tag-delete" (click)="deleteTag(tag.id)">X</button>
        </div>
      </div>
      <div *ngIf="tags().length === 0" class="empty-state">
        <p>Noch keine Tags vorhanden.</p>
      </div>
    </div>
  `,
  styles: [`
    .add-form { display: flex; gap: 0.75rem; align-items: center; }
    .add-form input { flex: 1; }
    .tags-list { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-top: 1rem; }
    .tag-item {
      display: flex; align-items: center; gap: 0.375rem;
      background: #e8f4fd; color: #006EC7; padding: 0.375rem 0.75rem;
      border-radius: 9999px; font-size: 0.875rem;
    }
    .tag-delete {
      background: none; border: none; color: #006EC7; cursor: pointer;
      font-size: 0.75rem; font-weight: 700; padding: 0;
    }
    .tag-delete:hover { color: #dc2626; }
  `]
})
export class KategorienComponent implements OnInit {
  private readonly dokumentService = inject(DokumentService);

  readonly categories = signal<CategoryDto[]>([]);
  readonly tags = signal<TagDto[]>([]);

  newCategoryName = '';
  newCategoryDesc = '';
  newTagName = '';

  ngOnInit(): void {
    this.loadCategories();
    this.loadTags();
  }

  loadCategories(): void {
    this.dokumentService.getCategories().subscribe(c => this.categories.set(c));
  }

  loadTags(): void {
    this.dokumentService.getTags().subscribe(t => this.tags.set(t));
  }

  addCategory(): void {
    this.dokumentService.createCategory(this.newCategoryName, this.newCategoryDesc || undefined).subscribe(() => {
      this.newCategoryName = '';
      this.newCategoryDesc = '';
      this.loadCategories();
    });
  }

  deleteCategory(id: string): void {
    if (confirm('Kategorie wirklich loeschen?')) {
      this.dokumentService.deleteCategory(id).subscribe(() => this.loadCategories());
    }
  }

  addTag(): void {
    this.dokumentService.createTag(this.newTagName).subscribe(() => {
      this.newTagName = '';
      this.loadTags();
    });
  }

  deleteTag(id: string): void {
    this.dokumentService.deleteTag(id).subscribe(() => this.loadTags());
  }
}
