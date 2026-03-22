import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/startseite/startseite.component').then(m => m.StartseiteComponent) },
  { path: 'suche', loadComponent: () => import('./pages/suche/suche.component').then(m => m.SucheComponent) },
  { path: 'kategorien', loadComponent: () => import('./pages/kategorien/kategorien.component').then(m => m.KategorienComponent) },
  { path: 'konfiguration', loadComponent: () => import('./pages/konfiguration/konfiguration.component').then(m => m.KonfigurationComponent) },
  { path: 'wissen', loadComponent: () => import('./pages/knowledge/knowledge-list.component').then(m => m.KnowledgeListComponent) },
  { path: 'wissen/:id', loadComponent: () => import('./pages/knowledge/knowledge-detail.component').then(m => m.KnowledgeDetailComponent) },
  { path: 'seg4-import', loadComponent: () => import('./pages/seg4-import/seg4-import.component').then(m => m.Seg4ImportComponent) },
  { path: 'kodierempfehlung', loadComponent: () => import('./pages/chat/chat.component').then(m => m.ChatComponent) },
  { path: 'dokument-kodierung', loadComponent: () => import('./pages/dokument-kodierung/dokument-kodierung.component').then(m => m.DokumentKodierungComponent) },
  { path: '**', redirectTo: '' },
];
