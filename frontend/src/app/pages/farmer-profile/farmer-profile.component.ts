import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PortalApiService } from '../../core/portal-api.service';
import { FarmerProfile } from '../../core/models';

@Component({
  selector: 'app-farmer-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <h1>My entitlement</h1>

      @if (profile(); as p) {
        <div class="card">
          <div class="head">
            <div>
              <strong>{{ p.fullName }}</strong>
              <div class="muted">Farmer number {{ p.farmerNumber }}</div>
            </div>
            <span class="badge" [class.ok]="p.eligible" [class.stop]="!p.eligible">
              {{ p.eligible ? 'Eligible' : p.stopSubsidy ? 'Subsidy stopped' : 'Inactive' }}
            </span>
          </div>
          @if (!p.eligible) {
            <p class="note">TFRA reports you are not currently eligible for subsidised inputs. Contact your ward extension officer.</p>
          }
        </div>

        <h2>Entitlement this season</h2>
        <table>
          <thead>
            <tr><th>Type</th><th>Entitled</th><th>Received</th><th>Reserved</th><th>Remaining</th></tr>
          </thead>
          <tbody>
            @for (line of p.entitlements; track line.type) {
              <tr>
                <td>{{ line.type }}</td>
                <td>{{ line.entitledKg | number }} kg</td>
                <td>{{ line.receivedKg | number }} kg</td>
                <td>{{ line.reservedKg | number }} kg</td>
                <td><strong>{{ line.remainingKg | number }} kg</strong></td>
              </tr>
            } @empty {
              <tr><td colspan="5">TFRA has no entitlement on record for you.</td></tr>
            }
          </tbody>
        </table>
        <p class="muted">Reserved = held by your open reservations on this portal. <a routerLink="/catalog">Reserve inputs →</a></p>

        <h2>Orders on record with TFRA</h2>
        <table>
          <thead><tr><th>Order</th><th>Product</th><th>Quantity</th><th>Status</th><th>Date</th></tr></thead>
          <tbody>
            @for (order of p.orders; track order.orderNumber) {
              <tr>
                <td>{{ order.orderNumber }}</td>
                <td>{{ order.productName }}</td>
                <td>{{ order.quantityKg | number }} kg</td>
                <td>{{ order.status }}</td>
                <td>{{ order.orderedOn | date }}</td>
              </tr>
            } @empty {
              <tr><td colspan="5">No orders on record.</td></tr>
            }
          </tbody>
        </table>

        <h2>Inputs already received</h2>
        <table>
          <thead><tr><th>Product</th><th>Quantity</th><th>From</th><th>Date</th></tr></thead>
          <tbody>
            @for (item of p.receivedItems; track $index) {
              <tr>
                <td>{{ item.productName }}</td>
                <td>{{ item.quantityKg | number }} kg</td>
                <td>{{ item.sellerName }}</td>
                <td>{{ item.receivedOn | date }}</td>
              </tr>
            } @empty {
              <tr><td colspan="4">Nothing received yet.</td></tr>
            }
          </tbody>
        </table>
      } @else if (!error()) {
        <p>Loading your details from TFRA…</p>
      }

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </div>
  `,
  styles: [
    `
      .page { max-width: 760px; margin: 0 auto; padding: 24px 16px; }
      h2 { font-size: 18px; margin-top: 28px; }
      .card { border: 1px solid #ddd; border-radius: 10px; padding: 16px; }
      .head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
      .muted { color: #777; font-size: 13px; }
      .badge { padding: 2px 10px; border-radius: 10px; font-size: 12px; white-space: nowrap; }
      .badge.ok { background: #e3f6e8; color: #157a33; }
      .badge.stop { background: #fde7e7; color: #a4262c; }
      .note { color: #9a6400; background: #fff8ec; padding: 8px 12px; border-radius: 6px; }
      table { width: 100%; border-collapse: collapse; margin-top: 10px; }
      th, td { text-align: left; padding: 8px; border-bottom: 1px solid #eee; }
      .error { color: #b00020; margin-top: 12px; }
    `
  ]
})
export class FarmerProfileComponent implements OnInit {
  readonly profile = signal<FarmerProfile | null>(null);
  readonly error = signal('');

  constructor(private api: PortalApiService) {}

  ngOnInit(): void {
    this.api.farmerProfile().subscribe({
      next: (res) => this.profile.set(res),
      error: (err) => this.error.set(err.error?.message ?? 'Could not load your entitlement.')
    });
  }
}
