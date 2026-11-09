'use client';

import React, { createContext, useContext, useState, useEffect, useCallback, useRef, useMemo } from 'react';
import {
  Vehicle,
  FillUp,
  Expense,
  Service,
  Reminder,
  UserProfile,
  SupportedLanguage,
} from '@/types';
import { translations } from '@/lib/translations';
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  signOut as fbSignOut,
  onAuthStateChanged,
  updateProfile,
} from 'firebase/auth';
import {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  onSnapshot,
  serverTimestamp,
  query,
  where,
  writeBatch,
} from 'firebase/firestore';
import {
  auth,
  db,
  OperationType,
  handleFirestoreError,
  testFirestoreConnection,
} from './firebase';
import { parseFuelioCsv, exportFuelioCsv } from './fuelio-csv';

function reportFirestoreErrorSafely(
  error: unknown,
  operationType: OperationType,
  path: string | null
) {
  try {
    handleFirestoreError(error, operationType, path);
  } catch {
    // Logged by handleFirestoreError; prevent uncaught exception from crashing UI
  }
}

export type SyncStatus = 'synced' | 'syncing' | 'local' | 'error';

interface AppUser {
  uid: string;
  email: string;
  displayName?: string;
  photoURL?: string;
  isDemo?: boolean;
}

interface AppContextType {
  user: AppUser | null;
  userProfile: UserProfile;
  lang: SupportedLanguage;
  setLang: (lang: SupportedLanguage) => void;
  t: typeof translations.en;
  vehicles: Vehicle[];
  activeVehicle: Vehicle | null;
  activeVehicleId: string;
  setActiveVehicleId: (id: string) => void;
  fillUps: FillUp[];
  expenses: Expense[];
  services: Service[];
  reminders: Reminder[];
  loading: boolean;
  syncStatus: SyncStatus;
  lastSyncedAt: string | null;
  authModalOpen: boolean;
  setAuthModalOpen: (open: boolean) => void;
  addVehicleModalOpen: boolean;
  setAddVehicleModalOpen: (open: boolean) => void;

  // Auth actions
  signInWithEmail: (
    email: string,
    password: string
  ) => Promise<{ success: boolean; error?: string; consoleUrl?: string }>;
  signUpWithEmail: (
    email: string,
    password: string,
    name?: string
  ) => Promise<{ success: boolean; error?: string; consoleUrl?: string }>;
  signInWithGoogle: () => Promise<{ success: boolean; error?: string }>;
  signOut: () => Promise<void>;
  signInDemo: () => void;

  // CRUD actions
  addVehicle: (v: Omit<Vehicle, 'id'>) => Promise<string>;
  updateVehicle: (id: string, v: Partial<Vehicle>) => Promise<void>;
  deleteVehicle: (id: string) => Promise<void>;

  addFillUp: (f: Omit<FillUp, 'id'>) => Promise<string>;
  deleteFillUp: (id: string) => Promise<void>;

  addExpense: (e: Omit<Expense, 'id'>) => Promise<string>;
  deleteExpense: (id: string) => Promise<void>;

  addService: (s: Omit<Service, 'id'>) => Promise<string>;
  deleteService: (id: string) => Promise<void>;

  addReminder: (r: Omit<Reminder, 'id'>) => Promise<string>;
  completeReminder: (id: string) => Promise<void>;
  deleteReminder: (id: string) => Promise<void>;

  // Backup & Preferences
  updatePreferences: (prefs: Partial<UserProfile>) => void;
  syncToCloudNow: () => Promise<{ success: boolean; error?: string; count?: number }>;
  exportBackupJson: () => string;
  importBackupJson: (jsonData: string) => boolean;
  exportFuelioCsvForVehicle: (vehicleId?: string) => string;
  importFuelioCsv: (
    csvText: string,
    targetVehicleId?: string
  ) => Promise<{
    success: boolean;
    importedFillUps: number;
    importedExpenses: number;
    error?: string;
  }>;
}

const defaultProfile: UserProfile = {
  name: 'Karim Mansouri',
  email: 'karim.mansouri@netcar.app',
  preferredLang: 'fr',
  units: 'metric',
  currency: 'DZD',
  activeVehicleId: 'veh-1',
};

const initialVehicles: Vehicle[] = [
  {
    id: 'veh-1',
    name: 'Daily Driver',
    make: 'Volkswagen',
    model: 'Golf VII 2.0 TDI',
    year: 2021,
    plate: '00124-116-16',
    fuelType: 'diesel',
    currentOdometer: 64250,
    photoUrl:
      'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?auto=format&fit=crop&w=800&q=80',
    notes: 'Vidange chaque 10 000 km avec filtre à huile',
  },
  {
    id: 'veh-2',
    name: 'Weekend Cruiser',
    make: 'Renault',
    model: 'Clio V 1.0 TCe',
    year: 2022,
    plate: '09834-118-31',
    fuelType: 'gasoline',
    currentOdometer: 28400,
    photoUrl:
      'https://images.unsplash.com/photo-1619767886558-efdc259cde1a?auto=format&fit=crop&w=800&q=80',
    notes: 'Véhicule urbain',
  },
];

const initialFillUps: FillUp[] = [
  {
    id: 'f-1',
    vehicleId: 'veh-1',
    date: '2026-11-05T10:00:00.000Z',
    odometer: 64250,
    volume: 50.0,
    pricePerUnit: 29.0,
    totalCost: 1450.0,
    fuelType: 'diesel',
    isFullTank: true,
    station: 'Naftal Sidi Fredj',
    notes: 'Plein de gasoil Naftal',
  },
  {
    id: 'f-2',
    vehicleId: 'veh-1',
    date: '2026-10-25T10:00:00.000Z',
    odometer: 63480,
    volume: 48.0,
    pricePerUnit: 29.0,
    totalCost: 1392.0,
    fuelType: 'diesel',
    isFullTank: true,
    station: 'Naftal Autoroute Est-Ouest',
  },
  {
    id: 'f-4',
    vehicleId: 'veh-1',
    date: '2026-10-12T09:15:00.000Z',
    odometer: 62710,
    volume: 47.5,
    pricePerUnit: 29.0,
    totalCost: 1377.5,
    fuelType: 'diesel',
    isFullTank: true,
    station: 'Naftal Chéraga',
    notes: 'Trajet mixte ville / rocade',
  },
  {
    id: 'f-5',
    vehicleId: 'veh-1',
    date: '2026-09-28T17:40:00.000Z',
    odometer: 61900,
    volume: 46.2,
    pricePerUnit: 29.0,
    totalCost: 1339.8,
    fuelType: 'diesel',
    isFullTank: true,
    station: 'Naftal Hydra',
  },
  {
    id: 'f-6',
    vehicleId: 'veh-1',
    date: '2026-09-14T11:20:00.000Z',
    odometer: 61090,
    volume: 49.0,
    pricePerUnit: 29.0,
    totalCost: 1421.0,
    fuelType: 'diesel',
    isFullTank: true,
    station: 'Naftal Dar El Beïda',
  },
  {
    id: 'f-7',
    vehicleId: 'veh-1',
    date: '2026-08-30T08:45:00.000Z',
    odometer: 60300,
    volume: 45.5,
    pricePerUnit: 29.0,
    totalCost: 1319.5,
    fuelType: 'diesel',
    isFullTank: true,
    station: 'Naftal Sidi Fredj',
  },
  {
    id: 'f-3',
    vehicleId: 'veh-2',
    date: '2026-11-02T10:00:00.000Z',
    odometer: 28400,
    volume: 38.0,
    pricePerUnit: 45.6,
    totalCost: 1732.8,
    fuelType: 'gasoline',
    isFullTank: true,
    station: 'Naftal Bir Mourad Rais',
  },
  {
    id: 'f-8',
    vehicleId: 'veh-2',
    date: '2026-10-18T14:30:00.000Z',
    odometer: 27750,
    volume: 36.5,
    pricePerUnit: 45.6,
    totalCost: 1664.4,
    fuelType: 'gasoline',
    isFullTank: true,
    station: 'Naftal Kouba',
  },
  {
    id: 'f-9',
    vehicleId: 'veh-2',
    date: '2026-10-03T09:00:00.000Z',
    odometer: 27120,
    volume: 37.0,
    pricePerUnit: 45.6,
    totalCost: 1687.2,
    fuelType: 'gasoline',
    isFullTank: true,
    station: 'Naftal Bir Mourad Rais',
  },
];

