import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { KnowledgeApiService } from '../../services/knowledge-api.service';
import { KnowledgeItemDto, KnowledgeSubArticleTreeDto } from '../../models/knowledge.model';

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
          <div><strong>Typ:</strong> {{ typeLabel(item()!.knowledgeType) }}</div>
          <div><strong>Verbindlichkeit:</strong> {{ item()!.bindingLevel }}</div>
          <div><strong>Quelle:</strong> {{ item()!.sourceReference || '-' }}</div>
          <div><strong>Erstellt:</strong> {{ item()!.createdAt | date:'dd.MM.yyyy HH:mm' }}</div>
          <div><strong>Aktualisiert:</strong> {{ item()!.updatedAt | date:'dd.MM.yyyy HH:mm' }}</div>
          <div *ngIf="item()!.knowledgeType === 'SEG4'"><strong>SEG4 Empfehlungen:</strong> {{ item()!.seg4RecommendationCount }}</div>
          <div *ngIf="item()!.productName"><strong>Produkt:</strong> {{ item()!.productName }}</div>
          <div *ngIf="item()!.productVersionLabel"><strong>Version:</strong> {{ item()!.productVersionLabel }}</div>
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

      <!-- SubArticle-Baum (Inhaltsverzeichnis fuer Handbuecher) -->
      <div class="card" style="margin-top:1rem" *ngIf="sections().length > 0">
        <h3>Inhaltsverzeichnis</h3>
        <ng-container *ngTemplateOutlet="sectionTree; context: { $implicit: sections() }"></ng-container>
      </div>

      <!-- Ausgewaehlter Abschnitt -->
      <div class="card" style="margin-top:1rem" *ngIf="selectedSection()">
        <h3>{{ selectedSection()!.sectionNumber ? selectedSection()!.sectionNumber + ' ' : '' }}{{ selectedSection()!.heading }}</h3>
        <p *ngIf="selectedSection()!.contentPreview">{{ selectedSection()!.contentPreview }}</p>
      </div>
    </div>

    <!-- Rekursives Template fuer den Abschnittsbaum -->
    <ng-template #sectionTree let-nodes>
      <ul class="section-tree">
        <li *ngFor="let node of nodes">
          <a class="section-link" (click)="selectSection(node)" [class.active]="selectedSection()?.id === node.id">
            <span class="section-number" *ngIf="node.sectionNumber">{{ node.sectionNumber }}</span>
            {{ node.heading }}
          </a>
          <ng-container *ngIf="node.children?.length > 0">
            <ng-container *ngTemplateOutlet="sectionTree; context: { $implicit: node.children }"></ng-container>
          </ng-container>
        </li>
      </ul>
    </ng-template>
  `,
  styles: [`
    .detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:0.75rem; }
    .section-tree { list-style:none; padding-left:1.25rem; margin:0.25rem 0; }
    .section-tree:first-child { padding-left:0; }
    .section-link { cursor:pointer; color:#2563eb; text-decoration:none; display:block; padding:0.15rem 0.5rem; border-radius:4px; }
    .section-link:hover { background:#f1f5f9; }
    .section-link.active { background:#dbeafe; font-weight:500; }
    .section-number { color:#64748b; margin-right:0.5rem; font-size:0.875rem; }
  `]
})
export class KnowledgeDetailComponent implements OnInit {
  private readonly api = inject(KnowledgeApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly item = signal<KnowledgeItemDto | null>(null);
  readonly loading = signal(true);
  readonly sections = signal<KnowledgeSubArticleTreeDto[]>([]);
  readonly selectedSection = signal<KnowledgeSubArticleTreeDto | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getById(id).subscribe({
      next: item => {
        this.item.set(item);
        this.loading.set(false);
        // SubArticle-Baum laden (fuer alle Typen, nicht nur HANDBUCH)
        this.api.getSubArticleTree(id).subscribe({
          next: tree => this.sections.set(tree),
          error: () => {} // kein Fehler wenn leer
        });
      },
      error: (err) => { console.error('Fehler beim Laden des Wissensobjekts', err); this.loading.set(false); }
    });
  }

  selectSection(section: KnowledgeSubArticleTreeDto): void {
    this.selectedSection.set(section);
  }

  deleteItem(): void {
    if (!confirm('Wissensobjekt wirklich loeschen?')) return;
    this.api.delete(this.item()!.id).subscribe({
      next: () => this.router.navigate(['/wissen']),
      error: (err) => console.error('Fehler beim Loeschen des Wissensobjekts', err)
    });
  }

  typeLabel(type: string): string {
    switch (type) {
      case 'SEG4': return 'SEG4';
      case 'ARTICLE': return 'Artikel';
      case 'GUIDELINE': return 'Leitlinie';
      case 'HANDBUCH': return 'Handbuch';
      default: return type;
    }
  }
}
