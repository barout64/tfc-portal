import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="hero">
      <h1>TFC Farmer &amp; Agro-Dealer Portal</h1>
      <p>Check your subsidy entitlement, browse warehouse stock, and reserve a purchase online — finish it at the warehouse.</p>
    </div>
    <div class="card-grid">
      <div class="card">
        <h2>Farmer</h2>
        <p>Register with your TFRA farmer wallet number to check your entitlement and reserve subsidised inputs.</p>
        <a routerLink="/farmers/register" class="btn">Register as a farmer</a>
        <a routerLink="/farmers/login" class="btn secondary">I already have an account</a>
      </div>
      <div class="card">
        <h2>Agro-dealer</h2>
        <p>Register with your TFRA agro-dealer number to browse the catalogue, send wholesale inquiries and orders, and view your TFRA sales reports.</p>
        <a routerLink="/agro-dealers/register" class="btn">Register as an agro-dealer</a>
        <a routerLink="/agro-dealers/login" class="btn secondary">I already have an account</a>
      </div>
      <div class="card">
        <h2>Warehouse staff</h2>
        <p>Complete reserved sales with farmers at the counter and handle agro-dealer orders.</p>
        <a routerLink="/staff/login" class="btn">Staff sign in</a>
      </div>
    </div>
  `,
  styles: [
    `
      .hero {
        text-align: center;
        padding: 48px 16px 24px;
      }
      .hero h1 {
        margin: 0 0 12px;
        font-size: 28px;
      }
      .hero p {
        color: #555;
        max-width: 560px;
        margin: 0 auto;
      }
      .card-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
        gap: 16px;
        padding: 16px;
        max-width: 900px;
        margin: 0 auto;
      }
      .card {
        border: 1px solid #ddd;
        border-radius: 10px;
        padding: 20px;
        display: flex;
        flex-direction: column;
        gap: 10px;
      }
      .card h2 {
        margin: 0;
      }
      .btn {
        display: block;
        text-align: center;
        padding: 10px;
        border-radius: 6px;
        background: #0a7d32;
        color: #fff;
        text-decoration: none;
      }
      .btn.secondary {
        background: #f1f1f1;
        color: #222;
      }
    `
  ]
})
export class HomeComponent {}
