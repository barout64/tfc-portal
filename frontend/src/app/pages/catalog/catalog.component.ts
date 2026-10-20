import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { PortalApiService } from '../../core/portal-api.service';
import { ProductSummary, WarehouseSummary } from '../../core/models';

interface CartLine {
  product: ProductSummary;
  bagCount: number;
}

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <h1>Warehouse catalogue</h1>
      <label>Filter by region</label>
      <input [(ngModel)]="region" (keyup.enter)="load()" placeholder="e.g. Morogoro" />
      <button class="btn secondary" (click)="load()">Search</button>

      @if (!isFarmer()) {
        <p class="note">Log in as a farmer to see your subsidised price and reserve a purchase.</p>
      }

      @for (warehouse of warehouses(); track warehouse.id) {
        <div class="warehouse">
          <h2>{{ warehouse.name }} <span class="region">· {{ warehouse.region }}</span></h2>
          @if (!warehouse.canTakeSubsidisedSales) {
            <p class="note">Not currently linked with TFRA — subsidised reservations are unavailable here.</p>
          }
          <table>
            <thead>
              <tr>
                <th>Product</th>
                <th>Bag</th>
                <th>Full price</th>
                <th>Farmer price</th>
                <th>Stock</th>
                @if (isFarmer()) {
                  <th>Bags</th>
                }
              </tr>
            </thead>
            <tbody>
              @for (product of warehouse.products; track product.id) {
                <tr>
                  <td>{{ product.name }}</td>
                  <td>{{ product.bagSizeKg }}kg</td>
                  <td>{{ product.fullPrice | number }}</td>
                  <td>{{ product.farmerUnitPrice !== null ? (product.farmerUnitPrice | number) : 'Not subsidised' }}</td>
                  <td><span class="badge" [class]="product.availability.toLowerCase()">{{ product.availability }}</span></td>
                  @if (isFarmer()) {
                    <td>
                      @if (product.farmerUnitPrice !== null && product.availability !== 'OUT' && warehouse.canTakeSubsidisedSales) {
                        <input type="number" min="0" style="width:60px" [ngModel]="cartQuantity(warehouse.id, product.id)"
                               (ngModelChange)="setCartQuantity(warehouse, product, $event)" />
                      }
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>

          @if (isFarmer() && cartTotalBags(warehouse.id) > 0) {
            <div class="cart-summary">
              <p>{{ cartTotalBags(warehouse.id) }} bag(s) selected at this warehouse.</p>
              <button class="btn" [disabled]="submitting()" (click)="reserve(warehouse)">Reserve this purchase</button>
            </div>
          }
        </div>
      }

      @if (result()) {
        <div class="result">
          <h3>Reservation created: {{ result()!.reference }}</h3>
          <p>Total value: {{ result()!.totalAmount | number }} · You pay: {{ result()!.farmerCashAmount | number }} · Subsidy: {{ result()!.subsidyAmount | number }}</p>
          <p>Go to the warehouse within 48 hours (by {{ result()!.expiresAt | date: 'medium' }}) to pay and collect.</p>
          <a (click)="goToRequests()">View my sale requests →</a>
        </div>
      }

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </div>
  `,
  styles: [
    `
      .page { max-width: 900px; margin: 0 auto; padding: 24px 16px; }
      input { padding: 8px; border: 1px solid #ccc; border-radius: 6px; }
      .btn { padding: 8px 14px; border: 0; border-radius: 6px; background: #0a7d32; color: #fff; cursor: pointer; }
      .btn.secondary { background: #eee; color: #222; margin-left: 8px; }
      .warehouse { margin-top: 28px; border: 1px solid #ddd; border-radius: 10px; padding: 16px; }
      .region { color: #777; font-weight: normal; font-size: 14px; }
      table { width: 100%; border-collapse: collapse; margin-top: 10px; }
      th, td { text-align: left; padding: 8px; border-bottom: 1px solid #eee; }
      .badge { padding: 2px 8px; border-radius: 10px; font-size: 12px; }
      .badge.available { background: #e3f6e8; color: #157a33; }
      .badge.low { background: #fff4df; color: #9a6400; }
      .badge.out { background: #fde7e7; color: #a4262c; }
      .note { color: #9a6400; background: #fff8ec; padding: 8px 12px; border-radius: 6px; }
      .cart-summary { margin-top: 12px; }
      .result { margin-top: 24px; padding: 16px; border: 1px solid #0a7d32; border-radius: 10px; background: #f1fbf3; }
      .error { color: #b00020; margin-top: 12px; }
    `
  ]
})
export class CatalogComponent implements OnInit {
  region = '';
  readonly warehouses = signal<WarehouseSummary[]>([]);
  readonly error = signal('');
  readonly submitting = signal(false);
  readonly result = signal<import('../../core/models').SaleRequestResponse | null>(null);

  private carts = new Map<number, Map<number, CartLine>>();

  constructor(private api: PortalApiService, private auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  isFarmer(): boolean {
    return this.auth.role === 'FARMER';
  }

  load(): void {
    this.error.set('');
    this.api.warehouses(this.region || undefined).subscribe({
      next: (res) => this.warehouses.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not load the catalogue.')
    });
  }

  cartQuantity(warehouseId: number, productId: number): number {
    return this.carts.get(warehouseId)?.get(productId)?.bagCount ?? 0;
  }

  setCartQuantity(warehouse: WarehouseSummary, product: ProductSummary, value: number): void {
    const warehouseCart = this.carts.get(warehouse.id) ?? new Map<number, CartLine>();
    if (value > 0) {
      warehouseCart.set(product.id, { product, bagCount: value });
    } else {
      warehouseCart.delete(product.id);
    }
    this.carts.set(warehouse.id, warehouseCart);
  }

  cartTotalBags(warehouseId: number): number {
    const cart = this.carts.get(warehouseId);
    if (!cart) {
      return 0;
    }
    return [...cart.values()].reduce((sum, line) => sum + line.bagCount, 0);
  }

  reserve(warehouse: WarehouseSummary): void {
    const cart = this.carts.get(warehouse.id);
    if (!cart || cart.size === 0) {
      return;
    }
    this.error.set('');
    this.submitting.set(true);
    const items = [...cart.values()].map((line) => ({ productId: line.product.id, bagCount: line.bagCount }));
    this.api.createSaleRequest(warehouse.id, items).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.result.set(res);
        this.carts.delete(warehouse.id);
        this.load();
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err.error?.message ?? 'Could not create the reservation.');
      }
    });
  }

  goToRequests(): void {
    this.router.navigateByUrl('/sale-requests');
  }
}
