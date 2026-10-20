import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PortalApiService } from '../../core/portal-api.service';
import { SaleRequestResponse } from '../../core/models';

@Component({
  selector: 'app-staff-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <h1>Complete a sale request</h1>
      <label>Reference</label>
      <input [(ngModel)]="reference" placeholder="SR-XXXXXXXX-XXX" (keyup.enter)="find()" />
      <button class="btn secondary" (click)="find()">Look up</button>

      @if (current()) {
        <div class="card">
          <div class="head">
            <strong>{{ current()!.reference }}</strong>
            <span class="badge">{{ current()!.status }}</span>
          </div>
          <ul>
            @for (item of current()!.items; track item.productName) {
              <li>{{ item.bagCount }} × {{ item.productName }}</li>
            }
          </ul>
          <p>
            Total {{ current()!.totalAmount | number }} · Farmer pays {{ current()!.farmerCashAmount | number }} · Subsidy
            {{ current()!.subsidyAmount | number }}
          </p>

          @if (current()!.status === 'SUBMITTED') {
            <button class="btn" (click)="start()">Mark ready for pickup</button>
          }

          @if (current()!.status === 'SUBMITTED' || current()!.status === 'READY_FOR_PICKUP') {
            <div class="section">
              <button class="btn secondary" (click)="requestOtp()">Request farmer OTP from TFRA</button>
              @if (otpMessage()) {
                <p class="note">{{ otpMessage() }}</p>
              }

              <label>Bag QR codes (one per line)</label>
              <textarea [(ngModel)]="bagQrCodesText" rows="3" placeholder="QR-0001&#10;QR-0002"></textarea>
              <label>Farmer's OTP</label>
              <input [(ngModel)]="otp" maxlength="6" placeholder="000000" />
              <button class="btn" (click)="complete()">Scan &amp; confirm sale</button>
            </div>
          }
        </div>
      }

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </div>
  `,
  styles: [
    `
      .page { max-width: 600px; margin: 0 auto; padding: 24px 16px; }
      label { display: block; margin: 12px 0 4px; font-size: 13px; color: #555; }
      input, textarea { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 6px; box-sizing: border-box; }
      .btn { margin-top: 12px; padding: 10px 16px; border: 0; border-radius: 6px; background: #0a7d32; color: #fff; cursor: pointer; }
      .btn.secondary { background: #eee; color: #222; }
      .card { margin-top: 20px; border: 1px solid #ddd; border-radius: 10px; padding: 16px; }
      .head { display: flex; justify-content: space-between; }
      .badge { padding: 2px 10px; border-radius: 10px; font-size: 12px; background: #e8f0fe; color: #1a4fa0; }
      .section { margin-top: 16px; border-top: 1px solid #eee; padding-top: 12px; }
      .note { color: #157a33; }
      .error { color: #b00020; margin-top: 12px; }
    `
  ]
})
export class StaffDashboardComponent {
  reference = '';
  bagQrCodesText = '';
  otp = '';

  readonly current = signal<SaleRequestResponse | null>(null);
  readonly error = signal('');
  readonly otpMessage = signal('');

  constructor(private api: PortalApiService) {}

  find(): void {
    this.error.set('');
    this.otpMessage.set('');
    this.api.staffFindSaleRequest(this.reference).subscribe({
      next: (res) => this.current.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Sale request not found.')
    });
  }

  start(): void {
    this.api.staffStartPickup(this.reference).subscribe({
      next: (res) => this.current.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not update this request.')
    });
  }

  requestOtp(): void {
    this.api.staffRequestOtp(this.reference).subscribe({
      next: (res) => this.otpMessage.set(res.message),
      error: (err) => this.error.set(err.error?.message ?? 'Could not request an OTP.')
    });
  }

  complete(): void {
    const bagQrCodes = this.bagQrCodesText
      .split('\n')
      .map((code) => code.trim())
      .filter((code) => code.length > 0);
    this.error.set('');
    this.api.staffComplete(this.reference, bagQrCodes, this.otp).subscribe({
      next: (res) => this.current.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not complete this sale.')
    });
  }
}