const initialExpenses: Expense[] = [
  {
    id: 'e-1',
    vehicleId: 'veh-1',
    date: '2026-10-29T10:00:00.000Z',
    category: 'parking',
    amount: 500.0,
    description: 'Parking Centre Ville',
    odometer: 63900,
  },
  {
    id: 'e-2',
    vehicleId: 'veh-1',
    date: '2026-10-14T10:00:00.000Z',
    category: 'insurance',
    amount: 18500.0,
    description: 'Assurance Tous Risques CAAT',
  },
  {
    id: 'e-3',
    vehicleId: 'veh-1',
    date: '2026-09-29T10:00:00.000Z',
    category: 'carWash',
    amount: 800.0,
    description: 'Lavage complet intérieur / extérieur',
  },
];

const initialServices: Service[] = [
  {
    id: 's-1',
    vehicleId: 'veh-1',
    date: '2026-09-24T10:00:00.000Z',
    type: 'oilChange',
    odometer: 60000,
    cost: 7500.0,
    description: 'Vidange huile Castrol 5W-40 + filtre à huile et à air',
    parts: ['Filtre à huile Mann', 'Huile moteur 5W-40 (5L)', 'Filtre à air'],
  },
  {
    id: 's-2',
    vehicleId: 'veh-1',
    date: '2026-07-11T10:00:00.000Z',
    type: 'brakes',
    odometer: 54000,
    cost: 12000.0,
    description: 'Remplacement plaquettes de frein avant Brembo',
    parts: ['Plaquettes de frein avant'],
  },
];

const initialReminders: Reminder[] = [
  {
    id: 'r-1',
    vehicleId: 'veh-1',
    type: 'oilChange',
    title: 'Scheduled Oil Change',
    intervalKm: 15000,
    intervalDays: 365,
    lastServiceOdometer: 60000,
    nextDueOdometer: 75000,
    nextDueDate: '2027-05-07T10:00:00.000Z',
    isRecurring: true,
    isActive: true,
  },
  {
    id: 'r-2',
    vehicleId: 'veh-1',
    type: 'inspection',
    title: 'Technical Inspection (Contrôle Technique)',
    intervalDays: 730,
    nextDueDate: '2027-01-07T10:00:00.000Z',
    isRecurring: true,
    isActive: true,
  },
  {
    id: 'r-3',
    vehicleId: 'veh-2',
    type: 'oilChange',
    title: 'Annual Oil & Filter Service',
    intervalKm: 15000,
    nextDueOdometer: 30000,
    isRecurring: true,
    isActive: true,
  },
];

function stripUndefined<T extends Record<string, any>>(obj: T): Record<string, any> {
  const result: Record<string, any> = {};
  for (const [k, v] of Object.entries(obj)) {
    if (v !== undefined && v !== '') {
      result[k] = v;
    }
  }
  return result;
}

function normalizeTimestampField(val: any): string | undefined {
  if (!val) return undefined;
  if (typeof val === 'string') return val;
  if (typeof val?.toDate === 'function') {
    return val.toDate().toISOString();
  }
  return String(val);
}

const AppContext = createContext<AppContextType | null>(null);

