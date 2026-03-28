import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { KnowledgeApiService } from '../../services/knowledge-api.service';
import { KnowledgeItemDto } from '../../models/knowledge.model';

@Component({
  selector: 'app-knowledge-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div *ngIf="loading()" style="text-align:center;padding:2rem">Laden...</div>
    <div *ngIf="!loading() && item()">
      <div class="page-header">
        <h1>{{ item()!.title }}</h1>
        <div style="display:flex;gap:0.5rem">
          <a routerLink="/wissen" class="btn btn-secondary">Zurueck</a>
          <button class="btn btn-danger" (click)="deleteItem()">Loeschen</button>
        </div>
      </div>

      <div class="card">
        <div class="detail-grid">
          <div><strong>Typ:</strong> {{ item()!.knowledgeType }}</div>
          <div><strong>Verbindlichkeit:</strong> {{ item()!.bindingLevel }}</div>
          <div><strong>Quelle:</strong> {{ item()!.sourceReference || '-' }}</div>
          <div><strong>Erstellt:</strong> {{ item()!.createdAt | date:'dd.MM.yyyy HH:mm' }}</div>
          <div><strong>Aktualisiert:</strong> {{ item()!.updatedAt | date:'dd.MM.yyyy HH:mm' }}</div>
          <div><strong>SEG4 Empfehlungen:</strong> {{ item()!.seg4RecommendationCount }}</div>
        </div>
      </div>

      <div class="card" style="margin-top:1rem" *ngIf="item()!.keywords">
        <h3>Schlagworte</h3>
        <p>{{ item()!.keywords }}</p>
      </div>

      <div class="card" style="margin-top:1rem" *ngIf="item()!.summary">
        <h3>Zusammenfassung</h3>
        <p>{{ item()!.summary }}</p>
      </div>

      <div class="card" style="margin-top:1rem" *ngIf="item()!.tags.length > 0">
        <h3>Tags</h3>
        <span *ngFor="let tag of item()!.tags" class="badge" style="margin-right:0.5rem">{{ tag }}</span>
      </div>
    </div>
  `,
  styles: [`
    .detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:0.75rem; }
  `]
})
export class KnowledgeDetailComponent implements OnInit {
  private readonly api = inject(KnowledgeApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly item = signal<KnowledgeItemDto | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getById(id).subscribe({
      next: item => { this.item.set(item); this.loading.set(false); },
      error: (err) => { console.error('Fehler beim Laden des Wissensobjekts', err); this.loading.set(false); }
    });
  }

  deleteItem(): void {
    if (!confirm('Wissensobjekt wirklich loeschen?')) return;
    this.api.delete(this.item()!.id).subscribe({
      next: () => this.router.navigate(['/wissen']),
      error: (err) => console.error('Fehler beim Loeschen des Wissensobjekts', err)
    });
  }
}
