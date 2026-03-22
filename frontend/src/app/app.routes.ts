import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/startseite/startseite.component').then(m => m.StartseiteComponent) },
  { path: 'dokumente', loadComponent: () => import('./pages/dokumente/dokument-list.component').then(m => m.DokumentListComponent) },
  { path: 'dokumente/neu', loadComponent: () => import('./pages/dokumente/dokument-form.component').then(m => m.DokumentFormComponent) },
  { path: 'dokumente/:id', loadComponent: () => import('./pages/dokumente/dokument-detail.component').then(m => m.DokumentDetailComponent) },
  { path: 'dokumente/:id/bearbeiten', loadComponent: () => import('./pages/dokumente/dokument-form.component').then(m => m.DokumentFormComponent) },
  { path: 'suche', loadComponent: () => import('./pages/suche/suche.component').then(m => m.SucheComponent) },
  { path: 'kategorien', loadComponent: () => import('./pages/kategorien/kategorien.component').then(m => m.KategorienComponent) },
  { path: 'konfiguration', loadComponent: () => import('./pages/konfiguration/konfiguration.component').then(m => m.KonfigurationComponent) },
  { path: 'wissen', loadComponent: () => import('./pages/knowledge/knowledge-list.component').then(m => m.KnowledgeListComponent) },
  { path: 'wissen/:id', loadComponent: () => import('./pages/knowledge/knowledge-detail.component').then(m => m.KnowledgeDetailComponent) },
  { path: 'seg4-import', loadComponent: () => import('./pages/seg4-import/seg4-import.component').then(m => m.Seg4ImportComponent) },
  { path: 'kodierempfehlung', loadComponent: () => import('./pages/chat/chat.component').then(m => m.ChatComponent) },
  { path: '**', redirectTo: '' },
];
