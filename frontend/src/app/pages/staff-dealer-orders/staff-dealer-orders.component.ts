import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DealerOrderAction, PortalApiService } from '../../core/portal-api.service';
import { DealerOrder } from '../../core/models';

@Component({
  selector: 'app-staff-dealer-orders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <h1>Agro-dealer inquiries &amp; orders</h1>
      <label class="toggle"><input type="checkbox" [(ngModel)]="openOnly" /> Only show ones waiting for a response</label>

      @for (order of visible(); track order.id) {
        <div class="card">
          <div class="head">
            <strong>{{ order.reference }} · {{ order.kind === 'ORDER' ? 'Order' : 'Inquiry' }}</strong>
            <span class="badge" [class]="order.status.toLowerCase()">{{ order.status }}</span>
          </div>
          <p class="muted">{{ order.dealerName }} (TFRA {{ order.dealerTfraNumber }}) · {{ order.warehouseName }} · {{ order.createdAt | date: 'medium' }}</p>
          <ul>
            @for (item of order.items; track item.productName) {
              <li>{{ item.bagCount }} × {{ item.productName }} &#64; {{ item.unitPrice | number }}</li>
            }
          </ul>
          <p>Estimated value: {{ order.estimatedAmount | number }}</p>
          @if (order.dealerNote) {
            <p class="quote">“{{ order.dealerNote }}”</p>
          }
          @if (order.warehouseResponse) {
            <p><strong>Our response:</strong> {{ order.warehouseResponse }}</p>
          }

          @if (order.status === 'SUBMITTED') {
            <textarea [(ngModel)]="responses[order.reference]" rows="2"
                      [placeholder]="order.kind === 'INQUIRY' ? 'Answer the inquiry (price, availability, timing)…' : 'Optional note to the dealer (pickup date, payment details)…'"></textarea>
            @if (order.kind === 'INQUIRY') {
              <button class="btn" (click)="act(order, 'answer')">Send answer</button>
            } @else {
              <button class="btn" (click)="act(order, 'accept')">Accept (takes stock)</button>
              <button class="btn danger" (click)="act(order, 'reject')">Reject</button>
            }
          }
          @if (order.status === 'ACCEPTED') {
            <button class="btn" (click)="act(order, 'fulfil')">Mark fulfilled</button>
          }
        </div>
      } @empty {
        <p>Nothing here.</p>
      }

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </div>
  `,
  styles: [
    `
      .page { max-width: 760px; margin: 0 auto; padding: 24px 16px; }
      .toggle { font-size: 14px; color: #555; }
      .card { border: 1px solid #ddd; border-radius: 10px; padding: 16px; margin-top: 16px; }
      .head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
      .muted { color: #777; font-size: 13px; }
      .quote { font-style: italic; color: #444; }
      textarea { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 6px; box-sizing: border-box; margin-top: 8px; }
      .btn { margin: 10px 8px 0 0; padding: 8px 14px; border: 0; border-radius: 6px; background: #0a7d32; color: #fff; cursor: pointer; }
      .btn.danger { background: #a4262c; }
      .badge { padding: 2px 10px; border-radius: 10px; font-size: 12px; background: #f1f1f1; color: #666; }
      .badge.submitted { background: #e8f0fe; color: #1a4fa0; }
      .badge.accepted, .badge.answered { background: #fff4df; color: #9a6400; }
      .badge.fulfilled { background: #e3f6e8; color: #157a33; }
      .badge.rejected { background: #fde7e7; color: #a4262c; }
      .error { color: #b00020; margin-top: 12px; }
      ul { margin: 8px 0; padding-left: 18px; }
    `
  ]
})
export class StaffDealerOrdersComponent implements OnInit {
  readonly orders = signal<DealerOrder[]>([]);
  readonly error = signal('');
  openOnly = true;
  responses: Record<string, string> = {};

  constructor(private api: PortalApiService) {}

  ngOnInit(): void {
    this.load();
  }

  visible(): DealerOrder[] {
    return this.openOnly ? this.orders().filter((o) => o.status === 'SUBMITTED' || o.status === 'ACCEPTED') : this.orders();
  }

  load(): void {
    this.api.staffDealerOrders().subscribe({
      next: (res) => this.orders.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not load dealer orders.')
    });
  }

  act(order: DealerOrder, action: DealerOrderAction): void {
    this.error.set('');
    this.api.staffRespondDealerOrder(order.reference, action, this.responses[order.reference] ?? '').subscribe({
      next: () => {
        delete this.responses[order.reference];
        this.load();
      },
      error: (err) => this.error.set(err.error?.message ?? 'Could not update this order.')
    });
  }
}
