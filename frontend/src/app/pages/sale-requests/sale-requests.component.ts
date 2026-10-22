import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PortalApiService } from '../../core/portal-api.service';
import { SaleRequestResponse } from '../../core/models';

@Component({
  selector: 'app-sale-requests',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <h1>My sale requests</h1>

      @if (requests().length === 0) {
        <p>You have no sale requests yet.</p>
      }

      @for (request of requests(); track request.id) {
        <div class="card">
          <div class="head">
            <strong>{{ request.reference }}</strong>
            <span class="badge" [class]="request.status.toLowerCase()">{{ request.status }}</span>
          </div>
          <p>{{ request.warehouseName }}</p>
          <ul>
            @for (item of request.items; track item.productName) {
              <li>{{ item.bagCount }} × {{ item.productName }} &#64; {{ item.farmerUnitPrice | number }}</li>
            }
          </ul>
          <p>
            Total {{ request.totalAmount | number }} · You pay {{ request.farmerCashAmount | number }} · Subsidy
            {{ request.subsidyAmount | number }}
          </p>
          <p>Reserved {{ request.reservedAt | date: 'medium' }} · Expires {{ request.expiresAt | date: 'medium' }}</p>
          @if (request.status === 'COMPLETED') {
            <p>Completed {{ request.completedAt | date: 'medium' }} · TFRA ref {{ request.tfraSaleReference }}</p>
            <a class="btn secondary" [routerLink]="['/sale-requests', request.reference, 'receipt']">View receipt</a>
          } @else if (request.submittedToTfra) {
            <p class="muted">Being completed at the warehouse — give staff the code TFRA texts you.</p>
          } @else if (request.status === 'SUBMITTED' || request.status === 'READY_FOR_PICKUP') {
            <button class="btn secondary" (click)="cancel(request.reference)">Cancel reservation</button>
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
      .page { max-width: 760px; margin: 0 auto; padding: 24px 16px; }
      .card { border: 1px solid #ddd; border-radius: 10px; padding: 16px; margin-top: 16px; }
      .head { display: flex; justify-content: space-between; align-items: center; }
      .badge { padding: 2px 10px; border-radius: 10px; font-size: 12px; }
      .badge.submitted { background: #e8f0fe; color: #1a4fa0; }
      .badge.ready_for_pickup { background: #fff4df; color: #9a6400; }
      .badge.completed { background: #e3f6e8; color: #157a33; }
      .badge.expired, .badge.cancelled { background: #f1f1f1; color: #666; }
      .muted { color: #777; font-size: 13px; }
      a.btn.secondary { display: inline-block; text-decoration: none; color: #222; }
      .btn.secondary { margin-top: 10px; padding: 8px 14px; border: 1px solid #ccc; border-radius: 6px; background: #fff; cursor: pointer; }
      .error { color: #b00020; margin-top: 12px; }
      ul { margin: 8px 0; padding-left: 18px; }
    `
  ]
})
export class SaleRequestsComponent implements OnInit {
  readonly requests = signal<SaleRequestResponse[]>([]);
  readonly error = signal('');

  constructor(private api: PortalApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.mySaleRequests().subscribe({
      next: (res) => this.requests.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not load your sale requests.')
    });
  }

  cancel(reference: string): void {
    this.api.cancelSaleRequest(reference).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(err.error?.message ?? 'Could not cancel this reservation.')
    });
  }
}
