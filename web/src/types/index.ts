export type FuelType = 'gasoline' | 'diesel' | 'lpg' | 'electric' | 'hybrid' | 'ethanol' | 'cng';

export type ExpenseCategory = 
  | 'insurance' 
  | 'parking' 
  | 'tolls' 
  | 'fines' 
  | 'carWash' 
  | 'taxes' 
  | 'registration' 
  | 'financing' 
  | 'repairs'
  | 'maintenance'
  | 'other';

export type ServiceType = 
  | 'oilChange' 
  | 'tireRotation' 
  | 'brakes' 
  | 'filters' 
  | 'battery' 
  | 'transmission' 
  | 'coolant' 
  | 'sparkPlugs' 
  | 'timing' 
  | 'inspection' 
  | 'other';

export type Units = 'metric' | 'imperial';
export type SupportedCurrency = 'DZD' | 'EUR' | 'USD' | 'CAD' | 'GBP' | 'MAD' | 'CHF';
export type SupportedLanguage = 'en' | 'fr';

export interface UserProfile {
  userId?: string;
  name: string;
  email: string;
  preferredLang: SupportedLanguage;
  units: Units;
  currency: SupportedCurrency;
  activeVehicleId?: string;
  createdAt?: any;
  updatedAt?: any;
}

export interface Vehicle {
  id: string;
  name?: string;
  make: string;
  model: string;
  year: number;
  plate?: string;
  vin?: string;
  fuelType: FuelType;
  photoUrl?: string;
  currentOdometer: number;
  notes?: string;
  createdAt?: any;
  updatedAt?: any;
}

export interface FillUp {
  id: string;
  vehicleId: string;
  date: any; // Firestore Timestamp or string
  odometer: number;
  volume: number; // Liters or Gallons
  pricePerUnit: number;
  totalCost: number;
  fuelType: FuelType;
  isFullTank: boolean;
  station?: string;
  notes?: string;
  createdAt?: any;
}

export interface Expense {
  id: string;
  vehicleId: string;
  date: any;
  category: ExpenseCategory;
  amount: number;
  description: string;
  receiptUrl?: string;
  odometer?: number;
  notes?: string;
  createdAt?: any;
}

export interface Service {
  id: string;
  vehicleId: string;
  date: any;
  type: ServiceType;
  odometer: number;
  cost: number;
  description: string;
  parts?: string[];
  receiptUrl?: string;
  notes?: string;
  createdAt?: any;
}

export interface Reminder {
  id: string;
  vehicleId: string;
  type: ServiceType | string;
  title?: string;
  intervalKm?: number;
  intervalDays?: number;
  lastServiceDate?: any;
  lastServiceOdometer?: number;
  nextDueDate?: any;
  nextDueOdometer?: number;
  isRecurring: boolean;
  isActive: boolean;
  createdAt?: any;
  updatedAt?: any;
}

export interface DashboardStats {
  totalVehicles: number;
  averageConsumption: number; // L/100km or MPG
  monthlyFuelCost: number;
  monthlyExpensesCost: number;
  monthlyServiceCost: number;
  totalMonthlyCost: number;
  dueRemindersCount: number;
}
