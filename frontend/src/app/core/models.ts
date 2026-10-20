export type Audience = 'farmers' | 'agro-dealers';
export type Role = 'FARMER' | 'AGRO_DEALER' | 'STAFF';
export type AvailabilityLevel = 'AVAILABLE' | 'LOW' | 'OUT';
export type SaleRequestStatus = 'SUBMITTED' | 'READY_FOR_PICKUP' | 'COMPLETED' | 'EXPIRED' | 'CANCELLED';

export interface MaskedPhoneResponse {
  message: string;
  maskedPhone: string;
}

export interface TokenResponse {
  token: string;
  displayName: string;
  role: Role;
}

export interface ProductSummary {
  id: number;
  warehouseId: number;
  name: string;
  type: string;
  bagSizeKg: number;
  fullPrice: number;
  farmerUnitPrice: number | null;
  availability: AvailabilityLevel;
}

export interface WarehouseSummary {
  id: number;
  name: string;
  region: string;
  district: string | null;
  village: string | null;
  canTakeSubsidisedSales: boolean;
  products: ProductSummary[];
}

export interface SaleRequestLine {
  productName: string;
  bagCount: number;
  fullUnitPrice: number;
  farmerUnitPrice: number;
}

export interface SaleRequestResponse {
  id: number;
  reference: string;
  status: SaleRequestStatus;
  warehouseName: string;
  totalAmount: number;
  farmerCashAmount: number;
  subsidyAmount: number;
  reservedAt: string;
  expiresAt: string;
  completedAt: string | null;
  items: SaleRequestLine[];
}

export interface CreateSaleRequestItem {
  productId: number;
  bagCount: number;
}

export interface AgroDealerSalesReportLine {
  productName: string;
  bagsSold: number;
  bagsScanned: number;
}

export interface AgroDealerSalesReport {
  tfraDealerNumber: string;
  from: string;
  to: string;
  bagsSold: number;
  bagsScanned: number;
  bagsUnscanned: number;
  lines: AgroDealerSalesReportLine[];
}

export interface ApiErrorBody {
  message: string;
  errors?: Record<string, string>;
}
