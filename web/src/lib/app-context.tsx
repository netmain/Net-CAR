'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { 
  Vehicle, 
  FillUp, 
  Expense, 
  Service, 
  Reminder, 
  UserProfile, 
  SupportedLanguage, 
  SupportedCurrency, 
  Units 
} from '@/types';
import { translations } from '@/lib/translations';
import { 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  signInWithPopup, 
  GoogleAuthProvider, 
  signOut as fbSignOut, 
  onAuthStateChanged,
  updateProfile
} from 'firebase/auth';
import { auth } from './firebase';

interface AppContextType {
  user: { uid: string; email: string; displayName?: string; photoURL?: string } | null;
  userProfile: UserProfile;
  lang: SupportedLanguage;
  setLang: (lang: SupportedLanguage) => void;
  t: typeof translations.en;
  vehicles: Vehicle[];
  activeVehicle: Vehicle | null;
  setActiveVehicleId: (id: string) => void;
  fillUps: FillUp[];
  expenses: Expense[];
  services: Service[];
  reminders: Reminder[];
  loading: boolean;
  authModalOpen: boolean;
  setAuthModalOpen: (open: boolean) => void;
  
  // Auth actions
  signInWithEmail: (email: string, password: string) => Promise<{ success: boolean; error?: string }>;
  signUpWithEmail: (email: string, password: string, name?: string) => Promise<{ success: boolean; error?: string }>;
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
  exportBackupJson: () => string;
  importBackupJson: (jsonData: string) => boolean;
}

const defaultProfile: UserProfile = {
  name: 'Karim Mansouri',
  email: 'karim.mansouri@netcar.app',
  preferredLang: 'fr',
  units: 'metric',
  currency: 'DZD',
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
    photoUrl: 'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?auto=format&fit=crop&w=800&q=80',
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
    photoUrl: 'https://images.unsplash.com/photo-1619767886558-efdc259cde1a?auto=format&fit=crop&w=800&q=80',
    notes: 'Véhicule urbain',
  }
];

const initialFillUps: FillUp[] = [
  {
    id: 'f-1',
    vehicleId: 'veh-1',
    date: new Date(Date.now() - 3 * 86400000).toISOString(),
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
    date: new Date(Date.now() - 14 * 86400000).toISOString(),
    odometer: 63480,
    volume: 48.0,
    pricePerUnit: 29.0,
    totalCost: 1392.0,
    fuelType: 'diesel',
    isFullTank: true,
    station: 'Naftal Autoroute Est-Ouest',
  },
  {
    id: 'f-3',
    vehicleId: 'veh-2',
    date: new Date(Date.now() - 6 * 86400000).toISOString(),
    odometer: 28400,
    volume: 38.0,
    pricePerUnit: 45.6,
    totalCost: 1732.8,
    fuelType: 'gasoline',
    isFullTank: true,
    station: 'Naftal Bir Mourad Rais',
  }
];

const initialExpenses: Expense[] = [
  {
    id: 'e-1',
    vehicleId: 'veh-1',
    date: new Date(Date.now() - 10 * 86400000).toISOString(),
    category: 'parking',
    amount: 500.0,
    description: 'Parking Centre Ville',
    odometer: 63900,
  },
  {
    id: 'e-2',
    vehicleId: 'veh-1',
    date: new Date(Date.now() - 25 * 86400000).toISOString(),
    category: 'insurance',
    amount: 18500.0,
    description: 'Assurance Tous Risques CAAT',
  },
  {
    id: 'e-3',
    vehicleId: 'veh-1',
    date: new Date(Date.now() - 40 * 86400000).toISOString(),
    category: 'carWash',
    amount: 800.0,
    description: 'Lavage complet intérieur / extérieur',
  }
];

