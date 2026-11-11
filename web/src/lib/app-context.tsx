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
  deleteField,
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

const MAX_CUSTOM_CATEGORIES = 100;
const MAX_CUSTOM_CATEGORY_LENGTH = 60;

function sanitizeCustomCategory(name: string): string {
  return String(name || '')
    .trim()
    .replace(/\s+/g, ' ')
    .slice(0, MAX_CUSTOM_CATEGORY_LENGTH);
}

function mergeUniqueStrings(...lists: Array<unknown>): string[] {
  const merged: string[] = [];
  const seen = new Set<string>();
  for (const list of lists) {
    if (!Array.isArray(list)) continue;
    for (const raw of list) {
      if (typeof raw !== 'string') continue;
      const clean = sanitizeCustomCategory(raw);
      if (!clean) continue;
      const key = clean.toLowerCase();
      if (seen.has(key)) continue;
      seen.add(key);
      merged.push(clean);
      if (merged.length >= MAX_CUSTOM_CATEGORIES) return merged;
    }
  }
  return merged;
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
  updateReminder: (id: string, updates: Partial<Reminder>) => Promise<void>;
  completeReminder: (id: string) => Promise<void>;
  deleteReminder: (id: string) => Promise<void>;

  // Custom categories (stored locally and best-effort synced to the profile)
  addCustomExpenseCategory: (name: string) => void;
  addCustomServiceType: (name: string) => void;

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
  customExpenseCategories: [],
  customServiceTypes: [],
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

function toTimestampMs(value: any): number {
  if (!value) return 0;
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0;
  if (typeof value === 'string') {
    const parsed = Date.parse(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  if (typeof value?.toMillis === 'function') {
    const ms = value.toMillis();
    return Number.isFinite(ms) ? ms : 0;
  }
  if (typeof value?.toDate === 'function') {
    const date = value.toDate();
    return date instanceof Date && Number.isFinite(date.getTime()) ? date.getTime() : 0;
  }
  if (typeof value?.seconds === 'number') {
    return value.seconds * 1000 + (Number(value.nanoseconds) || 0) / 1_000_000;
  }
  return 0;
}

function readStringArray(key: string): string[] | null {
  if (typeof window === 'undefined') return null;
  const raw = localStorage.getItem(key);
  if (raw === null) return null;
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
  } catch {
    return [];
  }
}

function writeStringArray(key: string, values: Iterable<string>) {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(key, JSON.stringify(Array.from(new Set(values))));
  } catch {
    // Ignore storage quota errors
  }
}

function readSyncedIds(collectionName: string): Set<string> | null {
  const values = readStringArray(`netcar_synced_ids_${collectionName}`);
  return values === null ? null : new Set(values);
}

function writeSyncedIds(collectionName: string, ids: Iterable<string>) {
  writeStringArray(`netcar_synced_ids_${collectionName}`, ids);
}

function readPendingDeletes(collectionName: string): Set<string> {
  return new Set(readStringArray(`netcar_pending_deletes_${collectionName}`) || []);
}

function writePendingDeletes(collectionName: string, ids: Set<string>) {
  writeStringArray(`netcar_pending_deletes_${collectionName}`, ids);
}

function markPendingDelete(collectionName: string, id: string) {
  const pending = readPendingDeletes(collectionName);
  pending.add(id);
  writePendingDeletes(collectionName, pending);
}

function unmarkPendingDelete(collectionName: string, id: string) {
  const pending = readPendingDeletes(collectionName);
  if (pending.delete(id)) {
    writePendingDeletes(collectionName, pending);
  }
}

function getLastSyncMs(): number {
  if (typeof window === 'undefined') return 0;
  const raw = localStorage.getItem('netcar_last_sync_ts');
  const parsed = raw ? Number(raw) : 0;
  return Number.isFinite(parsed) ? parsed : 0;
}

function setLastSyncMs(ms: number) {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem('netcar_last_sync_ts', String(ms));
  } catch {
    // Ignore storage quota errors
  }
}

function readLocalCollection<T>(key: string, fallback: T[]): T[] {
  if (typeof window === 'undefined') return fallback;
  const raw = localStorage.getItem(key);
  if (raw === null) return fallback;
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as T[]) : fallback;
  } catch {
    return fallback;
  }
}

interface RemoteEntry<T> {
  item: T;
  updatedAtMs: number;
}

interface MergeCollectionResult<T> {
  items: T[];
  ids: Set<string>;
  pushedCount: number;
  hadFailure: boolean;
}

async function processPendingDeletes(
  collectionName: string,
  collectionPath: string
): Promise<Set<string>> {
  const pending = readPendingDeletes(collectionName);
  if (pending.size === 0) return pending;

  for (const id of Array.from(pending)) {
    try {
      await deleteDoc(doc(db, collectionPath, id));
      pending.delete(id);
      const synced = readSyncedIds(collectionName);
      if (synced) {
        synced.delete(id);
        writeSyncedIds(collectionName, synced);
      }
    } catch {
      // Keep the deletion queued for the next sync attempt.
    }
  }
  writePendingDeletes(collectionName, pending);
  return pending;
}

async function mergeCollection<T>(
  collectionName: string,
  localItems: T[],
  remoteItems: Map<string, RemoteEntry<T>>,
  lastSyncMs: number,
  options: {
    getId: (item: T) => string;
    getUpdatedAtMs: (item: T) => number;
    withUpdatedAt: (item: T, iso: string) => T;
    remap: (item: T, id: string) => T;
    createRemote: (id: string, item: T) => Promise<void>;
    updateRemote: (id: string, item: T) => Promise<void>;
  }
): Promise<MergeCollectionResult<T>> {
  const previousSyncedIds = readSyncedIds(collectionName);
  const pendingDeletes = readPendingDeletes(collectionName);
  const merged = new Map<string, T>();

  // Remote documents win by default; pending local deletions are hidden until pushed.
  for (const [id, entry] of remoteItems) {
    if (!pendingDeletes.has(id)) {
      merged.set(id, options.remap(entry.item, id));
    }
  }

  let pushedCount = 0;
  let hadFailure = false;
  const nowIso = new Date().toISOString();

  for (const local of localItems) {
    const id = options.getId(local);
    if (!id || pendingDeletes.has(id)) continue;

    const localUpdatedMs = options.getUpdatedAtMs(local);
    const remote = remoteItems.get(id);

    if (remote) {
      // Only items changed since the last successful sync may override the cloud.
      const localChanged = localUpdatedMs > 0 && localUpdatedMs > lastSyncMs;
      const remoteChanged = remote.updatedAtMs > 0 && remote.updatedAtMs > lastSyncMs;
      let localWins = false;

      if (lastSyncMs > 0) {
        if (localChanged && !remoteChanged) {
          localWins = true;
        } else if (localChanged && remoteChanged) {
          localWins = localUpdatedMs >= remote.updatedAtMs;
        }
      } else if (localUpdatedMs > 0 && remote.updatedAtMs > 0) {
        // First sync after this device received the updated app: compare the
        // records directly. Stale records from older versions have no updatedAt
        // and therefore never override the cloud.
        localWins = localUpdatedMs > remote.updatedAtMs;
      } else if (localUpdatedMs > 0 && remote.updatedAtMs === 0) {
        localWins = true;
      }

      if (localWins) {
        try {
          await options.updateRemote(id, local);
          pushedCount++;
          merged.set(id, options.withUpdatedAt(options.remap(local, id), nowIso));
        } catch {
          hadFailure = true;
          merged.set(id, options.withUpdatedAt(options.remap(local, id), nowIso));
        }
      } else {
        merged.set(id, options.remap(remote.item, id));
      }
    } else {
      // Missing remotely: push new local records. If this ID was previously synced,
      // treat the absence as a remote deletion unless it changed locally since then.
      const shouldPush =
        previousSyncedIds === null
          ? true
          : !previousSyncedIds.has(id) || (localUpdatedMs > 0 && localUpdatedMs > lastSyncMs);

      if (shouldPush) {
        try {
          await options.createRemote(id, local);
          pushedCount++;
          merged.set(id, options.withUpdatedAt(options.remap(local, id), nowIso));
        } catch {
          hadFailure = true;
          merged.set(id, options.withUpdatedAt(options.remap(local, id), nowIso));
        }
      }
    }
  }

  const ids = new Set(merged.keys());
  writeSyncedIds(collectionName, ids);
  return { items: Array.from(merged.values()), ids, pushedCount, hadFailure };
}

