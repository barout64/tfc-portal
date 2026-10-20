import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PortalApiService } from '../../core/portal-api.service';
import { AgroDealerSalesReport } from '../../core/models';

@Component({
  selector: 'app-agro-dealer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <h1>My TFRA sales report</h1>
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
      table { width: 100%; border-collapse: collapse; margin-top: 10px; }
      th, td { text-align: left; padding: 8px; border-bottom: 1px solid #eee; }
      .error { color: #b00020; margin-top: 12px; }
    `
  ]
})
export class AgroDealerComponent {
  from = '';
  to = '';
  readonly report = signal<AgroDealerSalesReport | null>(null);
  readonly error = signal('');

  constructor(private api: PortalApiService) {}

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
