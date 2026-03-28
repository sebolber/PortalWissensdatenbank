import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DokumentService } from '../../services/dokument.service';
import { CategoryDto, TagDto } from '../../models/dokument.model';

@Component({
  selector: 'app-dokument-form',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="page-header">
      <h1>{{ isEdit ? 'Dokument bearbeiten' : 'Neues Dokument' }}</h1>
    </div>

    <div class="card">
      <div class="form-group">
        <label for="title">Titel *</label>
        <input id="title" type="text" class="form-control" [(ngModel)]="title" placeholder="Dokumenttitel eingeben" maxlength="500">
      </div>

      <div class="form-group">
        <label for="summary">Zusammenfassung</label>
        <input id="summary" type="text" class="form-control" [(ngModel)]="summary" placeholder="Kurze Zusammenfassung" maxlength="2000">
      </div>

      <div class="form-row">
        <div class="form-group" style="flex:1">
          <label for="category">Kategorie</label>
          <select id="category" class="form-control" [(ngModel)]="categoryId">
            <option value="">Keine Kategorie</option>
            <option *ngFor="let cat of categories()" [value]="cat.id">{{ cat.name }}</option>
          </select>
        </div>
      </div>

      <!-- Tags -->
      <div class="form-group">
        <label>Tags</label>
        <div class="tag-selector">
          <label *ngFor="let tag of availableTags()" class="tag-option">
            <input type="checkbox" [checked]="selectedTagIds.has(tag.id)" (change)="toggleTag(tag.id)">
            {{ tag.name }}
          </label>
        </div>
      </div>

      <div class="form-group">
        <label for="content">Inhalt *</label>
        <textarea id="content" class="form-control" [(ngModel)]="content" placeholder="Dokumentinhalt eingeben"></textarea>
      </div>

      <div class="form-group" *ngIf="isEdit">
        <label for="changeNote">Aenderungsnotiz</label>
        <input id="changeNote" type="text" class="form-control" [(ngModel)]="changeNote" placeholder="Was wurde geaendert?">
      </div>

      <div class="form-actions">
        <button class="btn btn-primary" (click)="save()" [disabled]="!title.trim() || !content.trim() || saving()">
          {{ saving() ? 'Speichern...' : (isEdit ? 'Aktualisieren' : 'Erstellen') }}
        </button>
        <a routerLink="/dokumente" class="btn btn-secondary">Abbrechen</a>
      </div>
    </div>
  `,
  styles: [`
    .form-row { display: flex; gap: 1rem; }
    .form-actions { display: flex; gap: 0.75rem; margin-top: 1.5rem; }
    .tag-selector { display: flex; flex-wrap: wrap; gap: 0.75rem; }
    .tag-option { display: flex; align-items: center; gap: 0.375rem; font-size: 0.875rem; cursor: pointer; }
    .tag-option input { cursor: pointer; }
  `]
})
export class DokumentFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dokumentService = inject(DokumentService);

  readonly categories = signal<CategoryDto[]>([]);
  readonly availableTags = signal<TagDto[]>([]);
  readonly saving = signal(false);

  isEdit = false;
  documentId = '';
  title = '';
  summary = '';
  content = '';
  categoryId = '';
  changeNote = '';
  selectedTagIds = new Set<string>();

  ngOnInit(): void {
    this.dokumentService.getCategories().subscribe({
      next: c => this.categories.set(c),
      error: (err) => console.error('Fehler beim Laden der Kategorien', err)
    });
    this.dokumentService.getTags().subscribe({
      next: t => this.availableTags.set(t),
      error: (err) => console.error('Fehler beim Laden der Tags', err)
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.documentId = id;
      this.dokumentService.getById(id).subscribe({
        next: doc => {
          this.title = doc.title;
          this.summary = doc.summary || '';
          this.content = doc.content;
          this.categoryId = doc.categoryId || '';
          doc.tags.forEach(t => this.selectedTagIds.add(t.id));
        },
        error: (err) => console.error('Fehler beim Laden des Dokuments', err)
      });
    }
  }

  toggleTag(tagId: string): void {
    if (this.selectedTagIds.has(tagId)) {
      this.selectedTagIds.delete(tagId);
    } else {
      this.selectedTagIds.add(tagId);
    }
  }

  save(): void {
    this.saving.set(true);
    const data = {
      title: this.title,
      content: this.content,
      summary: this.summary || undefined,
      categoryId: this.categoryId || undefined,
      tagIds: Array.from(this.selectedTagIds),
      publicWithinTenant: true,
      changeNote: this.changeNote || undefined,
    };

    const request = this.isEdit
      ? this.dokumentService.update(this.documentId, data)
      : this.dokumentService.create(data);

    request.subscribe({
      next: (doc) => this.router.navigate(['/dokumente', doc.id]),
      error: (err) => { console.error('Fehler beim Speichern des Dokuments', err); this.saving.set(false); }
    });
  }
}