function mapVehicleFromDoc(raw: any, docId: string): Vehicle {
  return {
    id: raw.id || docId,
    name: raw.name,
    make: raw.make || 'Vehicle',
    model: raw.model || 'Model',
    year: Number(raw.year) || 2021,
    plate: raw.plate,
    vin: raw.vin,
    fuelType: (raw.fuelType || 'gasoline') as Vehicle['fuelType'],
    photoUrl: raw.photoUrl,
    currentOdometer: Number(raw.currentOdometer) || 0,
    notes: raw.notes,
    createdAt: normalizeTimestampField(raw.createdAt),
    updatedAt: normalizeTimestampField(raw.updatedAt),
  };
}

function mapFillUpFromDoc(raw: any, docId: string): FillUp {
  return {
    id: raw.id || docId,
    vehicleId: raw.vehicleId,
    date: raw.date,
    odometer: Number(raw.odometer) || 0,
    volume: Number(raw.volume) || 0,
    pricePerUnit: Number(raw.pricePerUnit) || 0,
    totalCost: Number(raw.totalCost) || 0,
    fuelType: (raw.fuelType || 'gasoline') as FillUp['fuelType'],
    isFullTank: Boolean(raw.isFullTank),
    station: raw.station,
    notes: raw.notes,
    createdAt: normalizeTimestampField(raw.createdAt),
    updatedAt: normalizeTimestampField(raw.updatedAt),
  };
}

function mapExpenseFromDoc(raw: any, docId: string): Expense {
  return {
    id: raw.id || docId,
    vehicleId: raw.vehicleId,
    date: raw.date,
    category: (raw.category || 'other') as Expense['category'],
    amount: Number(raw.amount) || 0,
    description: raw.description || '',
    receiptUrl: raw.receiptUrl,
    odometer: raw.odometer !== undefined ? Number(raw.odometer) : undefined,
    notes: raw.notes,
    createdAt: normalizeTimestampField(raw.createdAt),
    updatedAt: normalizeTimestampField(raw.updatedAt),
  };
}

function mapServiceFromDoc(raw: any, docId: string): Service {
  return {
    id: raw.id || docId,
    vehicleId: raw.vehicleId,
    date: raw.date,
    type: (raw.type || 'other') as Service['type'],
    odometer: Number(raw.odometer) || 0,
    cost: Number(raw.cost) || 0,
    description: raw.description || '',
    parts: Array.isArray(raw.parts) ? raw.parts : undefined,
    receiptUrl: raw.receiptUrl,
    notes: raw.notes,
    createdAt: normalizeTimestampField(raw.createdAt),
    updatedAt: normalizeTimestampField(raw.updatedAt),
  };
}

function mapReminderFromDoc(raw: any, docId: string): Reminder {
  return {
    id: raw.id || docId,
    vehicleId: raw.vehicleId,
    type: raw.type || 'other',
    title: raw.title,
    intervalKm: raw.intervalKm !== undefined ? Number(raw.intervalKm) : undefined,
    intervalDays: raw.intervalDays !== undefined ? Number(raw.intervalDays) : undefined,
    lastServiceDate: raw.lastServiceDate,
    lastServiceOdometer:
      raw.lastServiceOdometer !== undefined ? Number(raw.lastServiceOdometer) : undefined,
    nextDueDate: raw.nextDueDate,
    nextDueOdometer:
      raw.nextDueOdometer !== undefined ? Number(raw.nextDueOdometer) : undefined,
    isRecurring: Boolean(raw.isRecurring),
    isActive: Boolean(raw.isActive),
    createdAt: normalizeTimestampField(raw.createdAt),
    updatedAt: normalizeTimestampField(raw.updatedAt),
  };
}

const VALID_FUEL_TYPES = [
  'gasoline',
  'diesel',
  'lpg',
  'electric',
  'hybrid',
  'ethanol',
  'cng',
];
const VALID_CURRENCIES = ['DZD', 'EUR', 'USD', 'CAD', 'GBP', 'MAD', 'CHF'];

