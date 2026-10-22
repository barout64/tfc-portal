import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AgroDealerSalesReport,
  CatalogLocation,
  CreateSaleRequestItem,
  DealerOrder,
  DealerOrderKind,
  FarmerProfile,
  SaleRequestResponse,
  WarehouseFilter,
  WarehouseSummary
} from './models';

export type DealerOrderAction = 'answer' | 'accept' | 'reject' | 'fulfil';

@Injectable({ providedIn: 'root' })
export class PortalApiService {
  constructor(private http: HttpClient) {}

  warehouses(filter: WarehouseFilter = {}): Observable<WarehouseSummary[]> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filter)) {
      if (value) {
        params = params.set(key, value);
      }
    }
    return this.http.get<WarehouseSummary[]>(`${environment.apiUrl}/catalog/warehouses`, { params });
  }

  locations(): Observable<CatalogLocation[]> {
    return this.http.get<CatalogLocation[]>(`${environment.apiUrl}/catalog/locations`);
  }

  farmerProfile(): Observable<FarmerProfile> {
    return this.http.get<FarmerProfile>(`${environment.apiUrl}/farmers/me`);
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

  createDealerOrder(warehouseId: number, kind: DealerOrderKind, items: CreateSaleRequestItem[], note: string): Observable<DealerOrder> {
    return this.http.post<DealerOrder>(`${environment.apiUrl}/agro-dealers/me/orders`, { warehouseId, kind, items, note });
  }

  myDealerOrders(): Observable<DealerOrder[]> {
    return this.http.get<DealerOrder[]>(`${environment.apiUrl}/agro-dealers/me/orders`);
  }

  cancelDealerOrder(reference: string): Observable<DealerOrder> {
    return this.http.post<DealerOrder>(`${environment.apiUrl}/agro-dealers/me/orders/${reference}/cancel`, {});
  }

  staffFindSaleRequest(reference: string): Observable<SaleRequestResponse> {
    return this.http.get<SaleRequestResponse>(`${environment.apiUrl}/staff/sale-requests/${reference}`);
  }

  staffStartPickup(reference: string): Observable<SaleRequestResponse> {
    return this.http.post<SaleRequestResponse>(`${environment.apiUrl}/staff/sale-requests/${reference}/start`, {});
  }

  staffSubmitToTfra(reference: string, bagQrCodes: string[]): Observable<SaleRequestResponse> {
    return this.http.post<SaleRequestResponse>(`${environment.apiUrl}/staff/sale-requests/${reference}/submit`, { bagQrCodes });
  }

  staffRequestOtp(reference: string): Observable<{ message: string; requestId: string }> {
    return this.http.post<{ message: string; requestId: string }>(`${environment.apiUrl}/staff/sale-requests/${reference}/request-otp`, {});
  }

  staffComplete(reference: string, otp: string): Observable<SaleRequestResponse> {
    return this.http.post<SaleRequestResponse>(`${environment.apiUrl}/staff/sale-requests/${reference}/complete`, { otp });
  }

  staffDealerOrders(): Observable<DealerOrder[]> {
    return this.http.get<DealerOrder[]>(`${environment.apiUrl}/staff/dealer-orders`);
  }

  staffRespondDealerOrder(reference: string, action: DealerOrderAction, response: string): Observable<DealerOrder> {
    return this.http.post<DealerOrder>(`${environment.apiUrl}/staff/dealer-orders/${reference}/${action}`, { response });
  }
}
