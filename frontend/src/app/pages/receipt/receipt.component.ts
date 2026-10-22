import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PortalApiService } from '../../core/portal-api.service';
import { SaleRequestResponse } from '../../core/models';

@Component({
  selector: 'app-receipt',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      @if (sale(); as s) {
        <div class="receipt">
          <h1>Subsidised sale receipt</h1>
          <div class="grid">
            <span>Receipt</span><strong>{{ s.reference }}</strong>
            <span>TFRA sale reference</span><strong>{{ s.tfraSaleReference }}</strong>
            <span>Date</span><span>{{ s.completedAt | date: 'medium' }}</span>
            <span>Farmer</span><span>{{ s.farmerName }} ({{ s.farmerNumber }})</span>
            <span>Warehouse</span><span>{{ s.warehouseName }}@if (s.warehouseLocation) {, {{ s.warehouseLocation }}}</span>
          </div>
          <table>
            <thead><tr><th>Product</th><th>Bags</th><th>Full price</th><th>Farmer price</th><th>You paid</th></tr></thead>
            <tbody>
              @for (item of s.items; track item.productName) {
                <tr>
                  <td>{{ item.productName }}</td>
                  <td>{{ item.bagCount }}</td>
                  <td>{{ item.fullUnitPrice | number }}</td>
                  <td>{{ item.farmerUnitPrice | number }}</td>
                  <td>{{ item.farmerUnitPrice * item.bagCount | number }}</td>
                </tr>
              }
            </tbody>
          </table>
          <div class="totals">
            <div><span>Total value</span><span>TZS {{ s.totalAmount | number }}</span></div>
            <div><span>Government subsidy</span><span>TZS {{ s.subsidyAmount | number }}</span></div>
            <div class="paid"><span>Paid by farmer</span><span>TZS {{ s.farmerCashAmount | number }}</span></div>
          </div>
        </div>
        <div class="actions">
          <button class="btn" (click)="print()">Print / save as PDF</button>
          <a routerLink="/sale-requests">← Back to my sale requests</a>
        </div>
      } @else if (error()) {
        <p class="error">{{ error() }}</p>
      } @else {
        <p>Loading receipt…</p>
      }
    </div>
  `,
  styles: [
    `
      .page { max-width: 720px; margin: 0 auto; padding: 24px 16px; }
      .receipt { border: 1px solid #ddd; border-radius: 10px; padding: 20px; }
      h1 { font-size: 22px; margin-top: 0; }
      .grid { display: grid; grid-template-columns: max-content 1fr; gap: 6px 16px; font-size: 14px; }
      .grid > span:nth-child(odd) { color: #777; }
      table { width: 100%; border-collapse: collapse; margin-top: 16px; font-size: 14px; }
      th, td { text-align: left; padding: 8px; border-bottom: 1px solid #eee; }
      .totals { margin-top: 12px; margin-left: auto; max-width: 320px; }
      .totals div { display: flex; justify-content: space-between; padding: 4px 0; }
      .totals .paid { font-weight: bold; border-top: 1px solid #ccc; margin-top: 4px; padding-top: 8px; }
      .actions { margin-top: 16px; display: flex; gap: 16px; align-items: center; }
      .btn { padding: 10px 16px; border: 0; border-radius: 6px; background: #0a7d32; color: #fff; cursor: pointer; }
      .error { color: #b00020; }
      @media print { .actions { display: none; } .receipt { border: 0; } }
    `
  ]
})
export class ReceiptComponent implements OnInit {
  readonly sale = signal<SaleRequestResponse | null>(null);
  readonly error = signal('');

  constructor(private api: PortalApiService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    const reference = this.route.snapshot.paramMap.get('reference') ?? '';
    this.api.trackSaleRequest(reference).subscribe({
      next: (res) => {
        if (res.status !== 'COMPLETED') {
          this.error.set('A receipt is only available once the sale is completed at the warehouse.');
          return;
        }
        this.sale.set(res);
      },
      error: (err) => this.error.set(err.error?.message ?? 'Could not load this receipt.')
    });
  }

  print(): void {
    window.print();
  }
}
