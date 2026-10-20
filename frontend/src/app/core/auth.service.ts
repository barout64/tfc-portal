import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Audience, MaskedPhoneResponse, Role, TokenResponse } from './models';

interface StoredSession {
  token: string;
  displayName: string;
  role: Role;
}

const STORAGE_KEY = 'tfc-portal-session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly session = signal<StoredSession | null>(this.readStoredSession());

  constructor(private http: HttpClient) {}

  get token(): string | null {
    return this.session()?.token ?? null;
  }

  get role(): Role | null {
    return this.session()?.role ?? null;
  }

  get isLoggedIn(): boolean {
    return this.session() !== null;
  }

  startRegistration(audience: Audience, tfraNumber: string): Observable<MaskedPhoneResponse> {
    return this.http.post<MaskedPhoneResponse>(`${environment.apiUrl}/auth/${audience}/register`, { tfraNumber });
  }

  completeRegistration(audience: Audience, tfraNumber: string, code: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${environment.apiUrl}/auth/${audience}/register/verify`, { tfraNumber, code })
      .pipe(tap((response) => this.storeSession(response)));
  }

  startLogin(audience: Audience, tfraNumber: string): Observable<MaskedPhoneResponse> {
    return this.http.post<MaskedPhoneResponse>(`${environment.apiUrl}/auth/${audience}/login`, { tfraNumber });
  }

  completeLogin(audience: Audience, tfraNumber: string, code: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${environment.apiUrl}/auth/${audience}/login/verify`, { tfraNumber, code })
      .pipe(tap((response) => this.storeSession(response)));
  }

  staffLogin(username: string, password: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${environment.apiUrl}/auth/staff/login`, { username, password })
      .pipe(tap((response) => this.storeSession(response)));
  }

  logout(): void {
    this.session.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  private storeSession(response: TokenResponse): void {
    const stored: StoredSession = { token: response.token, displayName: response.displayName, role: response.role };
    this.session.set(stored);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
  }

  private readStoredSession(): StoredSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as StoredSession) : null;
    } catch {
      return null;
    }
  }
}