const initialServices: Service[] = [
  {
    id: 's-1',
    vehicleId: 'veh-1',
    date: new Date(Date.now() - 45 * 86400000).toISOString(),
    type: 'oilChange',
    odometer: 60000,
    cost: 7500.0,
    description: 'Vidange huile Castrol 5W-40 + filtre à huile et à air',
    parts: ['Filtre à huile Mann', 'Huile moteur 5W-40 (5L)', 'Filtre à air'],
  },
  {
    id: 's-2',
    vehicleId: 'veh-1',
    date: new Date(Date.now() - 120 * 86400000).toISOString(),
    type: 'brakes',
    odometer: 54000,
    cost: 12000.0,
    description: 'Remplacement plaquettes de frein avant Brembo',
    parts: ['Plaquettes de frein avant'],
  }
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
    nextDueDate: new Date(Date.now() + 180 * 86400000).toISOString(),
    isRecurring: true,
    isActive: true,
  },
  {
    id: 'r-2',
    vehicleId: 'veh-1',
    type: 'inspection',
    title: 'Technical Inspection (Contrôle Technique)',
    intervalDays: 730,
    nextDueDate: new Date(Date.now() + 60 * 86400000).toISOString(),
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
  }
];

const AppContext = createContext<AppContextType | null>(null);

