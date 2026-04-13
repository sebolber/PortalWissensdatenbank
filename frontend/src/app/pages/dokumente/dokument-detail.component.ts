import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DokumentService } from '../../services/dokument.service';
import { DokumentDto, DokumentVersionDto } from '../../models/dokument.model';

@Component({
  selector: 'app-dokument-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div *ngIf="loading()" style="text-align:center; padding:3rem">Laden...</div>

    <div *ngIf="doc()">
      <div class="page-header">
        <div>
          <h1>{{ doc()!.title }}</h1>
          <div class="meta">
            <span class="badge" [ngClass]="'badge-' + doc()!.status.toLowerCase()">{{ doc()!.status }}</span>
            <span *ngIf="doc()!.categoryName">{{ doc()!.categoryName }}</span>
            <span>Version {{ doc()!.version }}</span>
            <span>{{ doc()!.viewCount }} Aufrufe</span>
          </div>
        </div>
        <div class="actions">
          <a [routerLink]="'/dokumente/' + doc()!.id + '/bearbeiten'" class="btn btn-secondary">Bearbeiten</a>
          <button *ngIf="doc()!.status === 'DRAFT'" class="btn btn-success" (click)="publish()">Veroeffentlichen</button>
          <button *ngIf="doc()!.status === 'PUBLISHED'" class="btn btn-secondary" (click)="archive()">Archivieren</button>
          <button class="btn btn-danger" (click)="deleteDoc()">Loeschen</button>
        </div>
      </div>

      <!-- Tags -->
      <div class="tags" *ngIf="doc()!.tags.length > 0">
        <span class="tag" *ngFor="let tag of doc()!.tags">{{ tag.name }}</span>
      </div>

      <!-- Summary -->
      <div class="card summary" *ngIf="doc()!.summary">
        <strong>Zusammenfassung:</strong>
        <p>{{ doc()!.summary }}</p>
      </div>

      <!-- Content -->
      <div class="card content-area">
        <div class="doc-content" [innerHTML]="doc()!.content"></div>
      </div>

      <!-- Bewertung -->
      <div class="card rating-section">
        <h3>Dokument bewerten</h3>
        <div class="rating-stars">
          <button *ngFor="let star of [1,2,3,4,5]" class="star-btn"
                  [class.active]="star <= feedbackRating"
                  (click)="feedbackRating = star">
            {{ star <= feedbackRating ? '\u2605' : '\u2606' }}
          </button>
          <span class="rating-text" *ngIf="doc()!.ratingCount > 0">
            {{ doc()!.averageRating | number:'1.1-1' }}/5 ({{ doc()!.ratingCount }} Bewertungen)
          </span>
        </div>
        <textarea class="form-control" [(ngModel)]="feedbackComment" placeholder="Kommentar (optional)"
                  style="margin-top:0.5rem; min-height:60px"></textarea>
        <button class="btn btn-primary btn-sm" style="margin-top:0.5rem" (click)="submitFeedback()"
                [disabled]="feedbackRating === 0">Bewertung abgeben</button>
      </div>

      <!-- Versionen -->
      <div class="card" *ngIf="versions().length > 0" style="margin-top:1rem">
        <h3>Versionsverlauf</h3>
        <table class="table" style="margin-top:0.5rem">
          <thead>
            <tr><th>Version</th><th>Titel</th><th>Geaendert</th><th>Notiz</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let v of versions()">
              <td>V{{ v.version }}</td>
              <td>{{ v.title }}</td>
              <td>{{ v.changedAt | date:'dd.MM.yyyy HH:mm' }}</td>
              <td>{{ v.changeNote || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .meta { display: flex; gap: 0.75rem; align-items: center; margin-top: 0.5rem; font-size: 0.8125rem; color: #78716c; }
    .actions { display: flex; gap: 0.5rem; }
    .tags { display: flex; gap: 0.375rem; margin-bottom: 1rem; flex-wrap: wrap; }
    .tag { background: #e8f4fd; color: #006EC7; padding: 0.25rem 0.625rem; border-radius: 9999px; font-size: 0.8125rem; }
    .summary { margin-bottom: 1rem; }
    .summary p { margin-top: 0.375rem; color: #57534e; }
    .content-area { margin-bottom: 1rem; }
    .doc-content { line-height: 1.7; }
    .rating-section { margin-top: 1rem; }
    .rating-section h3, .card h3 { font-size: 1rem; font-weight: 600; margin-bottom: 0.5rem; }
    .rating-stars { display: flex; align-items: center; gap: 0.25rem; }
    .star-btn { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: #d6d3d1; padding: 0; }
    .star-btn.active { color: #d97706; }
    .rating-text { margin-left: 0.75rem; font-size: 0.875rem; color: #78716c; }
  `]
})
export class DokumentDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dokumentService = inject(DokumentService);

  readonly doc = signal<DokumentDto | null>(null);
  readonly versions = signal<DokumentVersionDto[]>([]);
  readonly loading = signal(true);

  feedbackRating = 0;
  feedbackComment = '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.dokumentService.getById(id).subscribe({
        next: (d) => { this.doc.set(d); this.loading.set(false); },
        error: (err) => { console.error('Fehler beim Laden des Dokuments', err); this.loading.set(false); }
      });
      this.dokumentService.getVersions(id).subscribe({
        next: v => this.versions.set(v),
        error: (err) => console.error('Fehler beim Laden der Versionen', err)
      });
    }
  }

  publish(): void {
    this.dokumentService.publish(this.doc()!.id).subscribe({
      next: d => this.doc.set(d),
      error: (err) => console.error('Fehler beim Veroeffentlichen', err)
    });
  }

  archive(): void {
    this.dokumentService.archive(this.doc()!.id).subscribe({
      next: d => this.doc.set(d),
      error: (err) => console.error('Fehler beim Archivieren', err)
    });
  }

  deleteDoc(): void {
    if (confirm('Dokument wirklich loeschen?')) {
      this.dokumentService.delete(this.doc()!.id).subscribe({
        next: () => this.router.navigate(['/dokumente']),
        error: (err) => console.error('Fehler beim Loeschen des Dokuments', err)
      });
    }
  }

  submitFeedback(): void {
    this.dokumentService.submitFeedback(this.doc()!.id, this.feedbackRating, this.feedbackComment).subscribe({
      next: () => {
        this.dokumentService.getById(this.doc()!.id).subscribe({
          next: d => this.doc.set(d),
          error: (err) => console.error('Fehler beim Neuladen des Dokuments', err)
        });
        this.feedbackRating = 0;
        this.feedbackComment = '';
      },
      error: (err) => console.error('Fehler beim Absenden des Feedbacks', err)
    });
  }
}
