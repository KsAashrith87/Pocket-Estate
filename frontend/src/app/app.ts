import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

type Page = 'dashboard' | 'clients' | 'units' | 'houses' | 'payments';

interface User {
  username: string;
  email: string | null;
  displayName: string;
  firstName: string;
  lastName: string;
  profilePicture: string | null;
}

interface Client {
  id: number;
  name: string;
  email: string;
  phone: string;
  notes: string | null;
}

interface Unit {
  id: number;
  unitNumber: string;
  size: string;
  monthlyRate: number;
  status: string;
  clientId: number | null;
  clientName: string | null;
}

interface House {
  id: number;
  address: string;
  monthlyRent: number;
  status: string;
  tenantId: number | null;
  tenantName: string | null;
}

interface Payment {
  id: number;
  clientId: number;
  clientName: string;
  amount: number;
  billingMonth: string;
  paidOn: string;
  method: string;
}

interface RentPayment {
  id: number;
  houseId: number;
  houseAddress: string;
  tenantName: string | null;
  amount: number;
  billingMonth: string;
  paidOn: string;
  method: string;
}

interface Notification {
  id: string;
  title: string;
  message: string;
}

interface Dashboard {
  user: User;
  clientCount: number;
  occupiedUnits: number;
  totalUnits: number;
  paymentsDue: number;
  monthlyRevenue: number;
  monthlyRentRevenue: number;
  occupiedHouses: number;
  totalHouses: number;
  clients: Client[];
  units: Unit[];
  houses: House[];
  payments: Payment[];
  rentPayments: RentPayment[];
  notifications: Notification[];
}

interface ClientForm {
  name: string;
  email: string;
  phone: string;
  notes: string;
}

interface UnitForm {
  unitNumber: string;
  size: string;
  monthlyRate: number | null;
  clientId: number | null;
}

interface HouseForm {
  address: string;
  monthlyRent: number | null;
  tenantId: number | null;
}

interface PaymentForm {
  clientId: number | null;
  amount: number | null;
  billingMonth: string;
  method: string;
}

interface RentPaymentForm {
  houseId: number | null;
  amount: number | null;
  billingMonth: string;
  method: string;
}

