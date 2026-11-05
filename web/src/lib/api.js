const base = (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080').replace(/\/$/, '');
let csrf;
export class ApiError extends Error { constructor(message, status) { super(message); this.status = status; } }
export async function api(path, options = {}) {
  const method = options.method || 'GET';
  const headers = { ...options.headers };
  if (method !== 'GET') {
    if (!csrf) { const response = await fetch(`${base}/api/auth/csrf`, { credentials: 'include' }); if (!response.ok) throw new ApiError('Não foi possível iniciar uma sessão segura.', response.status); csrf = await response.json(); }
    headers[csrf.headerName] = csrf.token;
    if (!(options.body instanceof FormData) && !(options.body instanceof URLSearchParams)) headers['Content-Type'] = 'application/json';
  }
  let response;
  try { response = await fetch(`${base}/api${path}`, { ...options, headers, credentials: 'include', cache: 'no-store' }); }
  catch { throw new ApiError('Sem conexão com a API. Confira se o servidor está disponível. Suas alterações ainda não foram salvas.', 0); }
  if (!response.ok) {
    if (response.status === 403 || response.status === 401) csrf = null;
    const body = await response.json().catch(() => ({}));
    throw new ApiError(body.message || ({401:'Sua sessão terminou ou os dados de acesso não conferem.',403:'A sessão de segurança expirou. Tente novamente.',409:'Este registro mudou. Atualize os dados antes de salvar.',429:'Muitas tentativas. Aguarde cinco minutos.',413:'O arquivo excede 10 MB.'}[response.status]) || 'Não foi possível realizar a operação.', response.status);
  }
  if (path.startsWith('/auth/login') || path.startsWith('/auth/logout')) csrf = null;
  if (options.download) return response.blob();
  return response.status === 204 ? null : response.text().then(text => text ? JSON.parse(text) : null);
}
export function saveRecord(module, data, record) { return api(`/records/${module}`, { method: 'POST', body: JSON.stringify({ id: record?.id || crypto.randomUUID(), version: record?.version || 0, data }) }); }
export function downloadBlob(blob, name) { const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = name; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); }
