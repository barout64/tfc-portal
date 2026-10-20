import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { RegisterComponent } from './pages/register/register.component';
import { LoginComponent } from './pages/login/login.component';
import { CatalogComponent } from './pages/catalog/catalog.component';
import { SaleRequestsComponent } from './pages/sale-requests/sale-requests.component';
import { AgroDealerComponent } from './pages/agro-dealer/agro-dealer.component';
import { StaffLoginComponent } from './pages/staff-login/staff-login.component';
import { StaffDashboardComponent } from './pages/staff-dashboard/staff-dashboard.component';
import { roleGuard } from './core/role.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'farmers/register', component: RegisterComponent, data: { audience: 'farmers' } },
  { path: 'farmers/login', component: LoginComponent, data: { audience: 'farmers' } },
  { path: 'agro-dealers/register', component: RegisterComponent, data: { audience: 'agro-dealers' } },
  { path: 'agro-dealers/login', component: LoginComponent, data: { audience: 'agro-dealers' } },
  { path: 'catalog', component: CatalogComponent },
  { path: 'sale-requests', component: SaleRequestsComponent, canActivate: [roleGuard(['FARMER'])] },
  { path: 'agro-dealers/me', component: AgroDealerComponent, canActivate: [roleGuard(['AGRO_DEALER'])] },
  { path: 'staff/login', component: StaffLoginComponent },
  { path: 'staff', component: StaffDashboardComponent, canActivate: [roleGuard(['STAFF'])] },
  { path: '**', redirectTo: '' }
];
