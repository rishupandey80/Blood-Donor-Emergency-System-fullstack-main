const API_BASE = (process.env.REACT_APP_API_URL || 'http://localhost:8080').replace(/\/$/, '');

const TOKEN_KEY = 'bb_jwt_token';

function buildQuery(params = {}, includeEmpty = false) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && (includeEmpty || String(value) !== '')) {
      query.set(key, String(value));
    }
  });
  return query.toString();
}

async function request(path, options = {}) {
  const { method = 'GET', body, auth = true } = options;
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  if (auth) {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      ...(body !== undefined ? { body: JSON.stringify(body) } : {})
    });
  } catch (error) {
    throw new Error('Backend se connection nahi ho pa raha. Check karo Spring Boot localhost:8080 par run ho raha hai ya nahi.');
  }

  const raw = await response.text();
  let data = raw;
  if (raw) {
    try { data = JSON.parse(raw); } catch { /* plain text response */ }
  } else {
    data = null;
  }

  if (!response.ok) {
    const message = typeof data === 'string'
      ? data
      : data?.message || data?.error || data?.detail || data?.title || `Request failed (${response.status})`;
    throw new Error(message);
  }

  return data;
}

function get(path, params, includeEmpty = false) {
  const query = buildQuery(params, includeEmpty);
  return request(`${path}${query ? `?${query}` : ''}`);
}

function post(path, body, auth = true) {
  return request(path, { method: 'POST', body, auth });
}

function put(path, body) {
  return request(path, { method: 'PUT', ...(body !== undefined ? { body } : {}) });
}

export const api = {
  tokenKey: TOKEN_KEY,

  sendSignupOtp: (payload) => post('/api/auth/signup/send-otp', payload, false),
  verifySignupOtp: (payload) => post('/api/auth/signup/verify-otp', payload, false),
  login: (payload) => post('/api/auth/login', payload, false),
  forgot: (email) => post('/api/auth/forgot-password', { email }, false),
  reset: (payload) => post('/api/auth/reset-password', payload, false),

  me: () => get('/api/users/me'),
  updateMe: (payload) => put('/api/users/me', payload),
  changePassword: (payload) => put('/api/users/me/password', payload),
  deleteAccount: () => request('/api/users/me', { method: 'DELETE' }),

  // Backend mapping is singular: @RequestMapping("/api/request").
  addRequest: (payload) => post('/api/request', payload),
  requests: () => get('/api/request/active'),
  allRequests: () => get('/api/request'),
  myRequests: () => get('/api/request/mine'),
  acceptedRequests: () => get('/api/request/accepted-by-me'),
  requestAction: (id, action) => {
    if (!['accept', 'fulfill', 'cancel'].includes(action)) {
      return Promise.reject(new Error('Unsupported request action.'));
    }
    return put(`/api/request/${encodeURIComponent(id)}/${action}`);
  },

  donors: (filters = {}) => {
    const bloodGroup = (filters.bloodGroup || '').trim();
    const city = (filters.city || '').trim();
    if (bloodGroup && city) return get('/api/donors/search', { bloodGroup, city });
    if (bloodGroup) return get('/api/donors/search/bloodgroup', { bloodGroup });
    if (city) return get('/api/donors/search/city', { city });
    // The backend intentionally has no public all-donors endpoint.
    return Promise.resolve([]);
  },
  allDonors: () => get('/api/donors'),
  donorMe: () => get('/api/donors/me'),
  addDonor: (payload) => post('/api/donors/add', payload),
  donorAvailability: (donorId, available) => put(`/api/donors/update-availability?${buildQuery({ donorId, available })}`),
  donorVerify: (id, action) => put(`/api/donors/${encodeURIComponent(id)}/${action === 'verify' ? 'verify' : 'reject'}`),

  hospitals: (city = '') => city ? get('/api/hospitals/search', { city }) : get('/api/hospitals'),
  registerHospital: (payload) => post('/api/hospitals/register', payload),
  hospitalVerify: (id, action) => put(`/api/hospitals/${encodeURIComponent(id)}/${action === 'verify' ? 'verify' : 'reject'}`),

  banks: (city = '') => city ? get('/api/bloodbanks/search', { city }) : get('/api/bloodbanks'),
  registerBank: (payload) => post('/api/bloodbanks/register', payload),
  bankVerify: (id, action) => put(`/api/bloodbanks/${encodeURIComponent(id)}/${action === 'verify' ? 'verify' : 'reject'}`),
  bankStock: () => get('/api/bloodbanks/stock/mine'),
  stockSearch: (bloodGroup = '', city = '') => get('/api/bloodbanks/stock/search', { bloodGroup, city }, true),
  updateStock: (payload) => put('/api/bloodbanks/stock', payload),

  notifications: () => get('/api/notifications'),
  unreadCount: () => get('/api/notifications/unread-count'),
  markRead: (id) => put(`/api/notifications/${encodeURIComponent(id)}/read`),
  markAllRead: () => put('/api/notifications/read-all'),

  users: (role = '') => get('/api/admin/users', role ? { role } : {}),
  userAction: (id, action) => {
    if (!['ban', 'unban'].includes(action)) return Promise.reject(new Error('Unsupported user action.'));
    return put(`/api/admin/users/${encodeURIComponent(id)}/${action}`);
  }
};

export function normalizeList(value) {
  if (Array.isArray(value)) return value;
  if (Array.isArray(value?.data)) return value.data;
  if (Array.isArray(value?.content)) return value.content;
  if (Array.isArray(value?.items)) return value.items;
  if (Array.isArray(value?.results)) return value.results;
  return [];
}
