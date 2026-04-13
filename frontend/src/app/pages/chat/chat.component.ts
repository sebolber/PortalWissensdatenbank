import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ChatApiService, LlmModelDto } from '../../services/chat-api.service';
import { SuggestionResponse } from '../../models/knowledge.model';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page-header">
      <h1>KI-Kodierempfehlung</h1>
      <a routerLink="/wissen" class="btn btn-secondary">Zurueck</a>
    </div>

    <div class="card">
      <h3>Behandlungsdokument</h3>

      <div class="form-group" *ngIf="llmModels.length > 0">
        <label>LLM-Modell</label>
        <select class="form-control model-select" [(ngModel)]="selectedModelId">
          <option value="">Standard-Modell</option>
          <option *ngFor="let m of llmModels" [value]="m.id">
            {{ m.name || m.model }} ({{ m.provider }}){{ m.isActive ? ' *' : '' }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label>Dokumenttext / relevante Textstellen</label>
        <textarea class="form-control" rows="6" [(ngModel)]="dokumentText"
                  placeholder="Fuegen Sie hier den relevanten Text ein..."></textarea>
      </div>

      <div class="form-group">
        <label>Diagnosen (je Zeile eine)</label>
        <textarea class="form-control" rows="3" [(ngModel)]="diagnosenText"
                  placeholder="z.B. I50.0 Herzinsuffizienz"></textarea>
      </div>

      <div class="form-group">
        <label>Massnahmen / Prozeduren (je Zeile eine)</label>
        <textarea class="form-control" rows="3" [(ngModel)]="massnahmenText"
                  placeholder="z.B. 5-377.1 Implantation Herzschrittmacher"></textarea>
      </div>

      <button class="btn btn-primary" (click)="submit()" [disabled]="loading() || !dokumentText.trim()">
        {{ loading() ? 'Analyse laeuft...' : 'Kodierempfehlung generieren' }}
      </button>

      <div *ngIf="error()" class="error-msg" style="margin-top:1rem">
        {{ error() }}
        <div *ngIf="isContentFilterError()" class="filter-hint">
          Bitte waehlen Sie ein anderes LLM-Modell aus der Liste oben.
        </div>
      </div>
    </div>

    <!-- Ergebnis -->
    <div *ngIf="response()" class="result-section">
      <div class="card suggestion-card" *ngFor="let emp of response()!.empfehlungen; let i = index"
           [style.margin-top]="i > 0 ? '1rem' : '0'">
        <h3>Kodierempfehlung {{ response()!.empfehlungen.length > 1 ? (i + 1) : '' }}</h3>
        <div class="suggestion-content" [innerHTML]="formatResponse(emp)"></div>
      </div>

      <div class="card" style="margin-top:1rem">
        <div class="meta-info">
          <span>Modell: {{ response()!.llmModel }}</span>
          <span>Tokens: {{ response()!.tokenCount }}</span>
          <span>Audit-ID: {{ response()!.auditLogId }}</span>
        </div>
      </div>

      <div class="card" style="margin-top:1rem" *ngIf="response()!.quellen.length > 0">
        <h3>Verwendete Quellen</h3>
        <div *ngFor="let q of response()!.quellen" class="source-item">
          <a [routerLink]="'/wissen/' + q.id"><strong>{{ q.title }}</strong></a>
          <span class="badge" style="margin-left:0.5rem">{{ q.bindingLevel }}</span>
          <span *ngIf="q.bindingLevel === 'LEX_SPECIALIS'" class="lex-hint">lex specialis</span>
          <div class="match-reason">{{ q.matchReason }}</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .form-group { margin-bottom:1rem; }
    .form-group label { display:block; font-weight:500; margin-bottom:0.25rem; }
    .model-select { max-width:400px; }
    .error-msg { color:#dc2626; padding:0.75rem; background:#fef2f2; border-radius:6px; }
    .filter-hint { margin-top:0.5rem; font-weight:600; }
    .result-section { margin-top:1.5rem; }
    .suggestion-card { border-left:4px solid #2563eb; }
    .suggestion-content { white-space:pre-wrap; line-height:1.6; }
    .meta-info { margin-top:1rem; display:flex; gap:1.5rem; font-size:0.8rem; color:#78716c; }
    .source-item { padding:0.75rem 0; border-bottom:1px solid #e7e5e4; }
    .source-item:last-child { border-bottom:none; }
    .match-reason { font-size:0.8rem; color:#78716c; margin-top:0.25rem; }
    .lex-hint { font-size:0.75rem; color:#dc2626; font-weight:600; margin-left:0.5rem; }
  `]
})
export class ChatComponent implements OnInit {
  private readonly chatApi = inject(ChatApiService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly response = signal<SuggestionResponse | null>(null);

  llmModels: LlmModelDto[] = [];
  selectedModelId = '';
  dokumentText = '';
  diagnosenText = '';
  massnahmenText = '';

  ngOnInit(): void {
    this.chatApi.listLlmModels().subscribe({
      next: models => this.llmModels = models,
      error: (err) => console.error('Fehler beim Laden der LLM-Modelle', err)
    });
  }

  submit(): void {
    this.loading.set(true);
    this.error.set(null);
    this.response.set(null);

    const diagnosen = this.diagnosenText.split('\n').map(s => s.trim()).filter(Boolean);
    const massnahmen = this.massnahmenText.split('\n').map(s => s.trim()).filter(Boolean);

    this.chatApi.generateSuggestion({
      dokumentText: this.dokumentText,
      diagnosen,
      massnahmen,
      modelConfigId: this.selectedModelId || null
    }).subscribe({
      next: res => { this.response.set(res); this.loading.set(false); },
      error: err => {
        const msg = err.error?.message || err.error?.error || 'Fehler bei der KI-Analyse';
        this.error.set(msg);
        this.loading.set(false);
      }
    });
  }

  isContentFilterError(): boolean {
    const err = this.error();
    if (!err) return false;
    return err.includes('Content-Filter') || err.includes('blockiert') || err.includes('abgelehnt')
        || err.includes('Leere Antwort');
  }

  formatResponse(text: string): string {
    return text.replace(/\n/g, '<br>');
  }
}
