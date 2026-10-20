import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AgroDealerSalesReport,
  CreateSaleRequestItem,
  SaleRequestResponse,
  WarehouseSummary
} from './models';

@Injectable({ providedIn: 'root' })
export class PortalApiService {
  constructor(private http: HttpClient) {}

  warehouses(region?: string): Observable<WarehouseSummary[]> {
    const url = region ? `${environment.apiUrl}/catalog/warehouses?region=${encodeURIComponent(region)}` : `${environment.apiUrl}/catalog/warehouses`;
    return this.http.get<WarehouseSummary[]>(url);
  }

  createSaleRequest(warehouseId: number, items: CreateSaleRequestItem[]): Observable<SaleRequestResponse> {
    return this.http.post<SaleRequestResponse>(`${environment.apiUrl}/sale-requests`, { warehouseId, items });
  }

  mySaleRequests(): Observable<SaleRequestResponse[]> {
    return this.http.get<SaleRequestResponse[]>(`${environment.apiUrl}/sale-requests`);
  }

  trackSaleRequest(reference: string): Observable<SaleRequestResponse> {
    return this.http.get<SaleRequestResponse>(`${environment.apiUrl}/sale-requests/${reference}`);
  }

  cancelSaleRequest(reference: string): Observable<SaleRequestResponse> {
    return this.http.post<SaleRequestResponse>(`${environment.apiUrl}/sale-requests/${reference}/cancel`, {});
  }

  agroDealerSalesReport(from: string, to: string): Observable<AgroDealerSalesReport> {
    return this.http.get<AgroDealerSalesReport>(`${environment.apiUrl}/agro-dealers/me/sales-report?from=${from}&to=${to}`);
  }

  staffFindSaleRequest(reference: string): Observable<SaleRequestResponse> {
    return this.http.get<SaleRequestResponse>(`${environment.apiUrl}/staff/sale-requests/${reference}`);
  }

  staffStartPickup(reference: string): Observable<SaleRequestResponse> {
    return this.http.post<SaleRequestResponse>(`${environment.apiUrl}/staff/sale-requests/${reference}/start`, {});
  }

  staffRequestOtp(reference: string): Observable<{ message: string; requestId: string }> {
    return this.http.post<{ message: string; requestId: string }>(`${environment.apiUrl}/staff/sale-requests/${reference}/request-otp`, {});
  }

  staffComplete(reference: string, bagQrCodes: string[], otp: string): Observable<SaleRequestResponse> {
    return this.http.post<SaleRequestResponse>(`${environment.apiUrl}/staff/sale-requests/${reference}/complete`, { bagQrCodes, otp });
  }
}
