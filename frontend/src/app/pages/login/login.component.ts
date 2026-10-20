import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { Audience } from '../../core/models';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="panel">
      <h1>{{ audienceLabel() }} sign in</h1>

      @if (!codeSent()) {
        <p>Enter your TFRA {{ audienceLabel() }} number to receive a login code.</p>
        <label>TFRA number</label>
        <input [(ngModel)]="tfraNumber" placeholder="e.g. 81180400010" />
        <button class="btn" [disabled]="loading()" (click)="start()">Send login code</button>
      } @else {
        <p>{{ startMessage() }} (phone ending {{ maskedPhone() }})</p>
        <label>6-digit code</label>
        <input [(ngModel)]="code" maxlength="6" placeholder="000000" />
        <button class="btn" [disabled]="loading()" (click)="verify()">Sign in</button>
      }

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </div>
  `,
  styles: [
    `
      .panel { max-width: 420px; margin: 48px auto; padding: 24px; border: 1px solid #ddd; border-radius: 10px; }
      label { display: block; margin: 14px 0 6px; font-size: 13px; color: #555; }
      input { width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 6px; box-sizing: border-box; }
      .btn { margin-top: 18px; width: 100%; padding: 11px; border: 0; border-radius: 6px; background: #0a7d32; color: #fff; cursor: pointer; }
      .error { color: #b00020; margin-top: 12px; }
    `
  ]
})
export class LoginComponent {
  tfraNumber = '';
  code = '';

  readonly codeSent = signal(false);
  readonly startMessage = signal('');
  readonly maskedPhone = signal('');
  readonly loading = signal(false);
  readonly error = signal('');

  private audience: Audience;

  constructor(private route: ActivatedRoute, private router: Router, private auth: AuthService) {
    this.audience = this.route.snapshot.data['audience'];
  }

  audienceLabel(): string {
    return this.audience === 'farmers' ? 'Farmer' : 'Agro-dealer';
  }

  start(): void {
    this.error.set('');
    this.loading.set(true);
    this.auth.startLogin(this.audience, this.tfraNumber).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.codeSent.set(true);
        this.startMessage.set(res.message);
        this.maskedPhone.set(res.maskedPhone);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Something went wrong.');
      }
    });
  }

  verify(): void {
    this.error.set('');
    this.loading.set(true);
    this.auth.completeLogin(this.audience, this.tfraNumber, this.code).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl(this.audience === 'farmers' ? '/catalog' : '/agro-dealers/me');
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Something went wrong.');
      }
    });
  }
}
