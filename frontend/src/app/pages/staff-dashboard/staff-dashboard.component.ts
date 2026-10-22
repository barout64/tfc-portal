import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PortalApiService } from '../../core/portal-api.service';
import { SaleRequestResponse } from '../../core/models';

@Component({
  selector: 'app-staff-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page">
      <h1>Complete a sale request</h1>
      <p><a routerLink="/staff/dealer-orders">Agro-dealer inquiries &amp; orders →</a></p>
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

          <ol class="steps">
            <li [class.done]="current()!.status !== 'SUBMITTED'">Farmer has paid {{ current()!.farmerCashAmount | number }} in cash → open the request</li>
            <li [class.done]="current()!.submittedToTfra">Scan all {{ current()!.totalBags }} bag QR codes and submit the sale to TFRA</li>
            <li [class.done]="otpRequested() || current()!.status === 'COMPLETED'">Ask TFRA to text the farmer an OTP</li>
            <li [class.done]="current()!.status === 'COMPLETED'">Enter the OTP the farmer reads out → sale confirmed, stock out</li>
          </ol>

          @if (current()!.status === 'SUBMITTED') {
            <button class="btn" (click)="start()">1 · Farmer has paid — open request</button>
          }

          @if (current()!.status === 'READY_FOR_PICKUP' && !current()!.submittedToTfra) {
            <div class="section">
              <label>2 · Bag QR codes — scan {{ current()!.totalBags }}, one per line ({{ scannedCount() }} scanned)</label>
              <textarea [(ngModel)]="bagQrCodesText" rows="5" placeholder="QR-0001&#10;QR-0002"></textarea>
              <button class="btn" (click)="submit()">Submit sale to TFRA</button>
            </div>
          }

          @if (current()!.status === 'READY_FOR_PICKUP' && current()!.submittedToTfra) {
            <div class="section">
              <button class="btn secondary" (click)="requestOtp()">3 · Send OTP to farmer</button>
              @if (otpMessage()) {
                <p class="note">{{ otpMessage() }}</p>
              }
              <label>4 · Farmer's OTP</label>
              <input [(ngModel)]="otp" maxlength="8" placeholder="000000" />
              <button class="btn" [disabled]="!otp" (click)="complete()">Confirm sale</button>
            </div>
          }

          @if (current()!.status === 'COMPLETED') {
            <p class="note">Completed · TFRA reference {{ current()!.tfraSaleReference }}</p>
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
      .steps { padding-left: 20px; color: #555; font-size: 14px; }
      .steps li.done { color: #157a33; text-decoration: line-through; }
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
  readonly otpRequested = signal(false);

  constructor(private api: PortalApiService) {}

  scannedCount(): number {
    return this.parseCodes().length;
  }

  find(): void {
    this.reference = this.reference.trim();
    this.error.set('');
    this.otpMessage.set('');
    this.otpRequested.set(false);
    this.bagQrCodesText = '';
    this.otp = '';
    this.api.staffFindSaleRequest(this.reference).subscribe({
      next: (res) => this.current.set(res),
      error: (err) => {
        this.current.set(null);
        this.error.set(err.error?.message ?? 'Sale request not found.');
      }
    });
  }

  start(): void {
    this.error.set('');
    this.api.staffStartPickup(this.reference).subscribe({
      next: (res) => this.current.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not update this request.')
    });
  }

  submit(): void {
    this.error.set('');
    this.api.staffSubmitToTfra(this.reference, this.parseCodes()).subscribe({
      next: (res) => this.current.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'TFRA did not accept these bag QR codes.')
    });
  }

  requestOtp(): void {
    this.error.set('');
    this.api.staffRequestOtp(this.reference).subscribe({
      next: (res) => {
        this.otpRequested.set(true);
        this.otpMessage.set(res.message);
      },
      error: (err) => this.error.set(err.error?.message ?? 'Could not request an OTP.')
    });
  }

  complete(): void {
    this.error.set('');
    this.api.staffComplete(this.reference, this.otp.trim()).subscribe({
      next: (res) => this.current.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not complete this sale.')
    });
  }

  private parseCodes(): string[] {
    return this.bagQrCodesText
      .split('\n')
      .map((code) => code.trim())
      .filter((code) => code.length > 0);
  }
}
