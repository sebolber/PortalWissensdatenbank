import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ImportJobService } from './services/import-job.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="app-layout" [class.embedded]="embedded">
      <aside class="sidebar" [class.collapsed]="sidebarCollapsed" *ngIf="!embedded">
        <div class="sidebar-header">
          <div class="sidebar-logo">WB</div>
          <span class="sidebar-title" *ngIf="!sidebarCollapsed">Wissensdatenbank</span>
        </div>
        <nav class="sidebar-nav">
          <div class="nav-section" *ngIf="!sidebarCollapsed">Navigation</div>
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}" class="nav-item" title="Startseite">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
            <span *ngIf="!sidebarCollapsed">Startseite</span>
          </a>
          <a routerLink="/dokumente" routerLinkActive="active" class="nav-item" title="Dokumente">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
            <span *ngIf="!sidebarCollapsed">Dokumente</span>
          </a>
          <a routerLink="/suche" routerLinkActive="active" class="nav-item" title="Suche">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            <span *ngIf="!sidebarCollapsed">Suche</span>
          </a>
          <a routerLink="/wissen" routerLinkActive="active" class="nav-item" title="Wissensobjekte">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 014 4v14a3 3 0 00-3-3H2z"/><path d="M22 3h-6a4 4 0 00-4 4v14a3 3 0 013-3h7z"/></svg>
            <span *ngIf="!sidebarCollapsed">Wissensobjekte</span>
          </a>
          <a routerLink="/kodierempfehlung" routerLinkActive="active" class="nav-item" title="KI-Kodierempfehlung">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
            <span *ngIf="!sidebarCollapsed">KI-Kodierempfehlung</span>
          </a>
          <div class="nav-section" *ngIf="!sidebarCollapsed">Aktionen</div>
          <a routerLink="/dokumente/neu" routerLinkActive="active" class="nav-item" title="Neues Dokument">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            <span *ngIf="!sidebarCollapsed">Neues Dokument</span>
          </a>
          <a routerLink="/seg4-import" routerLinkActive="active" class="nav-item" title="SEG4 Import">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            <span *ngIf="!sidebarCollapsed">SEG4 Import</span>
          </a>
          <a routerLink="/kategorien" routerLinkActive="active" class="nav-item" title="Kategorien">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
            <span *ngIf="!sidebarCollapsed">Kategorien</span>
          </a>
          <a routerLink="/konfiguration" routerLinkActive="active" class="nav-item" title="Konfiguration">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>
            <span *ngIf="!sidebarCollapsed">Konfiguration</span>
          </a>
        </nav>
        <button class="sidebar-toggle" (click)="sidebarCollapsed = !sidebarCollapsed">
          <svg *ngIf="!sidebarCollapsed" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="11 17 6 12 11 7"/></svg>
          <svg *ngIf="sidebarCollapsed" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="13 7 18 12 13 17"/></svg>
        </button>
      </aside>
      <main class="main-content" [class.expanded]="sidebarCollapsed" [class.embedded]="embedded">
        <!-- Globale Import-Status-Leiste -->
        <div *ngIf="importJobs.hasActiveJobs()" class="import-banner">
          <div class="import-banner-content">
            <span class="import-spinner"></span>
            <span>{{ importJobs.activeJobs().length === 1
              ? 'Import laeuft: ' + importJobs.activeJobs()[0].fileName
              : importJobs.activeJobs().length + ' Importe laufen...' }}</span>
          </div>
        </div>
        <div *ngFor="let job of importJobs.recentDoneJobs()" class="import-banner"
             [class.import-banner-success]="job.status === 'done'"
             [class.import-banner-error]="job.status === 'error'">
          <div class="import-banner-content">
            <span *ngIf="job.status === 'done'">Import abgeschlossen: {{ job.fileName }}
              ({{ job.result?.recommendationCount }} Empfehlungen)
              <a [routerLink]="'/wissen/' + job.result?.id" class="banner-link">Anzeigen</a>
            </span>
            <span *ngIf="job.status === 'error'">Import fehlgeschlagen: {{ job.fileName }}
              – {{ job.errorMessage }}
            </span>
            <button class="banner-dismiss" (click)="importJobs.dismissJob(job.id)">&times;</button>
          </div>
        </div>
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .app-layout { display: flex; min-height: 100vh; }
    .app-layout.embedded { display: block; }

    .sidebar {
      width: 240px; background: #fff; border-right: 1px solid #e5e7eb;
      display: flex; flex-direction: column; position: fixed; top: 0; left: 0;
      height: 100vh; z-index: 40; transition: width 0.2s ease;
    }
    .sidebar.collapsed { width: 56px; }

    .sidebar-header {
      padding: 1rem; border-bottom: 1px solid #e5e7eb;
      display: flex; align-items: center; gap: 0.625rem; min-height: 56px;
    }
    .sidebar-logo {
      width: 32px; height: 32px; background: #006EC7; border-radius: 0.5rem;
      display: flex; align-items: center; justify-content: center;
      color: #fff; font-size: 0.6875rem; font-weight: 700; flex-shrink: 0;
    }
    .sidebar-title { font-size: 0.9375rem; font-weight: 600; color: #1f2937; white-space: nowrap; }

    .sidebar-nav { flex: 1; padding: 0.5rem; overflow-y: auto; }
    .nav-section {
      padding: 1rem 0.75rem 0.375rem; font-size: 0.6875rem; font-weight: 600;
      text-transform: uppercase; letter-spacing: 0.05em; color: #9ca3af;
    }
    .nav-item {
      display: flex; align-items: center; gap: 0.625rem;
      padding: 0.5rem 0.75rem; border-radius: 0.5rem; color: #6b7280;
      text-decoration: none; font-size: 0.8125rem; font-weight: 500;
      transition: all 0.15s; white-space: nowrap;
    }
    .nav-item:hover { background: #f3f4f6; color: #1f2937; }
    .nav-item.active { background: #eff6ff; color: #006EC7; font-weight: 600; }
    .nav-item svg { width: 18px; height: 18px; flex-shrink: 0; }

    .sidebar-toggle {
      margin: 0.5rem; padding: 0.5rem; border: none; background: transparent;
      color: #9ca3af; cursor: pointer; border-radius: 0.375rem;
      display: flex; align-items: center; justify-content: center;
    }
    .sidebar-toggle:hover { background: #f3f4f6; color: #1f2937; }
    .sidebar-toggle svg { width: 18px; height: 18px; }

    .main-content {
      flex: 1; margin-left: 240px; transition: margin-left 0.2s ease;
      background: #f5f5f4; min-height: 100vh; padding: 1.5rem;
    }
    .main-content.expanded { margin-left: 56px; }
    .main-content.embedded { margin-left: 0; }

    .import-banner {
      background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 0.5rem;
      padding: 0.625rem 1rem; margin-bottom: 0.75rem;
    }
    .import-banner-success { background: #f0fdf4; border-color: #86efac; }
    .import-banner-error { background: #fef2f2; border-color: #fca5a5; }
    .import-banner-content {
      display: flex; align-items: center; gap: 0.5rem; font-size: 0.8125rem;
    }
    .import-spinner {
      width: 14px; height: 14px; border: 2px solid #bfdbfe; border-top-color: #3b82f6;
      border-radius: 50%; animation: spin 0.8s linear infinite; flex-shrink: 0;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .banner-link { color: #2563eb; font-weight: 600; text-decoration: underline; margin-left: 0.25rem; }
    .banner-dismiss {
      margin-left: auto; background: none; border: none; font-size: 1.125rem;
      cursor: pointer; color: #9ca3af; padding: 0 0.25rem;
    }
    .banner-dismiss:hover { color: #1f2937; }
  `]
})
export class AppComponent implements OnInit {
  private readonly router = inject(Router);
  readonly importJobs = inject(ImportJobService);
  sidebarCollapsed = false;
  embedded = false;

  ngOnInit(): void {
    this.embedded = window.self !== window.top;

    const urlParams = new URLSearchParams(window.location.search);
    const page = urlParams.get('page');
    if (page) {
      this.router.navigateByUrl(page);
    }
  }
}