function buildVehicleCreateData(uid: string, id: string, v: Vehicle): Record<string, any> {
  return stripUndefined({
    id,
    userId: uid,
    name: v.name ? v.name.slice(0, 100) : undefined,
    make: (v.make || 'Vehicle').slice(0, 100),
    model: (v.model || 'Model').slice(0, 100),
    year: Math.min(2100, Math.max(1900, Number(v.year) || 2021)),
    plate: v.plate ? v.plate.slice(0, 30) : undefined,
    vin: v.vin ? v.vin.slice(0, 30) : undefined,
    fuelType: VALID_FUEL_TYPES.includes(v.fuelType) ? v.fuelType : 'gasoline',
    photoUrl: v.photoUrl ? v.photoUrl.slice(0, 2048) : undefined,
    currentOdometer: Math.max(0, Number(v.currentOdometer) || 0),
    notes: v.notes ? v.notes.slice(0, 5000) : undefined,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
}

function buildVehicleUpdateData(v: Vehicle): Record<string, any> {
  return {
    make: (v.make || 'Vehicle').slice(0, 100),
    model: (v.model || 'Model').slice(0, 100),
    year: Math.min(2100, Math.max(1900, Number(v.year) || 2021)),
    fuelType: VALID_FUEL_TYPES.includes(v.fuelType) ? v.fuelType : 'gasoline',
    currentOdometer: Math.max(0, Number(v.currentOdometer) || 0),
    name: v.name ? v.name.slice(0, 100) : deleteField(),
    plate: v.plate ? v.plate.slice(0, 30) : deleteField(),
    vin: v.vin ? v.vin.slice(0, 30) : deleteField(),
    photoUrl: v.photoUrl ? v.photoUrl.slice(0, 2048) : deleteField(),
    notes: v.notes ? v.notes.slice(0, 5000) : deleteField(),
    updatedAt: serverTimestamp(),
  };
}

function buildFillUpCreateData(
  uid: string,
  id: string,
  f: FillUp,
  vehicleId: string
): Record<string, any> {
  return stripUndefined({
    id,
    userId: uid,
    vehicleId,
    date: String(f.date || new Date().toISOString()).slice(0, 64),
    odometer: Math.max(0, Number(f.odometer) || 0),
    volume: Math.max(0, Number(f.volume) || 0),
    pricePerUnit: Math.max(0, Number(f.pricePerUnit) || 0),
    totalCost: Math.max(0, Number(f.totalCost) || 0),
    fuelType: String(f.fuelType || 'gasoline').slice(0, 30),
    isFullTank: Boolean(f.isFullTank),
    station: f.station ? f.station.slice(0, 200) : undefined,
    notes: f.notes ? f.notes.slice(0, 5000) : undefined,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
}

function buildFillUpUpdateData(f: FillUp, vehicleId: string): Record<string, any> {
  return {
    vehicleId,
    date: String(f.date || new Date().toISOString()).slice(0, 64),
    odometer: Math.max(0, Number(f.odometer) || 0),
    volume: Math.max(0, Number(f.volume) || 0),
    pricePerUnit: Math.max(0, Number(f.pricePerUnit) || 0),
    totalCost: Math.max(0, Number(f.totalCost) || 0),
    fuelType: String(f.fuelType || 'gasoline').slice(0, 30),
    isFullTank: Boolean(f.isFullTank),
    station: f.station ? f.station.slice(0, 200) : deleteField(),
    notes: f.notes ? f.notes.slice(0, 5000) : deleteField(),
    updatedAt: serverTimestamp(),
  };
}

function buildExpenseCreateData(
  uid: string,
  id: string,
  e: Expense,
  vehicleId: string
): Record<string, any> {
  return stripUndefined({
    id,
    userId: uid,
    vehicleId,
    date: String(e.date || new Date().toISOString()).slice(0, 64),
    category: String(e.category || 'other').slice(0, 100),
    amount: Math.max(0, Number(e.amount) || 0),
    description: (e.description || 'Expense').slice(0, 500),
    receiptUrl: e.receiptUrl ? e.receiptUrl.slice(0, 2048) : undefined,
    odometer:
      e.odometer !== undefined && Number(e.odometer) >= 0 ? Number(e.odometer) : undefined,
    notes: e.notes ? e.notes.slice(0, 5000) : undefined,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
}

function buildExpenseUpdateData(e: Expense, vehicleId: string): Record<string, any> {
  return {
    vehicleId,
    date: String(e.date || new Date().toISOString()).slice(0, 64),
    category: String(e.category || 'other').slice(0, 100),
    amount: Math.max(0, Number(e.amount) || 0),
    description: (e.description || 'Expense').slice(0, 500),
    receiptUrl: e.receiptUrl ? e.receiptUrl.slice(0, 2048) : deleteField(),
    odometer:
      e.odometer !== undefined && Number(e.odometer) >= 0
        ? Number(e.odometer)
        : deleteField(),
    notes: e.notes ? e.notes.slice(0, 5000) : deleteField(),
    updatedAt: serverTimestamp(),
  };
}

function buildServiceCreateData(
  uid: string,
  id: string,
  s: Service,
  vehicleId: string
): Record<string, any> {
  return stripUndefined({
    id,
    userId: uid,
    vehicleId,
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
  });
}

function buildServiceUpdateData(s: Service, vehicleId: string): Record<string, any> {
  return {
    vehicleId,
    date: String(s.date || new Date().toISOString()).slice(0, 64),
    type: String(s.type || 'other').slice(0, 100),
    odometer: Math.max(0, Number(s.odometer) || 0),
    cost: Math.max(0, Number(s.cost) || 0),
    description: (s.description || 'Service').slice(0, 500),
    parts:
      s.parts && s.parts.length > 0
        ? s.parts.slice(0, 50).map((p) => String(p).slice(0, 200))
        : deleteField(),
    receiptUrl: s.receiptUrl ? s.receiptUrl.slice(0, 2048) : deleteField(),
    notes: s.notes ? s.notes.slice(0, 5000) : deleteField(),
    updatedAt: serverTimestamp(),
  };
}

function buildReminderCreateData(
  uid: string,
  id: string,
  r: Reminder,
  vehicleId: string
): Record<string, any> {
  return stripUndefined({
    id,
    userId: uid,
    vehicleId,
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
  });
}

function buildReminderUpdateData(r: Reminder, vehicleId: string): Record<string, any> {
  return {
    vehicleId,
    type: String(r.type || 'other').slice(0, 100),
    title: r.title ? r.title.slice(0, 200) : deleteField(),
    intervalKm:
      r.intervalKm !== undefined && Number(r.intervalKm) >= 0
        ? Number(r.intervalKm)
        : deleteField(),
    intervalDays:
      r.intervalDays !== undefined && Number(r.intervalDays) >= 0
        ? Number(r.intervalDays)
        : deleteField(),
    lastServiceDate: r.lastServiceDate ? String(r.lastServiceDate).slice(0, 64) : deleteField(),
    lastServiceOdometer:
      r.lastServiceOdometer !== undefined && Number(r.lastServiceOdometer) >= 0
        ? Number(r.lastServiceOdometer)
        : deleteField(),
    nextDueDate: r.nextDueDate ? String(r.nextDueDate).slice(0, 64) : deleteField(),
    nextDueOdometer:
      r.nextDueOdometer !== undefined && Number(r.nextDueOdometer) >= 0
        ? Number(r.nextDueOdometer)
        : deleteField(),
    isRecurring: Boolean(r.isRecurring),
    isActive: Boolean(r.isActive),
    updatedAt: serverTimestamp(),
  };
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
        const currentSavedActiveId =
          localStorage.getItem('netcar_active_vehicle_id') || activeVehicleId || 'veh-1';

        // Read local data directly from storage: this effect runs once on mount and
        // React state may not have been hydrated from localStorage yet.
        const localVehicles = readLocalCollection<Vehicle>('netcar_vehicles', initialVehicles);
        const localFillUps = readLocalCollection<FillUp>('netcar_fillups', initialFillUps);
        const localExpenses = readLocalCollection<Expense>('netcar_expenses', initialExpenses);
        const localServices = readLocalCollection<Service>('netcar_services', initialServices);
        const localReminders = readLocalCollection<Reminder>('netcar_reminders', initialReminders);

        const validVehicles = localVehicles;
        const defaultVehId = validVehicles[0]?.id || 'veh-1';
        const validVehicleIds = new Set(validVehicles.map((v) => v.id));

        const baseUserDoc = stripUndefined({
          userId: uid,
          name: (displayName || 'Pilot').trim().slice(0, 200) || 'Pilot',
          email: (email || 'driver@netcar.app').trim().slice(0, 320) || 'driver@netcar.app',
          preferredLang: lang === 'en' || lang === 'fr' ? lang : 'fr',
          units: userProfile.units === 'imperial' ? 'imperial' : 'metric',
          currency: ['DZD', 'EUR', 'USD', 'CAD', 'GBP', 'MAD', 'CHF'].includes(userProfile.currency)
            ? userProfile.currency
            : 'DZD',
          activeVehicleId: validVehicleIds.has(currentSavedActiveId)
            ? currentSavedActiveId
            : defaultVehId,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
        const baseUserDocWithCategories = {
          ...baseUserDoc,
          customExpenseCategories: mergeUniqueStrings(userProfile.customExpenseCategories),
          customServiceTypes: mergeUniqueStrings(userProfile.customServiceTypes),
        };

        const userSnap = await getDoc(userDocRef);
        if (!userSnap.exists()) {
          try {
            await setDoc(userDocRef, baseUserDocWithCategories);
          } catch {
            // Older Firestore rules may reject the custom category fields.
            // Create the core profile anyway; categories stay local for now.
            await setDoc(userDocRef, baseUserDoc);
          }
        }
        isCloudReadyRef.current = true;

        // Only import local data when the cloud account is empty. When cloud data
        // already exists, the real-time listeners below become the source of truth
        // and syncToCloudNow merges changes per record.
        const vehiclesColRef = collection(db, 'users', uid, 'vehicles');
        const existingVehSnap = await getDocs(query(vehiclesColRef, where('userId', '==', uid)));
        const remoteVehIds = new Set(existingVehSnap.docs.map((d) => d.data().id || d.id));

        if (existingVehSnap.empty && validVehicles.length > 0) {
          const parentBatch = writeBatch(db);
          let parentOpsCount = 0;

          for (const v of validVehicles) {
            parentBatch.set(
              doc(db, 'users', uid, 'vehicles', v.id),
              buildVehicleCreateData(uid, v.id, v)
            );
            parentOpsCount++;
          }

          if (parentOpsCount > 0) {
            await parentBatch.commit();
          }
        }
        for (const rId of remoteVehIds) {
          validVehicleIds.add(rId);
        }

        if (existingVehSnap.empty && validVehicles.length > 0) {
          const childBatch = writeBatch(db);
          let childCount = 0;

          for (const f of localFillUps) {
            const targetVehId = validVehicleIds.has(f.vehicleId) ? f.vehicleId : defaultVehId;
            childBatch.set(
              doc(db, 'users', uid, 'fillups', f.id),
              stripUndefined({
                id: f.id,
                userId: uid,
                vehicleId: targetVehId,
                date: String(f.date || new Date().toISOString()).slice(0, 64),
                odometer: Math.max(0, Number(f.odometer) || 0),
                volume: Math.max(0, Number(f.volume) || 0),
                pricePerUnit: Math.max(0, Number(f.pricePerUnit) || 0),
                totalCost: Math.max(0, Number(f.totalCost) || 0),
                fuelType: String(f.fuelType || 'gasoline').slice(0, 30),
                isFullTank: Boolean(f.isFullTank),
                station: f.station ? f.station.slice(0, 200) : undefined,
                notes: f.notes ? f.notes.slice(0, 5000) : undefined,
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              })
            );
            childCount++;
          }

          for (const e of localExpenses) {
            const targetVehId = validVehicleIds.has(e.vehicleId) ? e.vehicleId : defaultVehId;
            childBatch.set(
              doc(db, 'users', uid, 'expenses', e.id),
              stripUndefined({
                id: e.id,
                userId: uid,
                vehicleId: targetVehId,
                date: String(e.date || new Date().toISOString()).slice(0, 64),
                category: String(e.category || 'other').slice(0, 100),
                amount: Math.max(0, Number(e.amount) || 0),
                description: (e.description || e.category || 'Expense').slice(0, 500),
                receiptUrl: e.receiptUrl ? e.receiptUrl.slice(0, 2048) : undefined,
                odometer:
                  e.odometer !== undefined && Number(e.odometer) >= 0
                    ? Number(e.odometer)
                    : undefined,
                notes: e.notes ? e.notes.slice(0, 5000) : undefined,
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              })
            );
            childCount++;
          }

          for (const s of localServices) {
            const targetVehId = validVehicleIds.has(s.vehicleId) ? s.vehicleId : defaultVehId;
            childBatch.set(
              doc(db, 'users', uid, 'services', s.id),
              stripUndefined({
                id: s.id,
                userId: uid,
                vehicleId: targetVehId,
                date: String(s.date || new Date().toISOString()).slice(0, 64),
                type: String(s.type || 'other').slice(0, 100),
                odometer: Math.max(0, Number(s.odometer) || 0),
                cost: Math.max(0, Number(s.cost) || 0),
                description: (s.description || s.type || 'Service').slice(0, 500),
                parts: s.parts && s.parts.length > 0 ? s.parts.slice(0, 50) : undefined,
                receiptUrl: s.receiptUrl ? s.receiptUrl.slice(0, 2048) : undefined,
                notes: s.notes ? s.notes.slice(0, 5000) : undefined,
                createdAt: serverTimestamp(),
                updatedAt: serverTimestamp(),
              })
            );
            childCount++;
          }

          for (const r of localReminders) {
            const targetVehId = validVehicleIds.has(r.vehicleId) ? r.vehicleId : defaultVehId;
            childBatch.set(
              doc(db, 'users', uid, 'reminders', r.id),
              stripUndefined({
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
                lastServiceDate: r.lastServiceDate
                  ? String(r.lastServiceDate).slice(0, 64)
                  : undefined,
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
              })
            );
            childCount++;
          }

          if (childCount > 0) {
            await childBatch.commit();
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
              customExpenseCategories: Array.isArray(data.customExpenseCategories)
                ? data.customExpenseCategories
                : [],
              customServiceTypes: Array.isArray(data.customServiceTypes)
                ? data.customServiceTypes
                : [],
              createdAt: normalizeTimestampField(data.createdAt),
              updatedAt: normalizeTimestampField(data.updatedAt),
            };
            setUserProfile((prev) => {
              const mergedProfile: UserProfile = {
                ...syncedProfile,
                customExpenseCategories: mergeUniqueStrings(
                  prev.customExpenseCategories,
                  syncedProfile.customExpenseCategories
                ),
                customServiceTypes: mergeUniqueStrings(
                  prev.customServiceTypes,
                  syncedProfile.customServiceTypes
                ),
              };
              saveToLocal('netcar_profile', mergedProfile);
              return mergedProfile;
            });
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
          const pending = readPendingDeletes('vehicles');
          const list: Vehicle[] = snap.docs
            .map((d) => mapVehicleFromDoc(d.data(), d.id))
            .filter((v) => !pending.has(v.id));
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
          const pending = readPendingDeletes('fillups');
          const list: FillUp[] = snap.docs
            .map((d) => mapFillUpFromDoc(d.data(), d.id))
            .filter((f) => !pending.has(f.id));
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
          const pending = readPendingDeletes('expenses');
          const list: Expense[] = snap.docs
            .map((d) => mapExpenseFromDoc(d.data(), d.id))
            .filter((e) => !pending.has(e.id));
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
          const pending = readPendingDeletes('services');
          const list: Service[] = snap.docs
            .map((d) => mapServiceFromDoc(d.data(), d.id))
            .filter((s) => !pending.has(s.id));
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
          const pending = readPendingDeletes('reminders');
          const list: Reminder[] = snap.docs
            .map((d) => mapReminderFromDoc(d.data(), d.id))
            .filter((r) => !pending.has(r.id));
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
        const updated = { ...prev, activeVehicleId: id, updatedAt: new Date().toISOString() };
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
        const updated = { ...prev, preferredLang: newLang, updatedAt: new Date().toISOString() };
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
    markPendingDelete('vehicles', id);
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
        unmarkPendingDelete('vehicles', id);
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
    const nowIso = new Date().toISOString();
    const newFillUp: FillUp = { ...f, id, createdAt: nowIso, updatedAt: nowIso };
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
    markPendingDelete('fillups', id);
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
        unmarkPendingDelete('fillups', id);
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
    const nowIso = new Date().toISOString();
    const newExpense: Expense = { ...e, id, createdAt: nowIso, updatedAt: nowIso };
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
    markPendingDelete('expenses', id);
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
        unmarkPendingDelete('expenses', id);
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
    const nowIso = new Date().toISOString();
    const newService: Service = { ...s, id, createdAt: nowIso, updatedAt: nowIso };
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
    markPendingDelete('services', id);
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
        unmarkPendingDelete('services', id);
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
    const nowIso = new Date().toISOString();
    const newReminder: Reminder = {
      ...r,
      id,
      isActive: true,
      createdAt: nowIso,
      updatedAt: nowIso,
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

  const updateReminder = async (id: string, updates: Partial<Reminder>) => {
    const target = reminders.find((r) => r.id === id);
    if (!target) return;

    const nowIso = new Date().toISOString();
    const merged: Reminder = { ...target, ...updates, updatedAt: nowIso };

    setReminders((prev) => {
      const updated = prev.map((r) => (r.id === id ? merged : r));
      saveToLocal('netcar_reminders', updated);
      return updated;
    });

    const currentUid = auth.currentUser?.uid;
    if (currentUid && isCloudReadyRef.current) {
      const path = `users/${currentUid}/reminders/${id}`;
      setSyncStatus('syncing');
      try {
        await updateDoc(
          doc(db, 'users', currentUid, 'reminders', id),
          buildReminderUpdateData(merged, merged.vehicleId)
        );
        setSyncStatus('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch (error) {
        setSyncStatus('error');
        reportFirestoreErrorSafely(error, OperationType.UPDATE, path);
      }
    }
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
        const updated = prev.map((r) => (r.id === id ? { ...r, isActive: false, updatedAt: nowIso } : r));
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
    markPendingDelete('reminders', id);
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
        unmarkPendingDelete('reminders', id);
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
      const updated = { ...prev, ...prefs, updatedAt: new Date().toISOString() };
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
        customExpenseCategories:
          prefs.customExpenseCategories !== undefined
            ? mergeUniqueStrings(prefs.customExpenseCategories)
            : undefined,
        customServiceTypes:
          prefs.customServiceTypes !== undefined
            ? mergeUniqueStrings(prefs.customServiceTypes)
            : undefined,
        updatedAt: serverTimestamp(),
      });
      const hasCustomCategoryFields =
        cleanPrefs.customExpenseCategories !== undefined ||
        cleanPrefs.customServiceTypes !== undefined;
      const cleanPrefsWithoutCategories = stripUndefined({
        name: prefs.name !== undefined ? prefs.name.slice(0, 200) : undefined,
        email: prefs.email !== undefined ? prefs.email.slice(0, 320) : undefined,
        preferredLang: prefs.preferredLang,
        units: prefs.units,
        currency: prefs.currency,
        activeVehicleId: prefs.activeVehicleId,
        updatedAt: serverTimestamp(),
      });

      const writePreferences = async () => {
        try {
          await updateDoc(doc(db, 'users', currentUid), cleanPrefs);
        } catch (error) {
          if (!hasCustomCategoryFields) throw error;
          // Best-effort category sync: older rules may reject the new fields.
          // Retry the remaining preferences so the rest of the change is saved.
          await updateDoc(doc(db, 'users', currentUid), cleanPrefsWithoutCategories);
        }
      };

      writePreferences()
        .then(() => {
          setSyncStatus('synced');
          setLastSyncedAt(new Date().toISOString());
        })
        .catch((error) => {
          setSyncStatus('error');
          reportFirestoreErrorSafely(error, OperationType.UPDATE, userDocPath);
        });
    }
  };

  const addCustomExpenseCategory = (name: string) => {
    const clean = sanitizeCustomCategory(name);
    if (!clean) return;
    const builtIn = Object.keys(translations.en.categories);
    const current = userProfile.customExpenseCategories || [];
    const exists = [...builtIn, ...current].some(
      (item) => item.toLowerCase() === clean.toLowerCase()
    );
    if (exists) return;
    updatePreferences({
      customExpenseCategories: [...current, clean].slice(-MAX_CUSTOM_CATEGORIES),
    });
  };

  const addCustomServiceType = (name: string) => {
    const clean = sanitizeCustomCategory(name);
    if (!clean) return;
    const builtIn = Object.keys(translations.en.serviceTypes);
    const current = userProfile.customServiceTypes || [];
    const exists = [...builtIn, ...current].some(
      (item) => item.toLowerCase() === clean.toLowerCase()
    );
    if (exists) return;
    updatePreferences({
      customServiceTypes: [...current, clean].slice(-MAX_CUSTOM_CATEGORIES),
    });
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
      const vehiclesCol = collection(db, 'users', uid, 'vehicles');
      const fillupsCol = collection(db, 'users', uid, 'fillups');
      const expensesCol = collection(db, 'users', uid, 'expenses');
      const servicesCol = collection(db, 'users', uid, 'services');
      const remindersCol = collection(db, 'users', uid, 'reminders');

      // Use the freshest local snapshot. React state updates are asynchronous, so
      // flows like "restore backup then sync" would otherwise push stale records.
      const localVehicles = readLocalCollection<Vehicle>('netcar_vehicles', vehicles);
      const localFillUps = readLocalCollection<FillUp>('netcar_fillups', fillUps);
      const localExpenses = readLocalCollection<Expense>('netcar_expenses', expenses);
      const localServices = readLocalCollection<Service>('netcar_services', services);
      const localReminders = readLocalCollection<Reminder>('netcar_reminders', reminders);
      const savedActiveId =
        (typeof window !== 'undefined'
          ? localStorage.getItem('netcar_active_vehicle_id')
          : null) || activeVehicleId;

      // 1. Push deletions queued while offline before reading the cloud, so old
      // remote copies cannot resurrect records the user deleted locally.
      await Promise.all([
        processPendingDeletes('vehicles', `users/${uid}/vehicles`),
        processPendingDeletes('fillups', `users/${uid}/fillups`),
        processPendingDeletes('expenses', `users/${uid}/expenses`),
        processPendingDeletes('services', `users/${uid}/services`),
        processPendingDeletes('reminders', `users/${uid}/reminders`),
      ]);

      // 2. Read both sides.
      const [
        userSnap,
        remoteVehSnap,
        remoteFillSnap,
        remoteExpSnap,
        remoteSvcSnap,
        remoteRemSnap,
      ] = await Promise.all([
        getDoc(userDocRef),
        getDocs(query(vehiclesCol, where('userId', '==', uid))),
        getDocs(query(fillupsCol, where('userId', '==', uid))),
        getDocs(query(expensesCol, where('userId', '==', uid))),
        getDocs(query(servicesCol, where('userId', '==', uid))),
        getDocs(query(remindersCol, where('userId', '==', uid))),
      ]);

      const remoteVehicles = new Map<string, RemoteEntry<Vehicle>>();
      remoteVehSnap.forEach((d) => {
        const raw = d.data();
        const item = mapVehicleFromDoc(raw, d.id);
        remoteVehicles.set(item.id, { item, updatedAtMs: toTimestampMs(raw.updatedAt) });
      });

      const remoteFillUps = new Map<string, RemoteEntry<FillUp>>();
      remoteFillSnap.forEach((d) => {
        const raw = d.data();
        const item = mapFillUpFromDoc(raw, d.id);
        remoteFillUps.set(item.id, { item, updatedAtMs: toTimestampMs(raw.updatedAt) });
      });

      const remoteExpenses = new Map<string, RemoteEntry<Expense>>();
      remoteExpSnap.forEach((d) => {
        const raw = d.data();
        const item = mapExpenseFromDoc(raw, d.id);
        remoteExpenses.set(item.id, { item, updatedAtMs: toTimestampMs(raw.updatedAt) });
      });

      const remoteServices = new Map<string, RemoteEntry<Service>>();
      remoteSvcSnap.forEach((d) => {
        const raw = d.data();
        const item = mapServiceFromDoc(raw, d.id);
        remoteServices.set(item.id, { item, updatedAtMs: toTimestampMs(raw.updatedAt) });
      });

      const remoteReminders = new Map<string, RemoteEntry<Reminder>>();
      remoteRemSnap.forEach((d) => {
        const raw = d.data();
        const item = mapReminderFromDoc(raw, d.id);
        remoteReminders.set(item.id, { item, updatedAtMs: toTimestampMs(raw.updatedAt) });
      });

      const lastSyncMs = getLastSyncMs();

      // 3. Merge vehicles first so child records always have a valid parent.
      const vehicleMerge = await mergeCollection('vehicles', localVehicles, remoteVehicles, lastSyncMs, {
        getId: (v) => v.id,
        getUpdatedAtMs: (v) => toTimestampMs(v.updatedAt),
        withUpdatedAt: (v, iso) => ({ ...v, updatedAt: iso }),
        remap: (v, id) => ({ ...v, id }),
        createRemote: async (id, v) => {
          await setDoc(doc(vehiclesCol, id), buildVehicleCreateData(uid, id, v));
        },
        updateRemote: async (id, v) => {
          await updateDoc(doc(vehiclesCol, id), buildVehicleUpdateData(v));
        },
      });

      const mergedVehicles = vehicleMerge.items;
      const mergedVehicleIds = new Set(mergedVehicles.map((v) => v.id));
      const defaultVehId = mergedVehicles[0]?.id || null;
      const resolveVehicleId = (vehicleId: string): string | null =>
        mergedVehicleIds.has(vehicleId) ? vehicleId : defaultVehId;
      const remapVehicleId = (item: { vehicleId: string }) =>
        resolveVehicleId(item.vehicleId) || item.vehicleId;

      setVehicles(mergedVehicles);
      saveToLocal('netcar_vehicles', mergedVehicles);

      // 4. Merge the remaining collections. Child records pointing at a vehicle
      // deleted on another device are remapped to the first available vehicle.
      const [fillMerge, expenseMerge, serviceMerge, reminderMerge] = await Promise.all([
        mergeCollection('fillups', localFillUps, remoteFillUps, lastSyncMs, {
          getId: (f) => f.id,
          getUpdatedAtMs: (f) => toTimestampMs(f.updatedAt),
          withUpdatedAt: (f, iso) => ({ ...f, updatedAt: iso }),
          remap: (f, id) => ({ ...f, id, vehicleId: remapVehicleId(f) }),
          createRemote: async (id, f) => {
            const vehicleId = resolveVehicleId(f.vehicleId);
            if (!vehicleId) throw new Error('No parent vehicle available');
            await setDoc(doc(fillupsCol, id), buildFillUpCreateData(uid, id, f, vehicleId));
          },
          updateRemote: async (id, f) => {
            const vehicleId = resolveVehicleId(f.vehicleId);
            if (!vehicleId) throw new Error('No parent vehicle available');
            await updateDoc(doc(fillupsCol, id), buildFillUpUpdateData(f, vehicleId));
          },
        }),
        mergeCollection('expenses', localExpenses, remoteExpenses, lastSyncMs, {
          getId: (e) => e.id,
          getUpdatedAtMs: (e) => toTimestampMs(e.updatedAt),
          withUpdatedAt: (e, iso) => ({ ...e, updatedAt: iso }),
          remap: (e, id) => ({ ...e, id, vehicleId: remapVehicleId(e) }),
          createRemote: async (id, e) => {
            const vehicleId = resolveVehicleId(e.vehicleId);
            if (!vehicleId) throw new Error('No parent vehicle available');
            await setDoc(doc(expensesCol, id), buildExpenseCreateData(uid, id, e, vehicleId));
          },
          updateRemote: async (id, e) => {
            const vehicleId = resolveVehicleId(e.vehicleId);
            if (!vehicleId) throw new Error('No parent vehicle available');
            await updateDoc(doc(expensesCol, id), buildExpenseUpdateData(e, vehicleId));
          },
        }),
        mergeCollection('services', localServices, remoteServices, lastSyncMs, {
          getId: (s) => s.id,
          getUpdatedAtMs: (s) => toTimestampMs(s.updatedAt),
          withUpdatedAt: (s, iso) => ({ ...s, updatedAt: iso }),
          remap: (s, id) => ({ ...s, id, vehicleId: remapVehicleId(s) }),
          createRemote: async (id, s) => {
            const vehicleId = resolveVehicleId(s.vehicleId);
            if (!vehicleId) throw new Error('No parent vehicle available');
            await setDoc(doc(servicesCol, id), buildServiceCreateData(uid, id, s, vehicleId));
          },
          updateRemote: async (id, s) => {
            const vehicleId = resolveVehicleId(s.vehicleId);
            if (!vehicleId) throw new Error('No parent vehicle available');
            await updateDoc(doc(servicesCol, id), buildServiceUpdateData(s, vehicleId));
          },
        }),
        mergeCollection('reminders', localReminders, remoteReminders, lastSyncMs, {
          getId: (r) => r.id,
          getUpdatedAtMs: (r) => toTimestampMs(r.updatedAt),
          withUpdatedAt: (r, iso) => ({ ...r, updatedAt: iso }),
          remap: (r, id) => ({ ...r, id, vehicleId: remapVehicleId(r) }),
          createRemote: async (id, r) => {
            const vehicleId = resolveVehicleId(r.vehicleId);
            if (!vehicleId) throw new Error('No parent vehicle available');
            await setDoc(doc(remindersCol, id), buildReminderCreateData(uid, id, r, vehicleId));
          },
          updateRemote: async (id, r) => {
            const vehicleId = resolveVehicleId(r.vehicleId);
            if (!vehicleId) throw new Error('No parent vehicle available');
            await updateDoc(doc(remindersCol, id), buildReminderUpdateData(r, vehicleId));
          },
        }),
      ]);

      setFillUps(fillMerge.items);
      saveToLocal('netcar_fillups', fillMerge.items);
      setExpenses(expenseMerge.items);
      saveToLocal('netcar_expenses', expenseMerge.items);
      setServices(serviceMerge.items);
      saveToLocal('netcar_services', serviceMerge.items);
      setReminders(reminderMerge.items);
      saveToLocal('netcar_reminders', reminderMerge.items);

      // 5. Merge the profile without letting stale local settings win.
      const remoteProfile = userSnap.exists() ? userSnap.data() : null;
      let desiredActiveId = savedActiveId;
      if (mergedVehicles.length > 0 && !mergedVehicleIds.has(desiredActiveId)) {
        desiredActiveId = mergedVehicles[0].id;
      }

      const remoteCustomExpenseCategories = Array.isArray(remoteProfile?.customExpenseCategories)
        ? remoteProfile.customExpenseCategories
        : [];
      const remoteCustomServiceTypes = Array.isArray(remoteProfile?.customServiceTypes)
        ? remoteProfile.customServiceTypes
        : [];
      const mergedCustomExpenseCategories = mergeUniqueStrings(
        userProfile.customExpenseCategories,
        remoteCustomExpenseCategories
      );
      const mergedCustomServiceTypes = mergeUniqueStrings(
        userProfile.customServiceTypes,
        remoteCustomServiceTypes
      );

      if (!remoteProfile) {
        const coreProfileData = stripUndefined({
          userId: uid,
          name:
            (userProfile.name || fbUser.displayName || 'Pilot').trim().slice(0, 200) ||
            'Pilot',
          email:
            (fbUser.email || userProfile.email || 'driver@netcar.app').trim().slice(0, 320) ||
            'driver@netcar.app',
          preferredLang: lang === 'en' || lang === 'fr' ? lang : 'fr',
          units: userProfile.units === 'imperial' ? 'imperial' : 'metric',
          currency: VALID_CURRENCIES.includes(userProfile.currency)
            ? userProfile.currency
            : 'DZD',
          activeVehicleId: desiredActiveId || undefined,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
        try {
          await setDoc(userDocRef, {
            ...coreProfileData,
            customExpenseCategories: mergedCustomExpenseCategories,
            customServiceTypes: mergedCustomServiceTypes,
          });
        } catch {
          // Best-effort category sync: older rules may reject the new fields.
          await setDoc(userDocRef, coreProfileData);
        }
      } else {
        const remoteActive =
          typeof remoteProfile.activeVehicleId === 'string' ? remoteProfile.activeVehicleId : '';
        const localProfileUpdatedMs = toTimestampMs(userProfile.updatedAt);
        const remoteProfileUpdatedMs = toTimestampMs(remoteProfile.updatedAt);
        const localProfileChanged =
          localProfileUpdatedMs > 0 && localProfileUpdatedMs > lastSyncMs;
        const remoteProfileChanged =
          remoteProfileUpdatedMs > 0 && remoteProfileUpdatedMs > lastSyncMs;
        let localProfileWins = false;
        if (lastSyncMs > 0) {
          if (localProfileChanged && !remoteProfileChanged) {
            localProfileWins = true;
          } else if (localProfileChanged && remoteProfileChanged) {
            localProfileWins = localProfileUpdatedMs >= remoteProfileUpdatedMs;
          }
        }

        if (localProfileWins) {
          const profileUpdateData = stripUndefined({
            userId: uid,
            name:
              (userProfile.name || fbUser.displayName || 'Pilot').trim().slice(0, 200) ||
              'Pilot',
            email:
              (fbUser.email || userProfile.email || 'driver@netcar.app').trim().slice(0, 320) ||
              'driver@netcar.app',
            preferredLang: lang === 'en' || lang === 'fr' ? lang : 'fr',
            units: userProfile.units === 'imperial' ? 'imperial' : 'metric',
            currency: VALID_CURRENCIES.includes(userProfile.currency)
              ? userProfile.currency
              : 'DZD',
            activeVehicleId: desiredActiveId || undefined,
            updatedAt: serverTimestamp(),
          });
          try {
            await updateDoc(userDocRef, {
              ...profileUpdateData,
              customExpenseCategories: mergedCustomExpenseCategories,
              customServiceTypes: mergedCustomServiceTypes,
            });
          } catch {
            // Best-effort category sync: older rules may reject the new fields.
            await updateDoc(userDocRef, profileUpdateData);
          }
          setUserProfile((prev) => ({
            ...prev,
            activeVehicleId: desiredActiveId || prev.activeVehicleId,
            customExpenseCategories: mergeUniqueStrings(
              prev.customExpenseCategories,
              mergedCustomExpenseCategories
            ),
            customServiceTypes: mergeUniqueStrings(
              prev.customServiceTypes,
              mergedCustomServiceTypes
            ),
            updatedAt: new Date().toISOString(),
          }));
        } else {
          if (remoteActive && mergedVehicleIds.has(remoteActive)) {
            desiredActiveId = remoteActive;
          }
          const needsActiveRepair =
            mergedVehicles.length > 0 &&
            (!remoteActive || !mergedVehicleIds.has(remoteActive));
          if (needsActiveRepair) {
            await updateDoc(userDocRef, {
              activeVehicleId: mergedVehicles[0].id,
              updatedAt: serverTimestamp(),
            });
            desiredActiveId = mergedVehicles[0].id;
          }
          setUserProfile((prev) => ({
            ...prev,
            customExpenseCategories: mergeUniqueStrings(
              prev.customExpenseCategories,
              mergedCustomExpenseCategories
            ),
            customServiceTypes: mergeUniqueStrings(
              prev.customServiceTypes,
              mergedCustomServiceTypes
            ),
          }));
        }
      }

      if (desiredActiveId && desiredActiveId !== activeVehicleId) {
        setActiveVehicleIdState(desiredActiveId);
        localStorage.setItem('netcar_active_vehicle_id', desiredActiveId);
      }

      const hadFailure =
        vehicleMerge.hadFailure ||
        fillMerge.hadFailure ||
        expenseMerge.hadFailure ||
        serviceMerge.hadFailure ||
        reminderMerge.hadFailure;
      const pushedCount =
        vehicleMerge.pushedCount +
        fillMerge.pushedCount +
        expenseMerge.pushedCount +
        serviceMerge.pushedCount +
        reminderMerge.pushedCount;

      if (!hadFailure) {
        setLastSyncMs(Date.now());
        setLastSyncedAt(new Date().toISOString());
        setSyncStatus('synced');
        return { success: true, count: pushedCount };
      }

      setSyncStatus('error');
      return {
        success: false,
        count: pushedCount,
        error:
          lang === 'fr'
            ? 'Certains éléments n’ont pas pu être envoyés. Ils restent enregistrés localement et seront réessayés à la prochaine synchronisation.'
            : 'Some records could not be uploaded. They remain saved locally and will retry on the next sync.',
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
      const nowIso = new Date().toISOString();

      function stamp<T extends { id: string; updatedAt?: any }>(item: T): T {
        return { ...item, updatedAt: nowIso };
      }

      if (data.vehicles && Array.isArray(data.vehicles)) {
        const imported = (data.vehicles as Vehicle[]).map(stamp);
        imported.forEach((v) => unmarkPendingDelete('vehicles', v.id));
        setVehicles(imported);
        saveToLocal('netcar_vehicles', imported);
      }
      if (data.fillUps && Array.isArray(data.fillUps)) {
        const imported = (data.fillUps as FillUp[]).map(stamp);
        imported.forEach((f) => unmarkPendingDelete('fillups', f.id));
        setFillUps(imported);
        saveToLocal('netcar_fillups', imported);
      }
      if (data.expenses && Array.isArray(data.expenses)) {
        const imported = (data.expenses as Expense[]).map(stamp);
        imported.forEach((e) => unmarkPendingDelete('expenses', e.id));
        setExpenses(imported);
        saveToLocal('netcar_expenses', imported);
      }
      if (data.services && Array.isArray(data.services)) {
        const imported = (data.services as Service[]).map(stamp);
        imported.forEach((s) => unmarkPendingDelete('services', s.id));
        setServices(imported);
        saveToLocal('netcar_services', imported);
      }
      if (data.reminders && Array.isArray(data.reminders)) {
        const imported = (data.reminders as Reminder[]).map(stamp);
        imported.forEach((r) => unmarkPendingDelete('reminders', r.id));
        setReminders(imported);
        saveToLocal('netcar_reminders', imported);
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

  const signInWithGoogle = async (
    loginHint?: string
  ): Promise<{ success: boolean; error?: string }> => {
    try {
      const provider = new GoogleAuthProvider();
      const customParams: Record<string, string> = { prompt: 'select_account' };
      if (loginHint && loginHint.includes('@')) {
        customParams.login_hint = loginHint.trim();
      }
      provider.setCustomParameters(customParams);
      const cred = await signInWithPopup(auth, provider);
      if (cred.user) {
        const fbUser = cred.user;
        const email = fbUser.email || loginHint || 'driver@netcar.app';
        const displayName = fbUser.displayName || email.split('@')[0] || 'Pilot';
        setUser({
          uid: fbUser.uid,
          email,
          displayName,
          photoURL: fbUser.photoURL || undefined,
          isDemo: false,
        });
      }
      return { success: true };
    } catch (err: any) {
      const formatted = formatAuthError(err, lang);
      return { success: false, error: formatted.error };
    }
  };

  const signInWithEmail = async (
    email: string,
    pass: string
  ): Promise<{ success: boolean; error?: string; consoleUrl?: string }> => {
    try {
      const cred = await signInWithEmailAndPassword(auth, email, pass);
      if (cred.user) {
        const fbUser = cred.user;
        const resolvedEmail = fbUser.email || email;
        setUser({
          uid: fbUser.uid,
          email: resolvedEmail,
          displayName: fbUser.displayName || resolvedEmail.split('@')[0] || 'Pilot',
          photoURL: fbUser.photoURL || undefined,
          isDemo: false,
        });
      }
      return { success: true };
    } catch (err: any) {
      const code = (err?.code || '').toLowerCase();
      if (code.includes('operation-not-allowed') || code.includes('configuration-not-found')) {
        const googleRes = await signInWithGoogle(email);
        if (googleRes.success) {
          return { success: true };
        }
      }
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
      if (cred.user) {
        const fbUser = cred.user;
        const resolvedEmail = fbUser.email || email;
        setUser({
          uid: fbUser.uid,
          email: resolvedEmail,
          displayName: name || fbUser.displayName || resolvedEmail.split('@')[0] || 'Pilot',
          photoURL: fbUser.photoURL || undefined,
          isDemo: false,
        });
      }
      return { success: true };
    } catch (err: any) {
      const code = (err?.code || '').toLowerCase();
      if (code.includes('operation-not-allowed') || code.includes('configuration-not-found')) {
        const googleRes = await signInWithGoogle(email);
        if (googleRes.success) {
          return { success: true };
        }
      }
      const formatted = formatAuthError(err, lang);
      return { success: false, ...formatted };
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
      updateReminder,
      completeReminder,
      deleteReminder,
      addCustomExpenseCategory,
      addCustomServiceType,
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
