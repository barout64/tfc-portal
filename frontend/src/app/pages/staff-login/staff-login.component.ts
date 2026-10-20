import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-staff-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="panel">
      <h1>Staff sign in</h1>
      <label>Username</label>
      <input [(ngModel)]="username" />
      <label>Password</label>
      <input type="password" [(ngModel)]="password" (keyup.enter)="login()" />
      <button class="btn" [disabled]="loading()" (click)="login()">Sign in</button>
      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </div>
  `,
  styles: [
    `
      .panel { max-width: 380px; margin: 48px auto; padding: 24px; border: 1px solid #ddd; border-radius: 10px; }
      label { display: block; margin: 14px 0 6px; font-size: 13px; color: #555; }
      input { width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 6px; box-sizing: border-box; }
      .btn { margin-top: 18px; width: 100%; padding: 11px; border: 0; border-radius: 6px; background: #0a7d32; color: #fff; cursor: pointer; }
      .error { color: #b00020; margin-top: 12px; }
    `
  ]
})
export class StaffLoginComponent {
  username = '';
  password = '';
  readonly loading = signal(false);
  readonly error = signal('');

  constructor(private auth: AuthService, private router: Router) {}

  login(): void {
    this.error.set('');
    this.loading.set(true);
    this.auth.staffLogin(this.username, this.password).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl('/staff');
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Invalid username or password.');
      }
    });
  }
}
