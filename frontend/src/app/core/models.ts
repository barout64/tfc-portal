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

export interface CatalogLocation {
  region: string;
  district: string | null;
  village: string | null;
}

export interface WarehouseFilter {
  region?: string;
  district?: string;
  village?: string;
}

export interface SaleRequestLine {
  productName: string;
  bagSizeKg: number;
  bagCount: number;
  fullUnitPrice: number;
  farmerUnitPrice: number;
}

export interface SaleRequestResponse {
  id: number;
  reference: string;
  status: SaleRequestStatus;
  farmerName: string;
  farmerNumber: string;
  warehouseName: string;
  warehouseLocation: string;
  totalAmount: number;
  farmerCashAmount: number;
  subsidyAmount: number;
  reservedAt: string;
  expiresAt: string;
  submittedToTfra: boolean;
  completedAt: string | null;
  tfraSaleReference: string | null;
  totalBags: number;
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

export interface EntitlementLine {
  type: string;
  entitledKg: number;
  receivedKg: number;
  reservedKg: number;
  remainingKg: number;
}

export interface TfraOrder {
  orderNumber: string;
  productType: string;
  productName: string;
  quantityKg: number;
  status: string;
  orderedOn: string;
}

export interface TfraReceivedItem {
  productType: string;
  productName: string;
  quantityKg: number;
  sellerName: string;
  receivedOn: string;
}

export interface FarmerProfile {
  farmerNumber: string;
  fullName: string;
  active: boolean;
  stopSubsidy: boolean;
  eligible: boolean;
  entitlements: EntitlementLine[];
  orders: TfraOrder[];
  receivedItems: TfraReceivedItem[];
}

export type DealerOrderKind = 'INQUIRY' | 'ORDER';
export type DealerOrderStatus = 'SUBMITTED' | 'ANSWERED' | 'ACCEPTED' | 'REJECTED' | 'FULFILLED' | 'CANCELLED';

export interface DealerOrderLine {
  productName: string;
  bagSizeKg: number;
  bagCount: number;
  unitPrice: number;
}

export interface DealerOrder {
  id: number;
  reference: string;
  kind: DealerOrderKind;
  status: DealerOrderStatus;
  dealerName: string;
  dealerTfraNumber: string;
  warehouseName: string;
  estimatedAmount: number;
  dealerNote: string | null;
  warehouseResponse: string | null;
  createdAt: string;
  respondedAt: string | null;
  items: DealerOrderLine[];
}

export interface ApiErrorBody {
  message: string;
  errors?: Record<string, string>;
}
