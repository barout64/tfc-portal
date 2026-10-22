import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PortalApiService } from '../../core/portal-api.service';
import { RouterLink } from '@angular/router';
import { AgroDealerSalesReport, DealerOrder } from '../../core/models';

@Component({
  selector: 'app-agro-dealer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page">
      <h1>My inquiries &amp; orders</h1>
      <p class="muted">Send a new one from the <a routerLink="/catalog">catalogue</a>.</p>
      @for (order of orders(); track order.id) {
        <div class="order">
          <div class="head">
            <strong>{{ order.reference }} · {{ order.kind === 'ORDER' ? 'Order' : 'Inquiry' }}</strong>
            <span class="badge" [class]="order.status.toLowerCase()">{{ order.status }}</span>
          </div>
          <p class="muted">{{ order.warehouseName }} · {{ order.createdAt | date: 'medium' }} · est. {{ order.estimatedAmount | number }}</p>
          <ul>
            @for (item of order.items; track item.productName) {
              <li>{{ item.bagCount }} × {{ item.productName }}</li>
            }
          </ul>
          @if (order.warehouseResponse) {
            <p><strong>Warehouse:</strong> {{ order.warehouseResponse }}</p>
          }
          @if (order.status === 'SUBMITTED') {
            <button class="btn secondary" (click)="cancel(order.reference)">Cancel</button>
          }
        </div>
      } @empty {
        <p>You haven't sent any inquiries or orders yet.</p>
      }

      <h1 class="report-title">My TFRA sales report</h1>
      <label>From</label>
      <input type="date" [(ngModel)]="from" />
      <label>To</label>
      <input type="date" [(ngModel)]="to" />
      <button class="btn" (click)="run()">Run report</button>

      @if (report()) {
        <div class="card">
          <p>Bags sold: {{ report()!.bagsSold }} · Scanned: {{ report()!.bagsScanned }} · Unscanned: {{ report()!.bagsUnscanned }}</p>
          <table>
            <thead><tr><th>Product</th><th>Sold</th><th>Scanned</th></tr></thead>
            <tbody>
              @for (line of report()!.lines; track line.productName) {
                <tr><td>{{ line.productName }}</td><td>{{ line.bagsSold }}</td><td>{{ line.bagsScanned }}</td></tr>
              } @empty {
                <tr><td colspan="3">No sales in this date range.</td></tr>
              }
            </tbody>
          </table>
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
      input { padding: 8px; border: 1px solid #ccc; border-radius: 6px; }
      .btn { margin-top: 16px; padding: 10px 16px; border: 0; border-radius: 6px; background: #0a7d32; color: #fff; cursor: pointer; }
      .card { margin-top: 20px; }
      .report-title { margin-top: 40px; }
      .muted { color: #777; font-size: 13px; }
      .order { border: 1px solid #ddd; border-radius: 10px; padding: 12px 16px; margin-top: 12px; }
      .head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
      .badge { padding: 2px 10px; border-radius: 10px; font-size: 12px; background: #f1f1f1; color: #666; }
      .badge.submitted { background: #e8f0fe; color: #1a4fa0; }
      .badge.accepted, .badge.answered { background: #fff4df; color: #9a6400; }
      .badge.fulfilled { background: #e3f6e8; color: #157a33; }
      .badge.rejected { background: #fde7e7; color: #a4262c; }
      .btn.secondary { background: #eee; color: #222; margin-top: 4px; }
      ul { margin: 8px 0; padding-left: 18px; }
      table { width: 100%; border-collapse: collapse; margin-top: 10px; }
      th, td { text-align: left; padding: 8px; border-bottom: 1px solid #eee; }
      .error { color: #b00020; margin-top: 12px; }
    `
  ]
})
export class AgroDealerComponent implements OnInit {
  from = '';
  to = '';
  readonly report = signal<AgroDealerSalesReport | null>(null);
  readonly orders = signal<DealerOrder[]>([]);
  readonly error = signal('');

  constructor(private api: PortalApiService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.api.myDealerOrders().subscribe({
      next: (res) => this.orders.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not load your orders.')
    });
  }

  cancel(reference: string): void {
    this.api.cancelDealerOrder(reference).subscribe({
      next: () => this.loadOrders(),
      error: (err) => this.error.set(err.error?.message ?? 'Could not cancel this request.')
    });
  }

  run(): void {
    if (!this.from || !this.to) {
      this.error.set('Pick both dates.');
      return;
    }
    this.error.set('');
    this.api.agroDealerSalesReport(this.from, this.to).subscribe({
      next: (res) => this.report.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not load the report.')
    });
  }
}