@Component({
  imports: [CommonModule, FormsModule],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit {
  readonly today = new Date();
  user: User | null = null;
  dashboard: Dashboard | null = null;
  page: Page = this.pageFromHash();
  authMode: 'login' | 'signup' = 'login';
  busy = false;
  errorMessage = '';
  successMessage = '';
  editingClient: Client | null = null;
  editingUnit: Unit | null = null;
  editingHouse: House | null = null;
  loginFormData = { username: '', password: '' };
  signupFormData = { firstName: '', lastName: '', username: '', email: '', password: '' };
  clientForm = this.emptyClientForm();
  unitForm = this.emptyUnitForm();
  houseForm = this.emptyHouseForm();
  paymentForm = this.emptyPaymentForm();
  rentPaymentForm = this.emptyRentPaymentForm();

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.http.get('/api/auth/csrf').subscribe({
      next: () => this.loadSession(),
      error: (error: unknown) => {
        this.errorMessage = this.describeError(error, 'Unable to connect to the server.');
      },
    });
  }

  login(): void {
    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.http.post<User>('/api/auth/login', this.loginFormData).subscribe({
      next: (user) => {
        this.user = user;
        this.loginFormData.password = '';
        this.loadDashboard();
      },
      error: (error: unknown) => {
        this.busy = false;
        this.errorMessage = this.describeError(error, 'Sign in failed.');
      },
    });
  }

  signup(): void {
    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.http.post<User>('/api/auth/signup', this.signupFormData).subscribe({
      next: () => {
        this.busy = false;
        this.authMode = 'login';
        this.loginFormData.username = this.signupFormData.username;
        this.signupFormData = { firstName: '', lastName: '', username: '', email: '', password: '' };
        this.successMessage = 'Account created. Sign in to open your workspace.';
      },
      error: (error: unknown) => {
        this.busy = false;
        this.errorMessage = this.describeError(error, 'Account creation failed.');
      },
    });
  }

  logout(): void {
    this.busy = true;
    this.http.post<void>('/api/auth/logout', {}).subscribe({
      next: () => this.clearSession(),
      error: (error: unknown) => {
        this.busy = false;
        this.errorMessage = this.describeError(error, 'Unable to sign out.');
      },
    });
  }

  navigate(page: Page): void {
    this.page = page;
    window.location.hash = `/${page}`;
    this.errorMessage = '';
    this.successMessage = '';
  }

  pageLabel(): string {
    return {
      dashboard: 'Portfolio overview',
      clients: 'People directory',
      units: 'Self-storage inventory',
      houses: 'Residential portfolio',
      payments: 'Payment ledger',
    }[this.page];
  }

  greeting(): string {
    const hour = this.today.getHours();
    return hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  }

  initials(name: string): string {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    return parts.length > 1
      ? `${parts[0][0]}${parts[1][0]}`.toUpperCase()
      : (parts[0]?.slice(0, 2) || 'PE').toUpperCase();
  }

  assignmentFor(clientId: number): string {
    const assignments = [
      ...(this.dashboard?.units.filter((unit) => unit.clientId === clientId).map((unit) => unit.unitNumber) ?? []),
      ...(this.dashboard?.houses.filter((house) => house.tenantId === clientId).map((house) => house.address) ?? []),
    ];
    return assignments.length ? assignments.join(', ') : '—';
  }

  startClient(): void {
    this.editingClient = { id: 0, name: '', email: '', phone: '', notes: '' };
    this.clientForm = this.emptyClientForm();
  }

  editClient(client: Client): void {
    this.editingClient = client;
    this.clientForm = { name: client.name, email: client.email, phone: client.phone, notes: client.notes ?? '' };
  }

  saveClient(): void {
    const request = this.editingClient?.id
      ? this.http.put<Client>(`/api/clients/${this.editingClient.id}`, this.clientForm)
      : this.http.post<Client>('/api/clients', this.clientForm);
    this.mutate(request, this.editingClient?.id ? 'Tenant or client updated.' : 'Tenant or client added.', () => {
      this.editingClient = null;
      this.clientForm = this.emptyClientForm();
    });
  }

  deleteClient(client: Client): void {
    if (window.confirm(`Delete ${client.name}?`)) {
      this.mutate(this.http.delete<void>(`/api/clients/${client.id}`), 'Tenant or client deleted.');
    }
  }

  startUnit(): void {
    this.editingUnit = { id: 0, unitNumber: '', size: '', monthlyRate: 0, status: 'Available', clientId: null, clientName: null };
    this.unitForm = this.emptyUnitForm();
  }

  editUnit(unit: Unit): void {
    this.editingUnit = unit;
    this.unitForm = { unitNumber: unit.unitNumber, size: unit.size, monthlyRate: unit.monthlyRate, clientId: unit.clientId };
  }

  saveUnit(): void {
    const request = this.editingUnit?.id
      ? this.http.put<Unit>(`/api/units/${this.editingUnit.id}`, this.unitForm)
      : this.http.post<Unit>('/api/units', this.unitForm);
    this.mutate(request, this.editingUnit?.id ? 'Storage unit updated.' : 'Storage unit added.', () => {
      this.editingUnit = null;
      this.unitForm = this.emptyUnitForm();
    });
  }

  deleteUnit(unit: Unit): void {
    if (window.confirm(`Delete unit ${unit.unitNumber}?`)) {
      this.mutate(this.http.delete<void>(`/api/units/${unit.id}`), 'Storage unit deleted.');
    }
  }

  startHouse(): void {
    this.editingHouse = { id: 0, address: '', monthlyRent: 0, status: 'Available', tenantId: null, tenantName: null };
    this.houseForm = this.emptyHouseForm();
  }

  editHouse(house: House): void {
    this.editingHouse = house;
    this.houseForm = { address: house.address, monthlyRent: house.monthlyRent, tenantId: house.tenantId };
  }

  saveHouse(): void {
    const request = this.editingHouse?.id
      ? this.http.put<House>(`/api/houses/${this.editingHouse.id}`, this.houseForm)
      : this.http.post<House>('/api/houses', this.houseForm);
    this.mutate(request, this.editingHouse?.id ? 'House updated.' : 'House added.', () => {
      this.editingHouse = null;
      this.houseForm = this.emptyHouseForm();
    });
  }

  deleteHouse(house: House): void {
    if (window.confirm(`Delete ${house.address}?`)) {
      this.mutate(this.http.delete<void>(`/api/houses/${house.id}`), 'House deleted.');
    }
  }

  savePayment(): void {
    if (this.paymentForm.clientId === null || this.paymentForm.amount === null) return;
    this.mutate(this.http.post<Payment>('/api/payments', this.paymentForm), 'Unit payment recorded.', () => {
      this.paymentForm = this.emptyPaymentForm();
    });
  }

  saveRentPayment(): void {
    if (this.rentPaymentForm.houseId === null || this.rentPaymentForm.amount === null) return;
    this.mutate(this.http.post<RentPayment>('/api/rent-payments', this.rentPaymentForm), 'Rent payment recorded.', () => {
      this.rentPaymentForm = this.emptyRentPaymentForm();
    });
  }

  deletePayment(payment: Payment): void {
    if (window.confirm(`Delete the payment from ${payment.clientName}?`)) {
      this.mutate(this.http.delete<void>(`/api/payments/${payment.id}`), 'Payment deleted.');
    }
  }

  deleteRentPayment(payment: RentPayment): void {
    if (window.confirm(`Delete the rent payment for ${payment.houseAddress}?`)) {
      this.mutate(this.http.delete<void>(`/api/rent-payments/${payment.id}`), 'Rent payment deleted.');
    }
  }

  private loadSession(): void {
    this.http.get<User>('/api/auth/me').subscribe({
      next: (user) => {
        this.user = user;
        this.loadDashboard();
      },
      error: (error: unknown) => {
        this.busy = false;
        if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
          this.errorMessage = this.describeError(error, 'Unable to load your account.');
        }
      },
    });
  }

  private loadDashboard(): void {
    this.busy = true;
    this.errorMessage = '';
    this.http.get<Dashboard>('/api/dashboard').subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.user = dashboard.user;
        this.busy = false;
      },
      error: (error: unknown) => {
        this.busy = false;
        this.errorMessage = this.describeError(error, 'Unable to load your workspace.');
      },
    });
  }

  private mutate<T>(request: Observable<T>, success: string, afterSuccess: () => void = () => {}): void {
    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    request.subscribe({
      next: () => {
        afterSuccess();
        this.successMessage = success;
        this.loadDashboard();
      },
      error: (error: unknown) => {
        this.busy = false;
        this.errorMessage = this.describeError(error, 'The requested change could not be saved.');
      },
    });
  }

  private describeError(error: unknown, fallback: string): string {
    if (!(error instanceof HttpErrorResponse)) return fallback;
    const body: unknown = error.error;
    if (body && typeof body === 'object') {
      if ('detail' in body && typeof body.detail === 'string') return body.detail;
      if ('message' in body && typeof body.message === 'string') return body.message;
    }
    return error.status === 0 ? 'Cannot reach the application server.' : fallback;
  }

  private clearSession(): void {
    this.user = null;
    this.dashboard = null;
    this.busy = false;
    this.page = 'dashboard';
    window.location.hash = '/login';
  }

  private pageFromHash(): Page {
    const route = window.location.hash.replace(/^#\/?/, '');
    if (route === 'clients' || route === 'units' || route === 'houses' || route === 'payments') return route;
    return 'dashboard';
  }

  private emptyClientForm(): ClientForm {
    return { name: '', email: '', phone: '', notes: '' };
  }

  private emptyUnitForm(): UnitForm {
    return { unitNumber: '', size: '', monthlyRate: null, clientId: null };
  }

  private emptyHouseForm(): HouseForm {
    return { address: '', monthlyRent: null, tenantId: null };
  }

  private emptyPaymentForm(): PaymentForm {
    return { clientId: null, amount: null, billingMonth: this.currentMonth(), method: 'ACH' };
  }

  private emptyRentPaymentForm(): RentPaymentForm {
    return { houseId: null, amount: null, billingMonth: this.currentMonth(), method: 'ACH' };
  }

  private currentMonth(): string {
    return new Date().toISOString().slice(0, 7);
  }
}
