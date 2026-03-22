import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Liest das Portal-JWT aus dem localStorage und haengt es an alle API-Requests an.
 * Wenn die App ueber den Portal-Proxy laeuft (iframe ODER direkte Proxy-URL),
 * werden absolute /api/... URLs auf den app-proxy Pfad umgeschrieben,
 * damit sie bei dieser App landen (nicht bei PortalCore).
 */
export const portalAuthInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.includes('api/')) {
    return next(req);
  }

  let url = req.url;

  // /api/... URLs auf app-proxy umschreiben wenn wir ueber den Proxy laufen
  if (url.startsWith('/api/')) {
    const proxyPrefix = getProxyPrefix();
    if (proxyPrefix) {
      url = proxyPrefix + url.substring(1); // "/api/foo" -> "/app-proxy/.../api/foo"
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

/**
 * Ermittelt den Proxy-Prefix aus base href oder window.location.
 * Funktioniert sowohl im iframe als auch bei direktem Proxy-URL-Zugriff.
 */
function getProxyPrefix(): string | null {
  // 1. Versuch: base href wurde von nginx sub_filter umgeschrieben
  const baseElement = document.querySelector('base');
  const rawHref = baseElement?.getAttribute('href') || '/';
  if (rawHref !== '/' && rawHref !== './') {
    // baseHref ist z.B. "/app-proxy/portal-app-portalwissensdatenbank/8080/"
    if (rawHref.includes('/app-proxy/')) {
      return rawHref;
    }
  }

  // 2. Versuch: Proxy-Pfad aus der aktuellen URL ableiten
  const match = window.location.pathname.match(/^(\/app-proxy\/[^/]+\/\d+\/)/);
  if (match) {
    return match[1];
  }

  return null;
}