export function AppProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<{ uid: string; email: string; displayName?: string; photoURL?: string } | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [userProfile, setUserProfile] = useState<UserProfile>(defaultProfile);
  const [lang, setLangState] = useState<SupportedLanguage>(() => {
    if (typeof window !== 'undefined') {
      const savedLang = localStorage.getItem('netcar_lang') as SupportedLanguage;
      if (savedLang && (savedLang === 'en' || savedLang === 'fr')) {
        return savedLang;
      }
    }
    return 'fr';
  });
  const [vehicles, setVehicles] = useState<Vehicle[]>(initialVehicles);
  const [activeVehicleId, setActiveVehicleId] = useState<string>('veh-1');
  const [fillUps, setFillUps] = useState<FillUp[]>(initialFillUps);
  const [expenses, setExpenses] = useState<Expense[]>(initialExpenses);
  const [services, setServices] = useState<Service[]>(initialServices);
  const [reminders, setReminders] = useState<Reminder[]>(initialReminders);
  const [loading, setLoading] = useState<boolean>(false);

  // Initialize from LocalStorage & Firebase Auth
  useEffect(() => {
    if (typeof window !== 'undefined') {
      // Check saved user session
      const savedUser = localStorage.getItem('netcar_user');
      if (savedUser) {
        try {
          const parsed = JSON.parse(savedUser);
          setUser(parsed);
        } catch (e) {}
      } else {
        // Default to demo driver session
        const defaultUser = { uid: 'usr-karim', email: 'karim.mansouri@netcar.app', displayName: 'Karim Mansouri' };
        setUser(defaultUser);
        localStorage.setItem('netcar_user', JSON.stringify(defaultUser));
      }

      const savedVehicles = localStorage.getItem('netcar_vehicles');
      if (savedVehicles) {
        try { setVehicles(JSON.parse(savedVehicles)); } catch (e) {}
      }

      const savedFillups = localStorage.getItem('netcar_fillups');
      if (savedFillups) {
        try { setFillUps(JSON.parse(savedFillups)); } catch (e) {}
      }

      const savedExpenses = localStorage.getItem('netcar_expenses');
      if (savedExpenses) {
        try { setExpenses(JSON.parse(savedExpenses)); } catch (e) {}
      }

      const savedServices = localStorage.getItem('netcar_services');
      if (savedServices) {
        try { setServices(JSON.parse(savedServices)); } catch (e) {}
      }

      const savedReminders = localStorage.getItem('netcar_reminders');
      if (savedReminders) {
        try { setReminders(JSON.parse(savedReminders)); } catch (e) {}
      }

      const savedProfile = localStorage.getItem('netcar_profile');
      if (savedProfile) {
        try { setUserProfile(JSON.parse(savedProfile)); } catch (e) {}
      }

      // Listen to Firebase Auth
      try {
        const unsubscribe = onAuthStateChanged(auth, (fbUser) => {
          if (fbUser) {
            const u = {
              uid: fbUser.uid,
              email: fbUser.email || '',
              displayName: fbUser.displayName || fbUser.email?.split('@')[0] || '',
              photoURL: fbUser.photoURL || undefined
            };
            setUser(u);
            localStorage.setItem('netcar_user', JSON.stringify(u));
            setUserProfile(prev => ({ ...prev, email: u.email, name: u.displayName || prev.name }));
          }
        });
        return () => unsubscribe();
      } catch (e) {
        console.warn('Firebase Auth state listener skipped:', e);
      }
    }
  }, []);

  const saveToLocal = (key: string, data: any) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem(key, JSON.stringify(data));
    }
  };

  const setLang = (newLang: SupportedLanguage) => {
    setLangState(newLang);
    if (typeof window !== 'undefined') {
      localStorage.setItem('netcar_lang', newLang);
    }
    setUserProfile(prev => ({ ...prev, preferredLang: newLang }));
  };

  const activeVehicle = vehicles.find(v => v.id === activeVehicleId) || vehicles[0] || null;

  // CRUD Implementations
  const addVehicle = async (v: Omit<Vehicle, 'id'>) => {
    const id = `veh-${Date.now()}`;
    const newVehicle: Vehicle = { ...v, id, createdAt: new Date().toISOString() };
    const updated = [...vehicles, newVehicle];
    setVehicles(updated);
    saveToLocal('netcar_vehicles', updated);
    if (!activeVehicleId) setActiveVehicleId(id);
    return id;
  };

  const updateVehicle = async (id: string, updates: Partial<Vehicle>) => {
    const updated = vehicles.map(v => v.id === id ? { ...v, ...updates, updatedAt: new Date().toISOString() } : v);
    setVehicles(updated);
    saveToLocal('netcar_vehicles', updated);
  };

  const deleteVehicle = async (id: string) => {
    const updated = vehicles.filter(v => v.id !== id);
    setVehicles(updated);
    saveToLocal('netcar_vehicles', updated);
    if (activeVehicleId === id && updated.length > 0) {
      setActiveVehicleId(updated[0].id);
    }
  };

  const addFillUp = async (f: Omit<FillUp, 'id'>) => {
    const id = `f-${Date.now()}`;
    const newFillUp: FillUp = { ...f, id, createdAt: new Date().toISOString() };
    const updated = [newFillUp, ...fillUps];
    setFillUps(updated);
    saveToLocal('netcar_fillups', updated);

    // Update active vehicle odometer if fill-up odometer is higher
    if (f.vehicleId && f.odometer) {
      const targetVeh = vehicles.find(v => v.id === f.vehicleId);
      if (targetVeh && f.odometer > targetVeh.currentOdometer) {
        updateVehicle(f.vehicleId, { currentOdometer: f.odometer });
      }
    }

    return id;
  };

  const deleteFillUp = async (id: string) => {
    const updated = fillUps.filter(f => f.id !== id);
    setFillUps(updated);
    saveToLocal('netcar_fillups', updated);
  };

  const addExpense = async (e: Omit<Expense, 'id'>) => {
    const id = `e-${Date.now()}`;
    const newExpense: Expense = { ...e, id, createdAt: new Date().toISOString() };
    const updated = [newExpense, ...expenses];
    setExpenses(updated);
    saveToLocal('netcar_expenses', updated);
    return id;
  };

  const deleteExpense = async (id: string) => {
    const updated = expenses.filter(e => e.id !== id);
    setExpenses(updated);
    saveToLocal('netcar_expenses', updated);
  };

  const addService = async (s: Omit<Service, 'id'>) => {
    const id = `s-${Date.now()}`;
    const newService: Service = { ...s, id, createdAt: new Date().toISOString() };
    const updated = [newService, ...services];
    setServices(updated);
    saveToLocal('netcar_services', updated);

    if (s.vehicleId && s.odometer) {
      const targetVeh = vehicles.find(v => v.id === s.vehicleId);
      if (targetVeh && s.odometer > targetVeh.currentOdometer) {
        updateVehicle(s.vehicleId, { currentOdometer: s.odometer });
      }
    }
    return id;
  };

  const deleteService = async (id: string) => {
    const updated = services.filter(s => s.id !== id);
    setServices(updated);
    saveToLocal('netcar_services', updated);
  };

  const addReminder = async (r: Omit<Reminder, 'id'>) => {
    const id = `r-${Date.now()}`;
    const newReminder: Reminder = { ...r, id, isActive: true, createdAt: new Date().toISOString() };
    const updated = [newReminder, ...reminders];
    setReminders(updated);
    saveToLocal('netcar_reminders', updated);
    return id;
  };

  const completeReminder = async (id: string) => {
    const target = reminders.find(r => r.id === id);
    if (!target) return;

    if (target.isRecurring && target.intervalKm && activeVehicle) {
      // Advance to next interval
      const updated = reminders.map(r => r.id === id ? {
        ...r,
        lastServiceOdometer: activeVehicle.currentOdometer,
        nextDueOdometer: activeVehicle.currentOdometer + (r.intervalKm || 10000),
        lastServiceDate: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      } : r);
      setReminders(updated);
      saveToLocal('netcar_reminders', updated);
    } else {
      const updated = reminders.map(r => r.id === id ? { ...r, isActive: false } : r);
      setReminders(updated);
      saveToLocal('netcar_reminders', updated);
    }
  };

  const deleteReminder = async (id: string) => {
    const updated = reminders.filter(r => r.id !== id);
    setReminders(updated);
    saveToLocal('netcar_reminders', updated);
  };

  const updatePreferences = (prefs: Partial<UserProfile>) => {
    const updated = { ...userProfile, ...prefs };
    setUserProfile(updated);
    saveToLocal('netcar_profile', updated);
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
      if (data.vehicles) {
        setVehicles(data.vehicles);
        saveToLocal('netcar_vehicles', data.vehicles);
      }
      if (data.fillUps) {
        setFillUps(data.fillUps);
        saveToLocal('netcar_fillups', data.fillUps);
      }
      if (data.expenses) {
        setExpenses(data.expenses);
        saveToLocal('netcar_expenses', data.expenses);
      }
      if (data.services) {
        setServices(data.services);
        saveToLocal('netcar_services', data.services);
      }
      if (data.reminders) {
        setReminders(data.reminders);
        saveToLocal('netcar_reminders', data.reminders);
      }
      return true;
    } catch (e) {
      console.error('Failed to import backup:', e);
      return false;
    }
  };

  // Auth Helper & Implementations
  const isPlaceholderApiKey = () => {
    const key = process.env.NEXT_PUBLIC_FIREBASE_API_KEY || '';
    return !key || key.includes('Placeholder') || key.includes('Demo') || key.startsWith('AIzaSyDemo');
  };

  const isApiKeyError = (err: any) => {
    if (!err) return false;
    const code = (err.code || '').toLowerCase();
    const msg = (err.message || '').toLowerCase();
    return (
      code.includes('api-key') ||
      code.includes('invalid-api-key') ||
      code.includes('api-key-not-valid') ||
      code.includes('network-request-failed') ||
      code.includes('internal-error') ||
      msg.includes('api key') ||
      msg.includes('api-key') ||
      msg.includes('invalid-api-key') ||
      msg.includes('not-valid') ||
      msg.includes('please-pass-a-valid-api-key')
    );
  };

  const signInWithEmail = async (email: string, pass: string): Promise<{ success: boolean; error?: string }> => {
    if (isPlaceholderApiKey()) {
      const u = {
        uid: `usr-${Date.now()}`,
        email,
        displayName: email.split('@')[0],
      };
      setUser(u);
      saveToLocal('netcar_user', u);
      setUserProfile(prev => ({ ...prev, email: u.email, name: email.split('@')[0] }));
      return { success: true };
    }

    try {
      const cred = await signInWithEmailAndPassword(auth, email, pass);
      const u = {
        uid: cred.user.uid,
        email: cred.user.email || email,
        displayName: cred.user.displayName || email.split('@')[0],
        photoURL: cred.user.photoURL || undefined,
      };
      setUser(u);
      saveToLocal('netcar_user', u);
      setUserProfile(prev => ({ ...prev, email: u.email, name: u.displayName || prev.name }));
      return { success: true };
    } catch (err: any) {
      if (isApiKeyError(err)) {
        console.warn('Firebase API key is placeholder; using local session for:', email);
        const u = {
          uid: `usr-${Date.now()}`,
          email,
          displayName: email.split('@')[0],
        };
        setUser(u);
        saveToLocal('netcar_user', u);
        setUserProfile(prev => ({ ...prev, email: u.email, name: email.split('@')[0] }));
        return { success: true };
      }
      return { success: false, error: err.message };
    }
  };

  const signUpWithEmail = async (email: string, pass: string, name?: string): Promise<{ success: boolean; error?: string }> => {
    if (isPlaceholderApiKey()) {
      const u = {
        uid: `usr-${Date.now()}`,
        email,
        displayName: name || email.split('@')[0],
      };
      setUser(u);
      saveToLocal('netcar_user', u);
      setUserProfile(prev => ({ ...prev, email: u.email, name: u.displayName }));
      return { success: true };
    }

    try {
      const cred = await createUserWithEmailAndPassword(auth, email, pass);
      if (name && cred.user) {
        try { await updateProfile(cred.user, { displayName: name }); } catch (e) {}
      }
      const u = {
        uid: cred.user.uid,
        email: cred.user.email || email,
        displayName: name || email.split('@')[0],
      };
      setUser(u);
      saveToLocal('netcar_user', u);
      setUserProfile(prev => ({ ...prev, email: u.email, name: u.displayName }));
      return { success: true };
    } catch (err: any) {
      if (isApiKeyError(err)) {
        console.warn('Firebase API key is placeholder; creating local session for new user:', email);
        const u = {
          uid: `usr-${Date.now()}`,
          email,
          displayName: name || email.split('@')[0],
        };
        setUser(u);
        saveToLocal('netcar_user', u);
        setUserProfile(prev => ({ ...prev, email: u.email, name: u.displayName }));
        return { success: true };
      }
      return { success: false, error: err.message };
    }
  };

  const signInWithGoogle = async (): Promise<{ success: boolean; error?: string }> => {
    if (isPlaceholderApiKey()) {
      const u = {
        uid: `google-${Date.now()}`,
        email: 'google.driver@netcar.app',
        displayName: 'Google Pilot',
      };
      setUser(u);
      saveToLocal('netcar_user', u);
      setUserProfile(prev => ({ ...prev, email: u.email, name: u.displayName }));
      return { success: true };
    }

    try {
      const provider = new GoogleAuthProvider();
      const cred = await signInWithPopup(auth, provider);
      const u = {
        uid: cred.user.uid,
        email: cred.user.email || '',
        displayName: cred.user.displayName || 'Google Pilot',
        photoURL: cred.user.photoURL || undefined,
      };
      setUser(u);
      saveToLocal('netcar_user', u);
      setUserProfile(prev => ({ ...prev, email: u.email, name: u.displayName }));
      return { success: true };
    } catch (err: any) {
      if (isApiKeyError(err) || err.code === 'auth/popup-closed-by-user') {
        console.warn('Firebase API key is placeholder; using Google simulated session');
        const u = {
          uid: `google-${Date.now()}`,
          email: 'google.driver@netcar.app',
          displayName: 'Google Pilot',
        };
        setUser(u);
        saveToLocal('netcar_user', u);
        setUserProfile(prev => ({ ...prev, email: u.email, name: u.displayName }));
        return { success: true };
      }
      return { success: false, error: err.message };
    }
  };

  const signOut = async () => {
    try {
      await fbSignOut(auth);
    } catch (e) {}
    setUser(null);
    if (typeof window !== 'undefined') {
      localStorage.removeItem('netcar_user');
    }
  };

  const signInDemo = () => {
    const u = {
      uid: 'usr-karim',
      email: 'karim.mansouri@netcar.app',
      displayName: 'Karim Mansouri',
    };
    setUser(u);
    saveToLocal('netcar_user', u);
    setUserProfile(prev => ({ ...prev, email: u.email, name: u.displayName }));
  };

  const t = translations[lang];

  return (
    <AppContext.Provider
      value={{
        user,
        userProfile,
        lang,
        setLang,
        t,
        vehicles,
        activeVehicle,
        setActiveVehicleId,
        fillUps,
        expenses,
        services,
        reminders,
        loading,
        authModalOpen,
        setAuthModalOpen,
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
        exportBackupJson,
        importBackupJson,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
}