export function AppProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AppUser | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [addVehicleModalOpen, setAddVehicleModalOpen] = useState(false);
  const [userProfile, setUserProfile] = useState<UserProfile>(defaultProfile);
  const [lang, setLangState] = useState<SupportedLanguage>('fr');
  const [vehicles, setVehicles] = useState<Vehicle[]>(initialVehicles);
  const [activeVehicleId, setActiveVehicleIdState] = useState<string>('veh-1');
  const [fillUps, setFillUps] = useState<FillUp[]>(initialFillUps);
  const [expenses, setExpenses] = useState<Expense[]>(initialExpenses);
  const [services, setServices] = useState<Service[]>(initialServices);
  const [reminders, setReminders] = useState<Reminder[]>(initialReminders);
  const [loading] = useState<boolean>(false);
  const [syncStatus, setSyncStatus] = useState<SyncStatus>('local');
  const [lastSyncedAt, setLastSyncedAt] = useState<string | null>(null);
  const isCloudReadyRef = useRef<boolean>(false);

  const saveToLocal = useCallback((key: string, data: any) => {
    if (typeof window !== 'undefined') {
      try {
        localStorage.setItem(key, JSON.stringify(data));
      } catch {
        // Ignore storage quota errors
      }
    }
  }, []);

  // Load initial state from localStorage & test Firestore connection
  useEffect(() => {
    testFirestoreConnection();

    if (typeof window !== 'undefined') {
      const savedLang = localStorage.getItem('netcar_lang') as SupportedLanguage;
      if (savedLang === 'en' || savedLang === 'fr') {
        setLangState(savedLang);
      }

      const savedActiveVehId = localStorage.getItem('netcar_active_vehicle_id');
      if (savedActiveVehId) {
        setActiveVehicleIdState(savedActiveVehId);
      }

      const savedVehicles = localStorage.getItem('netcar_vehicles');
      if (savedVehicles) {
        try {
          const parsed = JSON.parse(savedVehicles);
          if (Array.isArray(parsed) && parsed.length > 0) {
            setVehicles(parsed);
          }
        } catch {
          // Ignore malformed JSON
        }
      }

      const savedFillups = localStorage.getItem('netcar_fillups');
      if (savedFillups) {
        try {
          setFillUps(JSON.parse(savedFillups));
        } catch {
          // Ignore malformed JSON
        }
      }

      const savedExpenses = localStorage.getItem('netcar_expenses');
      if (savedExpenses) {
        try {
          setExpenses(JSON.parse(savedExpenses));
        } catch {
          // Ignore malformed JSON
        }
      }

      const savedServices = localStorage.getItem('netcar_services');
      if (savedServices) {
        try {
          setServices(JSON.parse(savedServices));
        } catch {
          // Ignore malformed JSON
        }
      }

      const savedReminders = localStorage.getItem('netcar_reminders');
      if (savedReminders) {
        try {
          setReminders(JSON.parse(savedReminders));
        } catch {
          // Ignore malformed JSON
        }
      }

      const savedProfile = localStorage.getItem('netcar_profile');
      if (savedProfile) {
        try {
          const parsedProfile = JSON.parse(savedProfile);
          setUserProfile(parsedProfile);
          if (parsedProfile.activeVehicleId && !savedActiveVehId) {
            setActiveVehicleIdState(parsedProfile.activeVehicleId);
          }
        } catch {
          // Ignore malformed JSON
        }
      }

      const savedDemoUser = localStorage.getItem('netcar_demo_user');
      if (savedDemoUser && !auth.currentUser) {
        try {
          setUser(JSON.parse(savedDemoUser));
        } catch {
          // Ignore malformed JSON
        }
      }
    }
  }, []);

  // Listen to Firebase Auth state & establish real-time Firestore listeners
  useEffect(() => {
    if (typeof window === 'undefined') return;

    let unsubProfile: (() => void) | null = null;
    let unsubVehicles: (() => void) | null = null;
    let unsubFillups: (() => void) | null = null;
    let unsubExpenses: (() => void) | null = null;
    let unsubServices: (() => void) | null = null;
    let unsubReminders: (() => void) | null = null;

    const cleanupListeners = () => {
      if (unsubProfile) unsubProfile();
      if (unsubVehicles) unsubVehicles();
      if (unsubFillups) unsubFillups();
      if (unsubExpenses) unsubExpenses();
      if (unsubServices) unsubServices();
      if (unsubReminders) unsubReminders();
      unsubProfile = null;
      unsubVehicles = null;
      unsubFillups = null;
      unsubExpenses = null;
      unsubServices = null;
      unsubReminders = null;
      isCloudReadyRef.current = false;
    };

    const unsubscribeAuth = onAuthStateChanged(auth, async (fbUser) => {
      cleanupListeners();

      if (!fbUser) {
        const savedDemo = localStorage.getItem('netcar_demo_user');
        if (savedDemo) {
          try {
            setUser(JSON.parse(savedDemo));
          } catch {
            setUser(null);
          }
        } else {
          setUser(null);
        }
        setSyncStatus('local');
        return;
      }

      // Real Firebase authenticated user
      localStorage.removeItem('netcar_demo_user');
      const uid = fbUser.uid;
      const email = fbUser.email || 'driver@netcar.app';
      const displayName = fbUser.displayName || email.split('@')[0] || 'Pilot';
      const appUser: AppUser = {
        uid,
        email,
        displayName,
        photoURL: fbUser.photoURL || undefined,
        isDemo: false,
      };
      setUser(appUser);
      setSyncStatus('syncing');

      const userDocPath = `users/${uid}`;
      const userDocRef = doc(db, 'users', uid);

      try {
        const userSnap = await getDoc(userDocRef);
        const currentSavedActiveId =
          localStorage.getItem('netcar_active_vehicle_id') || activeVehicleId || 'veh-1';

        if (!userSnap.exists()) {
          // Initialize user profile document in Firestore
          const initialUserDoc = stripUndefined({
            userId: uid,
            name: displayName.slice(0, 200),
            email: email.slice(0, 320),
            preferredLang: lang || 'fr',
            units: userProfile.units || 'metric',
            currency: userProfile.currency || 'DZD',
            activeVehicleId: currentSavedActiveId,
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          });
          await setDoc(userDocRef, initialUserDoc);
        }
        isCloudReadyRef.current = true;

        // Check if user has any vehicles in Firestore yet; if not, seed initial/local garage
        const vehiclesColRef = collection(db, 'users', uid, 'vehicles');
        const existingVehSnap = await getDocs(query(vehiclesColRef, where('userId', '==', uid)));
        if (existingVehSnap.empty) {
          const seedVehicles = vehicles.length > 0 ? vehicles : initialVehicles;
          for (const v of seedVehicles) {
            const vRef = doc(db, 'users', uid, 'vehicles', v.id);
            await setDoc(
              vRef,
              stripUndefined({
                id: v.id,
                userId: uid,
                name: v.name?.slice(0, 100),
                make: v.make.slice(0, 100),
                model: v.model.slice(0, 100),
                year: Number(v.year),
                plate: v.plate?.slice(0, 30),
                vin: v.vin?.slice(0, 30),
                fuelType: v.fuelType,
                photoUrl: v.photoUrl?.slice(0, 2048),
                currentOdometer: Number(v.currentOdometer) || 0,
                notes: v.notes?.slice(0, 5000),
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              })
            );
          }

          for (const f of fillUps) {
            const fRef = doc(db, 'users', uid, 'fillups', f.id);
            await setDoc(
              fRef,
              stripUndefined({
                id: f.id,
                userId: uid,
                vehicleId: f.vehicleId,
                date: String(f.date),
                odometer: Number(f.odometer),
                volume: Number(f.volume),
                pricePerUnit: Number(f.pricePerUnit),
                totalCost: Number(f.totalCost),
                fuelType: f.fuelType,
                isFullTank: Boolean(f.isFullTank),
                station: f.station?.slice(0, 200),
                notes: f.notes?.slice(0, 5000),
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              })
            );
          }

          for (const e of expenses) {
            const eRef = doc(db, 'users', uid, 'expenses', e.id);
            await setDoc(
              eRef,
              stripUndefined({
                id: e.id,
                userId: uid,
                vehicleId: e.vehicleId,
                date: String(e.date),
                category: e.category,
                amount: Number(e.amount),
                description: e.description.slice(0, 500),
                receiptUrl: e.receiptUrl?.slice(0, 2048),
                odometer: e.odometer !== undefined ? Number(e.odometer) : undefined,
                notes: e.notes?.slice(0, 5000),
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              })
            );
          }

          for (const s of services) {
            const sRef = doc(db, 'users', uid, 'services', s.id);
            await setDoc(
              sRef,
              stripUndefined({
                id: s.id,
                userId: uid,
                vehicleId: s.vehicleId,
                date: String(s.date),
                type: s.type,
                odometer: Number(s.odometer),
                cost: Number(s.cost),
                description: s.description.slice(0, 500),
                parts: s.parts && s.parts.length > 0 ? s.parts.slice(0, 50) : undefined,
                receiptUrl: s.receiptUrl?.slice(0, 2048),
                notes: s.notes?.slice(0, 5000),
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              })
            );
          }

          for (const r of reminders) {
            const rRef = doc(db, 'users', uid, 'reminders', r.id);
            await setDoc(
              rRef,
              stripUndefined({
                id: r.id,
                userId: uid,
                vehicleId: r.vehicleId,
                type: String(r.type),
                title: r.title?.slice(0, 200),
                intervalKm: r.intervalKm !== undefined ? Number(r.intervalKm) : undefined,
                intervalDays: r.intervalDays !== undefined ? Number(r.intervalDays) : undefined,
                lastServiceDate: r.lastServiceDate ? String(r.lastServiceDate) : undefined,
                lastServiceOdometer:
                  r.lastServiceOdometer !== undefined ? Number(r.lastServiceOdometer) : undefined,
                nextDueDate: r.nextDueDate ? String(r.nextDueDate) : undefined,
                nextDueOdometer:
                  r.nextDueOdometer !== undefined ? Number(r.nextDueOdometer) : undefined,
                isRecurring: Boolean(r.isRecurring),
                isActive: Boolean(r.isActive),
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              })
            );
          }
        }
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.WRITE, userDocPath);
      }

      // Attach Real-Time Listeners for Multi-Device Auto-Sync
      unsubProfile = onSnapshot(
        userDocRef,
        (snap) => {
          if (snap.exists()) {
            const data = snap.data();
            const syncedProfile: UserProfile = {
              userId: uid,
              name: data.name || displayName,
              email: data.email || email,
              preferredLang: data.preferredLang || 'fr',
              units: data.units || 'metric',
              currency: data.currency || 'DZD',
              activeVehicleId: data.activeVehicleId,
              createdAt: normalizeTimestampField(data.createdAt),
              updatedAt: normalizeTimestampField(data.updatedAt),
            };
            setUserProfile(syncedProfile);
            saveToLocal('netcar_profile', syncedProfile);
            if (data.preferredLang === 'en' || data.preferredLang === 'fr') {
              setLangState(data.preferredLang);
              localStorage.setItem('netcar_lang', data.preferredLang);
            }
            if (data.activeVehicleId) {
              setActiveVehicleIdState(data.activeVehicleId);
              localStorage.setItem('netcar_active_vehicle_id', data.activeVehicleId);
            }
            setSyncStatus('synced');
            setLastSyncedAt(new Date().toISOString());
          }
        },
        (error) => {
          setSyncStatus('error');
          reportFirestoreErrorSafely(error, OperationType.GET, userDocPath);
        }
      );

      const vehiclesPath = `users/${uid}/vehicles`;
      unsubVehicles = onSnapshot(
        query(collection(db, vehiclesPath), where('userId', '==', uid)),
        (snap) => {
          const list: Vehicle[] = snap.docs.map((d) => {
            const raw = d.data();
            return {
              id: raw.id || d.id,
              name: raw.name,
              make: raw.make,
              model: raw.model,
              year: Number(raw.year),
              plate: raw.plate,
              vin: raw.vin,
              fuelType: raw.fuelType,
              photoUrl: raw.photoUrl,
              currentOdometer: Number(raw.currentOdometer) || 0,
              notes: raw.notes,
              createdAt: normalizeTimestampField(raw.createdAt),
              updatedAt: normalizeTimestampField(raw.updatedAt),
            };
          });
          setVehicles(list);
          saveToLocal('netcar_vehicles', list);
          setSyncStatus('synced');
          setLastSyncedAt(new Date().toISOString());
        },
        (error) => {
          setSyncStatus('error');
          reportFirestoreErrorSafely(error, OperationType.LIST, vehiclesPath);
        }
      );

      const fillupsPath = `users/${uid}/fillups`;
      unsubFillups = onSnapshot(
        query(collection(db, fillupsPath), where('userId', '==', uid)),
        (snap) => {
          const list: FillUp[] = snap.docs.map((d) => {
            const raw = d.data();
            return {
              id: raw.id || d.id,
              vehicleId: raw.vehicleId,
              date: raw.date,
              odometer: Number(raw.odometer),
              volume: Number(raw.volume),
              pricePerUnit: Number(raw.pricePerUnit),
              totalCost: Number(raw.totalCost),
              fuelType: raw.fuelType,
              isFullTank: Boolean(raw.isFullTank),
              station: raw.station,
              notes: raw.notes,
              createdAt: normalizeTimestampField(raw.createdAt),
            };
          });
          setFillUps(list);
          saveToLocal('netcar_fillups', list);
          setSyncStatus('synced');
          setLastSyncedAt(new Date().toISOString());
        },
        (error) => {
          setSyncStatus('error');
          reportFirestoreErrorSafely(error, OperationType.LIST, fillupsPath);
        }
      );

      const expensesPath = `users/${uid}/expenses`;
      unsubExpenses = onSnapshot(
        query(collection(db, expensesPath), where('userId', '==', uid)),
        (snap) => {
          const list: Expense[] = snap.docs.map((d) => {
            const raw = d.data();
            return {
              id: raw.id || d.id,
              vehicleId: raw.vehicleId,
              date: raw.date,
              category: raw.category,
              amount: Number(raw.amount),
              description: raw.description,
              receiptUrl: raw.receiptUrl,
              odometer: raw.odometer !== undefined ? Number(raw.odometer) : undefined,
              notes: raw.notes,
              createdAt: normalizeTimestampField(raw.createdAt),
            };
          });
          setExpenses(list);
          saveToLocal('netcar_expenses', list);
          setSyncStatus('synced');
          setLastSyncedAt(new Date().toISOString());
        },
        (error) => {
          setSyncStatus('error');
          reportFirestoreErrorSafely(error, OperationType.LIST, expensesPath);
        }
      );

      const servicesPath = `users/${uid}/services`;
      unsubServices = onSnapshot(
        query(collection(db, servicesPath), where('userId', '==', uid)),
        (snap) => {
          const list: Service[] = snap.docs.map((d) => {
            const raw = d.data();
            return {
              id: raw.id || d.id,
              vehicleId: raw.vehicleId,
              date: raw.date,
              type: raw.type,
              odometer: Number(raw.odometer),
              cost: Number(raw.cost),
              description: raw.description,
              parts: raw.parts,
              receiptUrl: raw.receiptUrl,
              notes: raw.notes,
              createdAt: normalizeTimestampField(raw.createdAt),
            };
          });
          setServices(list);
          saveToLocal('netcar_services', list);
          setSyncStatus('synced');
          setLastSyncedAt(new Date().toISOString());
        },
        (error) => {
          setSyncStatus('error');
          reportFirestoreErrorSafely(error, OperationType.LIST, servicesPath);
        }
      );

      const remindersPath = `users/${uid}/reminders`;
      unsubReminders = onSnapshot(
        query(collection(db, remindersPath), where('userId', '==', uid)),
        (snap) => {
          const list: Reminder[] = snap.docs.map((d) => {
            const raw = d.data();
            return {
              id: raw.id || d.id,
              vehicleId: raw.vehicleId,
              type: raw.type,
              title: raw.title,
              intervalKm: raw.intervalKm !== undefined ? Number(raw.intervalKm) : undefined,
              intervalDays: raw.intervalDays !== undefined ? Number(raw.intervalDays) : undefined,
              lastServiceDate: raw.lastServiceDate,
              lastServiceOdometer:
                raw.lastServiceOdometer !== undefined
                  ? Number(raw.lastServiceOdometer)
                  : undefined,
              nextDueDate: raw.nextDueDate,
              nextDueOdometer:
                raw.nextDueOdometer !== undefined ? Number(raw.nextDueOdometer) : undefined,
              isRecurring: Boolean(raw.isRecurring),
              isActive: Boolean(raw.isActive),
              createdAt: normalizeTimestampField(raw.createdAt),
              updatedAt: normalizeTimestampField(raw.updatedAt),
            };
          });
          setReminders(list);
          saveToLocal('netcar_reminders', list);
          setSyncStatus('synced');
          setLastSyncedAt(new Date().toISOString());
        },
        (error) => {
          setSyncStatus('error');
          reportFirestoreErrorSafely(error, OperationType.LIST, remindersPath);
        }
      );
    });

    return () => {
      cleanupListeners();
      unsubscribeAuth();
    };
  }, []);

  // Switch active vehicle and persist across localStorage & Firestore
  const setActiveVehicleId = useCallback(
    (id: string) => {
      setActiveVehicleIdState(id);
      if (typeof window !== 'undefined') {
        localStorage.setItem('netcar_active_vehicle_id', id);
      }
      setUserProfile((prev) => {
        const updated = { ...prev, activeVehicleId: id };
        saveToLocal('netcar_profile', updated);
        return updated;
      });

      const currentUid = auth.currentUser?.uid;
      if (currentUid && isCloudReadyRef.current) {
        const userDocPath = `users/${currentUid}`;
        setSyncStatus('syncing');
        updateDoc(doc(db, 'users', currentUid), {
          activeVehicleId: id,
          updatedAt: serverTimestamp(),
        })
          .then(() => {
            setSyncStatus('synced');
            setLastSyncedAt(new Date().toISOString());
          })
          .catch((error) => {
            reportFirestoreErrorSafely(error, OperationType.UPDATE, userDocPath);
          });
      }
    },
    [saveToLocal]
  );

  const setLang = useCallback(
    (newLang: SupportedLanguage) => {
      setLangState(newLang);
      if (typeof window !== 'undefined') {
        localStorage.setItem('netcar_lang', newLang);
      }
      setUserProfile((prev) => {
        const updated = { ...prev, preferredLang: newLang };
        saveToLocal('netcar_profile', updated);
        return updated;
      });

      const currentUid = auth.currentUser?.uid;
      if (currentUid && isCloudReadyRef.current) {
        const userDocPath = `users/${currentUid}`;
        updateDoc(doc(db, 'users', currentUid), {
          preferredLang: newLang,
          updatedAt: serverTimestamp(),
        }).catch((error) => {
          reportFirestoreErrorSafely(error, OperationType.UPDATE, userDocPath);
        });
      }
    },
    [saveToLocal]
  );

  const activeVehicle =
    vehicles.find((v) => v.id === activeVehicleId) || vehicles[0] || null;

  // Keep activeVehicleId aligned if current activeVehicleId no longer exists in vehicles list
  useEffect(() => {
    if (vehicles.length > 0 && !vehicles.some((v) => v.id === activeVehicleId)) {
      setActiveVehicleIdState(vehicles[0].id);
      if (typeof window !== 'undefined') {
        localStorage.setItem('netcar_active_vehicle_id', vehicles[0].id);
      }
    }
  }, [vehicles, activeVehicleId]);

  // CRUD Implementations with Firestore Auto-Sync
  const addVehicle = async (v: Omit<Vehicle, 'id'>) => {
    const id = `veh-${Date.now()}`;
    const newVehicle: Vehicle = {
      ...v,
      id,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    setVehicles((prev) => {
      const updated = [...prev, newVehicle];
      saveToLocal('netcar_vehicles', updated);
      return updated;
    });

    // Always make the newly added vehicle the active vehicle immediately
    setActiveVehicleId(id);

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/vehicles/${id}`;
      setSyncStatus('syncing');
      try {
        await setDoc(
          doc(db, 'users', currentUid, 'vehicles', id),
          stripUndefined({
            id,
            userId: currentUid,
            name: v.name?.slice(0, 100),
            make: v.make.slice(0, 100),
            model: v.model.slice(0, 100),
            year: Number(v.year),
            plate: v.plate?.slice(0, 30),
            vin: v.vin?.slice(0, 30),
            fuelType: v.fuelType,
            photoUrl: v.photoUrl?.slice(0, 2048),
            currentOdometer: Number(v.currentOdometer) || 0,
            notes: v.notes?.slice(0, 5000),
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          })
        );
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.CREATE, path);
      }
    }

    return id;
  };

  const updateVehicle = async (id: string, updates: Partial<Vehicle>) => {
    setVehicles((prev) => {
      const updated = prev.map((v) =>
        v.id === id ? { ...v, ...updates, updatedAt: new Date().toISOString() } : v
      );
      saveToLocal('netcar_vehicles', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/vehicles/${id}`;
      setSyncStatus('syncing');
      try {
        const cleanUpdates = stripUndefined({
          name: updates.name !== undefined ? updates.name.slice(0, 100) : undefined,
          make: updates.make !== undefined ? updates.make.slice(0, 100) : undefined,
          model: updates.model !== undefined ? updates.model.slice(0, 100) : undefined,
          year: updates.year !== undefined ? Number(updates.year) : undefined,
          plate: updates.plate !== undefined ? updates.plate.slice(0, 30) : undefined,
          vin: updates.vin !== undefined ? updates.vin.slice(0, 30) : undefined,
          fuelType: updates.fuelType,
          photoUrl: updates.photoUrl !== undefined ? updates.photoUrl.slice(0, 2048) : undefined,
          currentOdometer:
            updates.currentOdometer !== undefined ? Number(updates.currentOdometer) : undefined,
          notes: updates.notes !== undefined ? updates.notes.slice(0, 5000) : undefined,
          updatedAt: serverTimestamp(),
        });
        await updateDoc(doc(db, 'users', currentUid, 'vehicles', id), cleanUpdates);
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.UPDATE, path);
      }
    }
  };

  const deleteVehicle = async (id: string) => {
    let nextActiveId: string | null = null;
    setVehicles((prev) => {
      const updated = prev.filter((v) => v.id !== id);
      saveToLocal('netcar_vehicles', updated);
      if (activeVehicleId === id && updated.length > 0) {
        nextActiveId = updated[0].id;
      }
      return updated;
    });

    if (nextActiveId) {
      setActiveVehicleId(nextActiveId);
    }

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/vehicles/${id}`;
      setSyncStatus('syncing');
      try {
        await deleteDoc(doc(db, 'users', currentUid, 'vehicles', id));
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.DELETE, path);
      }
    }
  };

  const addFillUp = async (f: Omit<FillUp, 'id'>) => {
    const id = `f-${Date.now()}`;
    const newFillUp: FillUp = { ...f, id, createdAt: new Date().toISOString() };
    setFillUps((prev) => {
      const updated = [newFillUp, ...prev];
      saveToLocal('netcar_fillups', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/fillups/${id}`;
      setSyncStatus('syncing');
      try {
        await setDoc(
          doc(db, 'users', currentUid, 'fillups', id),
          stripUndefined({
            id,
            userId: currentUid,
            vehicleId: f.vehicleId,
            date: String(f.date),
            odometer: Number(f.odometer),
            volume: Number(f.volume),
            pricePerUnit: Number(f.pricePerUnit),
            totalCost: Number(f.totalCost),
            fuelType: f.fuelType,
            isFullTank: Boolean(f.isFullTank),
            station: f.station?.slice(0, 200),
            notes: f.notes?.slice(0, 5000),
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          })
        );
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.CREATE, path);
      }
    }

    // Update vehicle odometer if fill-up odometer is higher
    if (f.vehicleId && f.odometer) {
      const targetVeh = vehicles.find((v) => v.id === f.vehicleId);
      if (targetVeh && f.odometer > targetVeh.currentOdometer) {
        await updateVehicle(f.vehicleId, { currentOdometer: f.odometer });
      }
    }

    return id;
  };

  const deleteFillUp = async (id: string) => {
    setFillUps((prev) => {
      const updated = prev.filter((f) => f.id !== id);
      saveToLocal('netcar_fillups', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/fillups/${id}`;
      setSyncStatus('syncing');
      try {
        await deleteDoc(doc(db, 'users', currentUid, 'fillups', id));
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.DELETE, path);
      }
    }
  };

  const addExpense = async (e: Omit<Expense, 'id'>) => {
    const id = `e-${Date.now()}`;
    const newExpense: Expense = { ...e, id, createdAt: new Date().toISOString() };
    setExpenses((prev) => {
      const updated = [newExpense, ...prev];
      saveToLocal('netcar_expenses', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/expenses/${id}`;
      setSyncStatus('syncing');
      try {
        await setDoc(
          doc(db, 'users', currentUid, 'expenses', id),
          stripUndefined({
            id,
            userId: currentUid,
            vehicleId: e.vehicleId,
            date: String(e.date),
            category: e.category,
            amount: Number(e.amount),
            description: e.description.slice(0, 500),
            receiptUrl: e.receiptUrl?.slice(0, 2048),
            odometer: e.odometer !== undefined ? Number(e.odometer) : undefined,
            notes: e.notes?.slice(0, 5000),
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          })
        );
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.CREATE, path);
      }
    }
    return id;
  };

  const deleteExpense = async (id: string) => {
    setExpenses((prev) => {
      const updated = prev.filter((e) => e.id !== id);
      saveToLocal('netcar_expenses', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/expenses/${id}`;
      setSyncStatus('syncing');
      try {
        await deleteDoc(doc(db, 'users', currentUid, 'expenses', id));
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.DELETE, path);
      }
    }
  };

  const addService = async (s: Omit<Service, 'id'>) => {
    const id = `s-${Date.now()}`;
    const newService: Service = { ...s, id, createdAt: new Date().toISOString() };
    setServices((prev) => {
      const updated = [newService, ...prev];
      saveToLocal('netcar_services', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/services/${id}`;
      setSyncStatus('syncing');
      try {
        await setDoc(
          doc(db, 'users', currentUid, 'services', id),
          stripUndefined({
            id,
            userId: currentUid,
            vehicleId: s.vehicleId,
            date: String(s.date),
            type: s.type,
            odometer: Number(s.odometer),
            cost: Number(s.cost),
            description: s.description.slice(0, 500),
            parts: s.parts && s.parts.length > 0 ? s.parts.slice(0, 50) : undefined,
            receiptUrl: s.receiptUrl?.slice(0, 2048),
            notes: s.notes?.slice(0, 5000),
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          })
        );
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.CREATE, path);
      }
    }

    if (s.vehicleId && s.odometer) {
      const targetVeh = vehicles.find((v) => v.id === s.vehicleId);
      if (targetVeh && s.odometer > targetVeh.currentOdometer) {
        await updateVehicle(s.vehicleId, { currentOdometer: s.odometer });
      }
    }
    return id;
  };

  const deleteService = async (id: string) => {
    setServices((prev) => {
      const updated = prev.filter((s) => s.id !== id);
      saveToLocal('netcar_services', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/services/${id}`;
      setSyncStatus('syncing');
      try {
        await deleteDoc(doc(db, 'users', currentUid, 'services', id));
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.DELETE, path);
      }
    }
  };

  const addReminder = async (r: Omit<Reminder, 'id'>) => {
    const id = `r-${Date.now()}`;
    const newReminder: Reminder = {
      ...r,
      id,
      isActive: true,
      createdAt: new Date().toISOString(),
    };
    setReminders((prev) => {
      const updated = [newReminder, ...prev];
      saveToLocal('netcar_reminders', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/reminders/${id}`;
      setSyncStatus('syncing');
      try {
        await setDoc(
          doc(db, 'users', currentUid, 'reminders', id),
          stripUndefined({
            id,
            userId: currentUid,
            vehicleId: r.vehicleId,
            type: String(r.type),
            title: r.title?.slice(0, 200),
            intervalKm: r.intervalKm !== undefined ? Number(r.intervalKm) : undefined,
            intervalDays: r.intervalDays !== undefined ? Number(r.intervalDays) : undefined,
            lastServiceDate: r.lastServiceDate ? String(r.lastServiceDate) : undefined,
            lastServiceOdometer:
              r.lastServiceOdometer !== undefined ? Number(r.lastServiceOdometer) : undefined,
            nextDueDate: r.nextDueDate ? String(r.nextDueDate) : undefined,
            nextDueOdometer:
              r.nextDueOdometer !== undefined ? Number(r.nextDueOdometer) : undefined,
            isRecurring: Boolean(r.isRecurring),
            isActive: true,
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          })
        );
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.CREATE, path);
      }
    }
    return id;
  };

  const completeReminder = async (id: string) => {
    const target = reminders.find((r) => r.id === id);
    if (!target) return;

    const nowIso = new Date().toISOString();
    let firestoreUpdate: Record<string, any> = {};

    if (target.isRecurring && target.intervalKm && activeVehicle) {
      const nextOdo = activeVehicle.currentOdometer + (target.intervalKm || 10000);
      firestoreUpdate = {
        lastServiceOdometer: activeVehicle.currentOdometer,
        nextDueOdometer: nextOdo,
        lastServiceDate: nowIso,
        updatedAt: serverTimestamp(),
      };
      setReminders((prev) => {
        const updated = prev.map((r) =>
          r.id === id
            ? {
                ...r,
                lastServiceOdometer: activeVehicle.currentOdometer,
                nextDueOdometer: nextOdo,
                lastServiceDate: nowIso,
                updatedAt: nowIso,
              }
            : r
        );
        saveToLocal('netcar_reminders', updated);
        return updated;
      });
    } else {
      firestoreUpdate = {
        isActive: false,
        updatedAt: serverTimestamp(),
      };
      setReminders((prev) => {
        const updated = prev.map((r) => (r.id === id ? { ...r, isActive: false } : r));
        saveToLocal('netcar_reminders', updated);
        return updated;
      });
    }

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/reminders/${id}`;
      setSyncStatus('syncing');
      try {
        await updateDoc(doc(db, 'users', currentUid, 'reminders', id), firestoreUpdate);
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.UPDATE, path);
      }
    }
  };

  const deleteReminder = async (id: string) => {
    setReminders((prev) => {
      const updated = prev.filter((r) => r.id !== id);
      saveToLocal('netcar_reminders', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/reminders/${id}`;
      setSyncStatus('syncing');
      try {
        await deleteDoc(doc(db, 'users', currentUid, 'reminders', id));
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.DELETE, path);
      }
    }
  };

  const updatePreferences = (prefs: Partial<UserProfile>) => {
    setUserProfile((prev) => {
      const updated = { ...prev, ...prefs };
      saveToLocal('netcar_profile', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const userDocPath = `users/${currentUid}`;
      setSyncStatus('syncing');
      const cleanPrefs = stripUndefined({
        name: prefs.name !== undefined ? prefs.name.slice(0, 200) : undefined,
        email: prefs.email !== undefined ? prefs.email.slice(0, 320) : undefined,
        preferredLang: prefs.preferredLang,
        units: prefs.units,
        currency: prefs.currency,
        activeVehicleId: prefs.activeVehicleId,
        updatedAt: serverTimestamp(),
      });
      updateDoc(doc(db, 'users', currentUid), cleanPrefs)
        .then(() => {
          setSyncStatus('synced');
          setLastSyncedAt(new Date().toISOString());
        })
        .catch((error) => {
          setSyncStatus('error');
          handleFirestoreError(error, OperationType.UPDATE, userDocPath);
        });
    }
  };

  const syncToCloudNow = async (): Promise<{
    success: boolean;
    error?: string;
    count?: number;
  }> => {
    const fbUser = auth.currentUser;
    if (!fbUser) {
      setAuthModalOpen(true);
      return {
        success: false,
        error:
          lang === 'fr'
            ? 'Veuillez vous connecter avec votre compte Google ou Email pour synchroniser avec le Cloud.'
            : 'Please sign in with your Google or Email account to synchronize with the Cloud.',
      };
    }

    const uid = fbUser.uid;
    setSyncStatus('syncing');
    try {
      const userDocRef = doc(db, 'users', uid);
      const validVehicles = vehicles.length > 0 ? vehicles : initialVehicles;
      const defaultVehId = validVehicles[0]?.id || 'veh-1';
      const validVehicleIds = new Set(validVehicles.map((v) => v.id));

      const baseProfile = stripUndefined({
        userId: uid,
        name: (userProfile.name || fbUser.displayName || 'Pilot').trim().slice(0, 200) || 'Pilot',
        email:
          (fbUser.email || userProfile.email || 'driver@netcar.app').trim().slice(0, 320) ||
          'driver@netcar.app',
        preferredLang: lang === 'en' || lang === 'fr' ? lang : 'fr',
        units: userProfile.units === 'imperial' ? 'imperial' : 'metric',
        currency: ['DZD', 'EUR', 'USD', 'CAD', 'GBP', 'MAD', 'CHF'].includes(userProfile.currency)
          ? userProfile.currency
          : 'DZD',
        activeVehicleId: validVehicleIds.has(activeVehicleId) ? activeVehicleId : defaultVehId,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });

      // Step 1: Write user profile & all parent vehicles in an initial atomic batch
      // so parentUserExists and parentVehicleExists are 100% satisfied for child records
      const parentBatch = writeBatch(db);
      parentBatch.set(userDocRef, baseProfile);

      for (const v of validVehicles) {
        parentBatch.set(
          doc(db, 'users', uid, 'vehicles', v.id),
          stripUndefined({
            id: v.id,
            userId: uid,
            name: v.name ? v.name.slice(0, 100) : undefined,
            make: (v.make || 'Vehicle').slice(0, 100),
            model: (v.model || 'Model').slice(0, 100),
            year: Math.min(2100, Math.max(1900, Number(v.year) || 2021)),
            plate: v.plate ? v.plate.slice(0, 30) : undefined,
            vin: v.vin ? v.vin.slice(0, 30) : undefined,
            fuelType: ['gasoline', 'diesel', 'lpg', 'electric', 'hybrid', 'ethanol', 'cng'].includes(
              v.fuelType
            )
              ? v.fuelType
              : 'gasoline',
            photoUrl: v.photoUrl ? v.photoUrl.slice(0, 2048) : undefined,
            currentOdometer: Math.max(0, Number(v.currentOdometer) || 0),
            notes: v.notes ? v.notes.slice(0, 5000) : undefined,
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          })
        );
      }

      await parentBatch.commit();
      isCloudReadyRef.current = true;

      // Step 2: Prepare all child operations and commit in chunks of 400
      const childOps: Array<{ ref: ReturnType<typeof doc>; data: Record<string, any> }> = [];

      for (const f of fillUps) {
        const targetVehId = validVehicleIds.has(f.vehicleId) ? f.vehicleId : defaultVehId;
        const vol = Number(f.volume);
        if (!Number.isFinite(vol) || vol <= 0) continue;
        childOps.push({
          ref: doc(db, 'users', uid, 'fillups', f.id),
          data: stripUndefined({
            id: f.id,
            userId: uid,
            vehicleId: targetVehId,
            date: String(f.date || new Date().toISOString()).slice(0, 64),
            odometer: Math.max(0, Number(f.odometer) || 0),
            volume: vol,
            pricePerUnit: Math.max(0, Number(f.pricePerUnit) || 0),
            totalCost: Math.max(0, Number(f.totalCost) || 0),
            fuelType: String(f.fuelType || 'gasoline').slice(0, 30),
            isFullTank: Boolean(f.isFullTank),
            station: f.station ? f.station.slice(0, 200) : undefined,
            notes: f.notes ? f.notes.slice(0, 5000) : undefined,
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          }),
        });
      }

      for (const e of expenses) {
        const targetVehId = validVehicleIds.has(e.vehicleId) ? e.vehicleId : defaultVehId;
        const amt = Number(e.amount);
        if (!Number.isFinite(amt) || amt <= 0) continue;
        childOps.push({
          ref: doc(db, 'users', uid, 'expenses', e.id),
          data: stripUndefined({
            id: e.id,
            userId: uid,
            vehicleId: targetVehId,
            date: String(e.date || new Date().toISOString()).slice(0, 64),
            category: String(e.category || 'other').slice(0, 100),
            amount: amt,
            description: (e.description || 'Expense').slice(0, 500),
            receiptUrl: e.receiptUrl ? e.receiptUrl.slice(0, 2048) : undefined,
            odometer:
              e.odometer !== undefined && Number(e.odometer) >= 0 ? Number(e.odometer) : undefined,
            notes: e.notes ? e.notes.slice(0, 5000) : undefined,
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          }),
        });
      }

      for (const s of services) {
        const targetVehId = validVehicleIds.has(s.vehicleId) ? s.vehicleId : defaultVehId;
        childOps.push({
          ref: doc(db, 'users', uid, 'services', s.id),
          data: stripUndefined({
            id: s.id,
            userId: uid,
            vehicleId: targetVehId,
            date: String(s.date || new Date().toISOString()).slice(0, 64),
            type: String(s.type || 'other').slice(0, 100),
            odometer: Math.max(0, Number(s.odometer) || 0),
            cost: Math.max(0, Number(s.cost) || 0),
            description: (s.description || 'Service').slice(0, 500),
            parts: s.parts && s.parts.length > 0 ? s.parts.slice(0, 50) : undefined,
            receiptUrl: s.receiptUrl ? s.receiptUrl.slice(0, 2048) : undefined,
            notes: s.notes ? s.notes.slice(0, 5000) : undefined,
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          }),
        });
      }

      for (const r of reminders) {
        const targetVehId = validVehicleIds.has(r.vehicleId) ? r.vehicleId : defaultVehId;
        childOps.push({
          ref: doc(db, 'users', uid, 'reminders', r.id),
          data: stripUndefined({
            id: r.id,
            userId: uid,
            vehicleId: targetVehId,
            type: String(r.type || 'other').slice(0, 100),
            title: r.title ? r.title.slice(0, 200) : undefined,
            intervalKm:
              r.intervalKm !== undefined && Number(r.intervalKm) >= 0
                ? Number(r.intervalKm)
                : undefined,
            intervalDays:
              r.intervalDays !== undefined && Number(r.intervalDays) >= 0
                ? Number(r.intervalDays)
                : undefined,
            lastServiceDate: r.lastServiceDate ? String(r.lastServiceDate).slice(0, 64) : undefined,
            lastServiceOdometer:
              r.lastServiceOdometer !== undefined && Number(r.lastServiceOdometer) >= 0
                ? Number(r.lastServiceOdometer)
                : undefined,
            nextDueDate: r.nextDueDate ? String(r.nextDueDate).slice(0, 64) : undefined,
            nextDueOdometer:
              r.nextDueOdometer !== undefined && Number(r.nextDueOdometer) >= 0
                ? Number(r.nextDueOdometer)
                : undefined,
            isRecurring: Boolean(r.isRecurring),
            isActive: Boolean(r.isActive),
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          }),
        });
      }

      const CHUNK_SIZE = 400;
      for (let i = 0; i < childOps.length; i += CHUNK_SIZE) {
        const batch = writeBatch(db);
        const chunk = childOps.slice(i, i + CHUNK_SIZE);
        for (const item of chunk) {
          batch.set(item.ref, item.data);
        }
        await batch.commit();
      }

      const nowIso = new Date().toISOString();
      setSyncStatus('synced');
      setLastSyncedAt(nowIso);
      return {
        success: true,
        count: validVehicles.length + childOps.length,
      };
    } catch (error) {
      setSyncStatus('error');
      reportFirestoreErrorSafely(error, OperationType.WRITE, `users/${uid}`);
      return {
        success: false,
        error:
          lang === 'fr'
            ? 'Échec de synchronisation Cloud. Vérifiez votre connexion Internet et réessayez.'
            : 'Cloud synchronization failed. Please check your internet connection and try again.',
      };
    }
  };

  const exportBackupJson = () => {
    const backup = {
      exportedAt: new Date().toISOString(),
      version: '1.0.0',
      userProfile,
      vehicles,
      fillUps,
      expenses,
      services,
      reminders,
    };
    return JSON.stringify(backup, null, 2);
  };

  const importBackupJson = (jsonData: string): boolean => {
    try {
      const data = JSON.parse(jsonData);
      if (data.vehicles && Array.isArray(data.vehicles)) {
        setVehicles(data.vehicles);
        saveToLocal('netcar_vehicles', data.vehicles);
      }
      if (data.fillUps && Array.isArray(data.fillUps)) {
        setFillUps(data.fillUps);
        saveToLocal('netcar_fillups', data.fillUps);
      }
      if (data.expenses && Array.isArray(data.expenses)) {
        setExpenses(data.expenses);
        saveToLocal('netcar_expenses', data.expenses);
      }
      if (data.services && Array.isArray(data.services)) {
        setServices(data.services);
        saveToLocal('netcar_services', data.services);
      }
      if (data.reminders && Array.isArray(data.reminders)) {
        setReminders(data.reminders);
        saveToLocal('netcar_reminders', data.reminders);
      }
      return true;
    } catch (e) {
      console.error('Failed to import backup:', e);
      return false;
    }
  };

  const exportFuelioCsvForVehicle = (targetVehicleId?: string): string => {
    const veh =
      vehicles.find((v) => v.id === (targetVehicleId || activeVehicle?.id)) ||
      activeVehicle ||
      vehicles[0];
    if (!veh) return '';
    const vehFillUps = fillUps.filter((f) => f.vehicleId === veh.id);
    const vehExpenses = expenses.filter((e) => e.vehicleId === veh.id);
    return exportFuelioCsv(veh, vehFillUps, vehExpenses, userProfile.units);
  };

  const importFuelioCsv = async (
    csvText: string,
    targetVehicleId?: string
  ): Promise<{
    success: boolean;
    importedFillUps: number;
    importedExpenses: number;
    error?: string;
  }> => {
    const targetVeh =
      vehicles.find((v) => v.id === (targetVehicleId || activeVehicle?.id)) ||
      activeVehicle ||
      vehicles[0];

    if (!targetVeh) {
      return {
        success: false,
        importedFillUps: 0,
        importedExpenses: 0,
        error:
          lang === 'fr'
            ? 'Veuillez d’abord sélectionner ou ajouter un véhicule.'
            : 'Please select or add a vehicle first.',
      };
    }

    try {
      const parsed = parseFuelioCsv(csvText, targetVeh.id, targetVeh.fuelType);
      if (parsed.fillUps.length === 0 && parsed.expenses.length === 0) {
        return {
          success: false,
          importedFillUps: 0,
          importedExpenses: 0,
          error:
            lang === 'fr'
              ? 'Aucune entrée carburant valide trouvée dans ce fichier CSV Fuelio.'
              : 'No valid fuel entries found in this Fuelio CSV file.',
        };
      }

      const nowMs = Date.now();
      const nowIso = new Date().toISOString();

      // Deduplicate against existing fill-ups for this vehicle by odometer + volume
      const existingKeys = new Set(
        fillUps
          .filter((f) => f.vehicleId === targetVeh.id)
          .map((f) => `${f.odometer}_${Number(f.volume).toFixed(2)}`)
      );

      const newFillUpItems: FillUp[] = [];
      parsed.fillUps.forEach((f, idx) => {
        const key = `${f.odometer}_${Number(f.volume).toFixed(2)}`;
        if (!existingKeys.has(key)) {
          existingKeys.add(key);
          newFillUpItems.push({
            ...f,
            id: `f-csv-${nowMs}-${idx}`,
            createdAt: nowIso,
          });
        }
      });

      const newExpenseItems: Expense[] = parsed.expenses.map((e, idx) => ({
        ...e,
        id: `e-csv-${nowMs}-${idx}`,
        createdAt: nowIso,
      }));

      if (newFillUpItems.length > 0) {
        setFillUps((prev) => {
          const updated = [...newFillUpItems, ...prev];
          saveToLocal('netcar_fillups', updated);
          return updated;
        });
      }

      if (newExpenseItems.length > 0) {
        setExpenses((prev) => {
          const updated = [...newExpenseItems, ...prev];
          saveToLocal('netcar_expenses', updated);
          return updated;
        });
      }

      if (parsed.maxOdometer > targetVeh.currentOdometer) {
        await updateVehicle(targetVeh.id, { currentOdometer: parsed.maxOdometer });
      }

      const currentUid = auth.currentUser?.uid;
      if (currentUid && isCloudReadyRef.current) {
        setSyncStatus('syncing');
        try {
          const ops: Array<{ ref: ReturnType<typeof doc>; data: Record<string, any> }> = [];
          for (const f of newFillUpItems) {
            ops.push({
              ref: doc(db, 'users', currentUid, 'fillups', f.id),
              data: stripUndefined({
                id: f.id,
                userId: currentUid,
                vehicleId: f.vehicleId,
                date: String(f.date).slice(0, 64),
                odometer: Math.max(0, Number(f.odometer) || 0),
                volume: Number(f.volume),
                pricePerUnit: Math.max(0, Number(f.pricePerUnit) || 0),
                totalCost: Math.max(0, Number(f.totalCost) || 0),
                fuelType: String(f.fuelType || 'gasoline').slice(0, 30),
                isFullTank: Boolean(f.isFullTank),
                station: f.station?.slice(0, 200),
                notes: f.notes?.slice(0, 5000),
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              }),
            });
          }
          for (const e of newExpenseItems) {
            ops.push({
              ref: doc(db, 'users', currentUid, 'expenses', e.id),
              data: stripUndefined({
                id: e.id,
                userId: currentUid,
                vehicleId: e.vehicleId,
                date: String(e.date).slice(0, 64),
                category: String(e.category || 'other').slice(0, 100),
                amount: Number(e.amount),
                description: (e.description || 'Expense').slice(0, 500),
                odometer: e.odometer !== undefined ? Number(e.odometer) : undefined,
                notes: e.notes?.slice(0, 5000),
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              }),
            });
          }

          const CHUNK_SIZE = 400;
          for (let i = 0; i < ops.length; i += CHUNK_SIZE) {
            const batch = writeBatch(db);
            const chunk = ops.slice(i, i + CHUNK_SIZE);
            for (const item of chunk) {
              batch.set(item.ref, item.data);
            }
            await batch.commit();
          }
          setSyncStatus('synced');
          setLastSyncedAt(new Date().toISOString());
        } catch (cloudErr) {
          setSyncStatus('error');
          reportFirestoreErrorSafely(cloudErr, OperationType.WRITE, `users/${currentUid}/fillups`);
        }
      }

      return {
        success: true,
        importedFillUps: newFillUpItems.length,
        importedExpenses: newExpenseItems.length,
      };
    } catch (err) {
      return {
        success: false,
        importedFillUps: 0,
        importedExpenses: 0,
        error:
          err instanceof Error
            ? err.message
            : lang === 'fr'
              ? 'Erreur lors de la lecture du fichier CSV Fuelio.'
              : 'Failed to parse Fuelio CSV file.',
      };
    }
  };

  // Firebase Auth Implementations (Email/Password + Google OAuth)
  const formatAuthError = (
    err: any,
    currentLang: SupportedLanguage
  ): { error: string; consoleUrl?: string } => {
    const code = (err?.code || '').toLowerCase();
    const projectConsoleUrl =
      'https://console.firebase.google.com/project/axiomatic-serenity-8lvvl/authentication/providers';

    if (code.includes('operation-not-allowed') || code.includes('configuration-not-found')) {
      return {
        error:
          currentLang === 'fr'
            ? "Le fournisseur Email/Mot de passe n'est pas encore activé dans votre console Firebase. Activez-le dans Firebase Console > Authentication > Sign-in method, ou connectez-vous directement avec Google."
            : 'Email/Password sign-in is not enabled yet in your Firebase project. Please enable it in Firebase Console > Authentication > Sign-in method, or sign in with Google.',
        consoleUrl: projectConsoleUrl,
      };
    }
    if (
      code.includes('invalid-credential') ||
      code.includes('wrong-password') ||
      code.includes('user-not-found')
    ) {
      return {
        error:
          currentLang === 'fr'
            ? 'Email ou mot de passe incorrect. Si vous êtes nouveau, cliquez sur Créer un compte.'
            : 'Invalid email or password. If you do not have an account yet, switch to Create Account.',
      };
    }
    if (code.includes('email-already-in-use')) {
      return {
        error:
          currentLang === 'fr'
            ? 'Cette adresse e-mail est déjà associée à un compte. Veuillez vous connecter.'
            : 'This email address is already registered. Please sign in instead.',
      };
    }
    if (code.includes('popup-closed-by-user')) {
      return {
        error:
          currentLang === 'fr'
            ? 'La fenêtre de connexion Google a été fermée avant la fin.'
            : 'Google Sign-In popup was closed before completing.',
      };
    }
    if (code.includes('popup-blocked')) {
      return {
        error:
          currentLang === 'fr'
            ? 'La fenêtre popup a été bloquée par votre navigateur. Veuillez autoriser les popups.'
            : 'Sign-in popup was blocked by your browser. Please allow popups for this site.',
      };
    }
    return {
      error:
        err?.message ||
        (currentLang === 'fr' ? "Erreur d'authentification." : 'Authentication error.'),
    };
  };

  const signInWithEmail = async (
    email: string,
    pass: string
  ): Promise<{ success: boolean; error?: string; consoleUrl?: string }> => {
    try {
      await signInWithEmailAndPassword(auth, email, pass);
      return { success: true };
    } catch (err: any) {
      const formatted = formatAuthError(err, lang);
      return { success: false, ...formatted };
    }
  };

  const signUpWithEmail = async (
    email: string,
    pass: string,
    name?: string
  ): Promise<{ success: boolean; error?: string; consoleUrl?: string }> => {
    try {
      const cred = await createUserWithEmailAndPassword(auth, email, pass);
      if (name && cred.user) {
        try {
          await updateProfile(cred.user, { displayName: name });
        } catch {
          // Ignore display name update errors
        }
      }
      return { success: true };
    } catch (err: any) {
      const formatted = formatAuthError(err, lang);
      return { success: false, ...formatted };
    }
  };

  const signInWithGoogle = async (): Promise<{ success: boolean; error?: string }> => {
    try {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: 'select_account' });
      await signInWithPopup(auth, provider);
      return { success: true };
    } catch (err: any) {
      const formatted = formatAuthError(err, lang);
      return { success: false, error: formatted.error };
    }
  };

  const signOut = async () => {
    try {
      await fbSignOut(auth);
    } catch {
      // Ignore signout errors
    }
    setUser(null);
    setSyncStatus('local');
    if (typeof window !== 'undefined') {
      localStorage.removeItem('netcar_demo_user');
    }
  };

  const signInDemo = () => {
    const u: AppUser = {
      uid: 'usr-karim',
      email: 'karim.mansouri@netcar.app',
      displayName: 'Karim Mansouri',
      isDemo: true,
    };
    setUser(u);
    setSyncStatus('local');
    saveToLocal('netcar_demo_user', u);
    setUserProfile((prev) => ({
      ...prev,
      email: u.email,
      name: u.displayName || prev.name,
    }));
  };

  const t = translations[lang];

  const contextValue = useMemo<AppContextType>(
    () => ({
      user,
      userProfile,
      lang,
      setLang,
      t,
      vehicles,
      activeVehicle,
      activeVehicleId,
      setActiveVehicleId,
      fillUps,
      expenses,
      services,
      reminders,
      loading,
      syncStatus,
      lastSyncedAt,
      authModalOpen,
      setAuthModalOpen,
      addVehicleModalOpen,
      setAddVehicleModalOpen,
      signInWithEmail,
      signUpWithEmail,
      signInWithGoogle,
      signOut,
      signInDemo,
      addVehicle,
      updateVehicle,
      deleteVehicle,
      addFillUp,
      deleteFillUp,
      addExpense,
      deleteExpense,
      addService,
      deleteService,
      addReminder,
      completeReminder,
      deleteReminder,
      updatePreferences,
      syncToCloudNow,
      exportBackupJson,
      importBackupJson,
      exportFuelioCsvForVehicle,
      importFuelioCsv,
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [
      user,
      userProfile,
      lang,
      t,
      vehicles,
      activeVehicle,
      activeVehicleId,
      fillUps,
      expenses,
      services,
      reminders,
      loading,
      syncStatus,
      lastSyncedAt,
      authModalOpen,
      addVehicleModalOpen,
    ]
  );

  return <AppContext.Provider value={contextValue}>{children}</AppContext.Provider>;
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
}
