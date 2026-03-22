import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Liest das Portal-JWT aus dem localStorage und haengt es an alle API-Requests an.
 * Wenn die App im Portal-iframe laeuft, werden absolute /api/... URLs auf den
 * app-proxy Pfad umgeschrieben, damit sie bei dieser App landen (nicht bei PortalCore).
 */
export const portalAuthInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.includes('api/')) {
    return next(req);
  }

  let url = req.url;

  // Im iframe: /api/... auf app-proxy umschreiben
  if (window.self !== window.top && url.startsWith('/api/')) {
    const baseElement = document.querySelector('base');
    const baseHref = baseElement?.getAttribute('href') || '/';
    if (baseHref !== '/' && baseHref !== './') {
      // baseHref ist z.B. "/app-proxy/portal-app-portalwissensdatenbank/8080/"
      url = baseHref + url.substring(1); // "/api/foo" -> "{baseHref}api/foo"
    }
  }

  const token = localStorage.getItem('portal_token');
  if (token) {
    return next(req.clone({
      url,
      setHeaders: { Authorization: `Bearer ${token}` }
    }));
  }

  if (url !== req.url) {
    return next(req.clone({ url }));
  }
  return next(req);
};
