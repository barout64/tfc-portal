import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { PortalApiService } from '../../core/portal-api.service';
import {
  CatalogLocation,
  DealerOrder,
  DealerOrderKind,
  ProductSummary,
  SaleRequestResponse,
  WarehouseSummary
} from '../../core/models';

interface CartLine {
  product: ProductSummary;
  bagCount: number;
}

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page">
      <h1>Warehouse catalogue</h1>
      <div class="filters">
        <div>
          <label>Region</label>
          <select [(ngModel)]="region" (ngModelChange)="district = ''; village = ''; load()">
            <option value="">All regions</option>
            @for (r of regions(); track r) { <option [value]="r">{{ r }}</option> }
          </select>
        </div>
        <div>
          <label>District</label>
          <select [(ngModel)]="district" (ngModelChange)="village = ''; load()" [disabled]="!region">
            <option value="">All districts</option>
            @for (d of districts(); track d) { <option [value]="d">{{ d }}</option> }
          </select>
        </div>
        <div>
          <label>Village</label>
          <select [(ngModel)]="village" (ngModelChange)="load()" [disabled]="!district">
            <option value="">All villages</option>
            @for (v of villages(); track v) { <option [value]="v">{{ v }}</option> }
          </select>
        </div>
      </div>

      @if (!isFarmer() && !isDealer()) {
        <p class="note">Log in as a farmer to see your subsidised price and reserve a purchase, or as an agro-dealer to send a wholesale inquiry or order.</p>
      }
      @if (isFarmer()) {
        <p class="muted">Check what you can still buy on <a routerLink="/farmers/me">My entitlement</a>.</p>
      }

      @for (warehouse of warehouses(); track warehouse.id) {
        <div class="warehouse">
          <h2>{{ warehouse.name }} <span class="region">· {{ locationOf(warehouse) }}</span></h2>
          @if (isFarmer() && !warehouse.canTakeSubsidisedSales) {
            <p class="note">Not currently linked with TFRA — subsidised reservations are unavailable here.</p>
          }
          <table>
            <thead>
              <tr>
                <th>Product</th>
                <th>Bag</th>
                <th>Full price</th>
                @if (!isDealer()) {
                  <th>Farmer price</th>
                }
                <th>Stock</th>
                @if (isFarmer() || isDealer()) {
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
                  @if (!isDealer()) {
                    <td>{{ product.farmerUnitPrice !== null ? (product.farmerUnitPrice | number) : isFarmer() ? 'Not subsidised' : 'Log in to see' }}</td>
                  }
                  <td><span class="badge" [class]="product.availability.toLowerCase()">{{ product.availability }}</span></td>
                  @if (canAddToCart(warehouse, product)) {
                    <td>
                      <input type="number" min="0" style="width:60px" [ngModel]="cartQuantity(warehouse.id, product.id)"
                             (ngModelChange)="setCartQuantity(warehouse, product, $event)" />
                    </td>
                  } @else if (isFarmer() || isDealer()) {
                    <td></td>
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

          @if (isDealer() && cartTotalBags(warehouse.id) > 0) {
            <div class="cart-summary">
              <p>{{ cartTotalBags(warehouse.id) }} bag(s) · estimated {{ cartEstimate(warehouse.id) | number }} at full price.</p>
              <textarea [(ngModel)]="dealerNotes[warehouse.id]" rows="2" placeholder="Message to the warehouse (optional): delivery, timing, payment terms…"></textarea>
              <button class="btn secondary" [disabled]="submitting()" (click)="sendDealer(warehouse, 'INQUIRY')">Send inquiry</button>
              <button class="btn" [disabled]="submitting()" (click)="sendDealer(warehouse, 'ORDER')">Place order</button>
            </div>
          }
        </div>
      } @empty {
        <p>No warehouses match these filters.</p>
      }

      @if (result()) {
        <div class="result">
          <h3>Reservation created: {{ result()!.reference }}</h3>
          <p>Total value: {{ result()!.totalAmount | number }} · You pay: {{ result()!.farmerCashAmount | number }} · Subsidy: {{ result()!.subsidyAmount | number }}</p>
          <p>Go to the warehouse within 48 hours (by {{ result()!.expiresAt | date: 'medium' }}) to pay and collect.</p>
          <a routerLink="/sale-requests">View my sale requests →</a>
        </div>
      }

      @if (dealerResult()) {
        <div class="result">
          <h3>{{ dealerResult()!.kind === 'ORDER' ? 'Order' : 'Inquiry' }} sent: {{ dealerResult()!.reference }}</h3>
          <p>{{ dealerResult()!.warehouseName }} will respond on the portal. Estimated value {{ dealerResult()!.estimatedAmount | number }}.</p>
          <a routerLink="/agro-dealers/me">View my orders →</a>
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
      input, select { padding: 8px; border: 1px solid #ccc; border-radius: 6px; }
      .filters { display: flex; flex-wrap: wrap; gap: 12px; }
      .filters select { min-width: 160px; }
      label { display: block; margin: 0 0 4px; font-size: 13px; color: #555; }
      textarea { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 6px; box-sizing: border-box; margin-bottom: 8px; }
      .muted { color: #777; font-size: 14px; }
      table { display: block; overflow-x: auto; }
      .btn { padding: 8px 14px; border: 0; border-radius: 6px; background: #0a7d32; color: #fff; cursor: pointer; }
      .btn.secondary { background: #eee; color: #222; margin-right: 8px; }
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
  district = '';
  village = '';
  dealerNotes: Record<number, string> = {};

  readonly locations = signal<CatalogLocation[]>([]);
  readonly regions = computed(() => unique(this.locations().map((l) => l.region)));
  readonly warehouses = signal<WarehouseSummary[]>([]);
  readonly error = signal('');
  readonly submitting = signal(false);
  readonly result = signal<SaleRequestResponse | null>(null);
  readonly dealerResult = signal<DealerOrder | null>(null);

  private carts = new Map<number, Map<number, CartLine>>();

  constructor(private api: PortalApiService, private auth: AuthService) {}

  ngOnInit(): void {
    this.api.locations().subscribe({ next: (res) => this.locations.set(res) });
    this.load();
  }

  isFarmer(): boolean {
    return this.auth.role === 'FARMER';
  }

  isDealer(): boolean {
    return this.auth.role === 'AGRO_DEALER';
  }

  districts(): string[] {
    return unique(this.locations().filter((l) => l.region === this.region).map((l) => l.district));
  }

  villages(): string[] {
    return unique(this.locations().filter((l) => l.region === this.region && l.district === this.district).map((l) => l.village));
  }

  locationOf(warehouse: WarehouseSummary): string {
    return [warehouse.village, warehouse.district, warehouse.region].filter((part) => !!part).join(', ');
  }

  load(): void {
    this.error.set('');
    this.api.warehouses({ region: this.region, district: this.district, village: this.village }).subscribe({
      next: (res) => this.warehouses.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not load the catalogue.')
    });
  }

  canAddToCart(warehouse: WarehouseSummary, product: ProductSummary): boolean {
    if (this.isFarmer()) {
      return product.farmerUnitPrice !== null && product.availability !== 'OUT' && warehouse.canTakeSubsidisedSales;
    }
    // Dealers can ask about anything, including out-of-stock lines.
    return this.isDealer();
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

  cartEstimate(warehouseId: number): number {
    const cart = this.carts.get(warehouseId);
    return cart ? [...cart.values()].reduce((sum, line) => sum + line.bagCount * line.product.fullPrice, 0) : 0;
  }

  reserve(warehouse: WarehouseSummary): void {
    const items = this.cartItems(warehouse.id);
    if (items.length === 0) {
      return;
    }
    this.startSubmit();
    this.api.createSaleRequest(warehouse.id, items).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.result.set(res);
        this.carts.delete(warehouse.id);
        this.load();
      },
      error: (err) => this.failSubmit(err, 'Could not create the reservation.')
    });
  }

  sendDealer(warehouse: WarehouseSummary, kind: DealerOrderKind): void {
    const items = this.cartItems(warehouse.id);
    if (items.length === 0) {
      return;
    }
    this.startSubmit();
    this.api.createDealerOrder(warehouse.id, kind, items, this.dealerNotes[warehouse.id] ?? '').subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.dealerResult.set(res);
        this.carts.delete(warehouse.id);
        delete this.dealerNotes[warehouse.id];
      },
      error: (err) => this.failSubmit(err, 'Could not send this request.')
    });
  }

  private cartItems(warehouseId: number) {
    const cart = this.carts.get(warehouseId);
    return cart ? [...cart.values()].map((line) => ({ productId: line.product.id, bagCount: line.bagCount })) : [];
  }

  private startSubmit(): void {
    this.error.set('');
    this.result.set(null);
    this.dealerResult.set(null);
    this.submitting.set(true);
  }

  private failSubmit(err: { error?: { message?: string } }, fallback: string): void {
    this.submitting.set(false);
    this.error.set(err.error?.message ?? fallback);
  }
}

function unique(values: (string | null)[]): string[] {
  return [...new Set(values.filter((v): v is string => !!v))].sort();
}
