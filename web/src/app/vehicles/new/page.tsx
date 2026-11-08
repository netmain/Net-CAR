'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useApp } from '@/lib/app-context';
import { FuelType } from '@/types';
import { 
  Car, 
  ArrowLeft, 
  Check, 
  Sparkles, 
  Image as ImageIcon 
} from 'lucide-react';

const PRESET_PHOTOS = [
  { label: 'Sedan (Blue)', url: 'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?auto=format&fit=crop&w=800&q=80' },
  { label: 'Hatchback (Grey)', url: 'https://images.unsplash.com/photo-1619767886558-efdc259cde1a?auto=format&fit=crop&w=800&q=80' },
  { label: 'SUV (White)', url: 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=800&q=80' },
  { label: 'Electric / Modern', url: 'https://images.unsplash.com/photo-1563720223185-11003d516935?auto=format&fit=crop&w=800&q=80' },
];

export default function NewVehiclePage() {
  const router = useRouter();
  const { addVehicle, userProfile, lang, t } = useApp();

  const [name, setName] = useState('');
  const [make, setMake] = useState('');
  const [model, setModel] = useState('');
  const [year, setYear] = useState<number>(new Date().getFullYear());
  const [plate, setPlate] = useState('');
  const [vin, setVin] = useState('');
  const [fuelType, setFuelType] = useState<FuelType>('gasoline');
  const [odometer, setOdometer] = useState<number | ''>('');
  const [photoUrl, setPhotoUrl] = useState('');
  const [notes, setNotes] = useState('');

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const distUnit = userProfile.units === 'metric' ? 'km' : 'mi';

  const validate = () => {
    const errs: Record<string, string> = {};
    if (!make.trim()) errs.make = lang === 'fr' ? 'La marque est requise' : 'Make is required';
    if (!model.trim()) errs.model = lang === 'fr' ? 'Le modèle est requis' : 'Model is required';
    if (!year || year < 1900 || year > new Date().getFullYear() + 2) {
      errs.year = lang === 'fr' ? 'Année invalide' : 'Please enter a valid year';
    }
    if (odometer === '' || Number(odometer) < 0) {
      errs.odometer = lang === 'fr' ? 'Kilométrage initial requis' : 'Initial odometer reading is required';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    try {
      await addVehicle({
        name: name.trim() || undefined,
        make: make.trim(),
        model: model.trim(),
        year: Number(year),
        plate: plate.trim() ? plate.trim().toUpperCase() : '',
        vin: vin.trim() || undefined,
        fuelType,
        currentOdometer: Number(odometer),
        photoUrl: photoUrl.trim() || undefined,
        notes: notes.trim() || undefined,
      });

      router.push('/vehicles');
    } catch (err) {
      console.error(err);
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6 animate-in fade-in duration-300">
      {/* Top Header */}
      <div className="flex items-center gap-4 border-b border-slate-800/80 pb-4">
        <Link
          href="/vehicles"
          className="flex h-10 w-10 items-center justify-center rounded-xl border border-slate-800 bg-[#0F1522] text-slate-300 shadow-md hover:bg-slate-800 hover:text-white transition"
        >
          <ArrowLeft className="h-5 w-5" />
        </Link>
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-slate-100 font-mono">
            {t.vehicles.addVehicle}
          </h1>
          <p className="text-xs text-slate-400">
            {t.vehicles.subtitle}
          </p>
        </div>
      </div>

      {/* Main Form Card */}
      <form onSubmit={handleSubmit} className="rounded-2xl border border-slate-800 bg-[#0F1522] p-6 sm:p-8 shadow-xl shadow-black/40 space-y-6">
        {/* Section 1: Basic Vehicle Specs */}
        <div>
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400 mb-4 flex items-center gap-2">
            <Car className="h-4 w-4 text-cyan-400" />
            <span>{lang === 'fr' ? 'Spécifications du Véhicule' : 'Vehicle Specifications'}</span>
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                {t.vehicles.make} *
              </label>
              <input
                type="text"
                placeholder="e.g. Toyota, BMW, Peugeot"
                value={make}
                onChange={(e) => setMake(e.target.value)}
                className={`w-full rounded-xl border bg-slate-900/90 px-3.5 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 ${
                  errors.make 
                    ? 'border-red-500/80 focus:border-red-500 focus:ring-red-500/30' 
                    : 'border-slate-700 focus:border-amber-500 focus:ring-amber-500/30'
                }`}
              />
              {errors.make && <p className="mt-1 text-xs text-red-400 font-mono">{errors.make}</p>}
            </div>

            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                {t.vehicles.model} *
              </label>
              <input
                type="text"
                placeholder="e.g. Golf VII, Corolla, Clio"
                value={model}
                onChange={(e) => setModel(e.target.value)}
                className={`w-full rounded-xl border bg-slate-900/90 px-3.5 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 ${
                  errors.model 
                    ? 'border-red-500/80 focus:border-red-500 focus:ring-red-500/30' 
                    : 'border-slate-700 focus:border-amber-500 focus:ring-amber-500/30'
                }`}
              />
              {errors.model && <p className="mt-1 text-xs text-red-400 font-mono">{errors.model}</p>}
            </div>

            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                {t.vehicles.year} *
              </label>
              <input
                type="number"
                placeholder="e.g. 2022"
                value={year}
                onChange={(e) => setYear(Number(e.target.value))}
                className={`w-full rounded-xl border bg-slate-900/90 px-3.5 py-2.5 text-sm font-mono text-slate-100 focus:outline-none focus:ring-1 ${
                  errors.year 
                    ? 'border-red-500/80 focus:border-red-500 focus:ring-red-500/30' 
                    : 'border-slate-700 focus:border-amber-500 focus:ring-amber-500/30'
                }`}
              />
              {errors.year && <p className="mt-1 text-xs text-red-400 font-mono">{errors.year}</p>}
            </div>

            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                {lang === 'fr' ? 'Surnom / Libellé (Optionnel)' : 'Nickname / Label (Optional)'}
              </label>
              <input
                type="text"
                placeholder="e.g. Daily Driver, Véhicule Pro"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/30"
              />
            </div>
          </div>
        </div>

        {/* Section 2: Registration & Fuel */}
        <div className="border-t border-slate-800 pt-6">
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400 mb-4 flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-amber-400" />
            <span>{lang === 'fr' ? 'Immatriculation & Carburant' : 'Registration & Fuel'}</span>
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                {t.vehicles.plate} <span className="text-slate-500 font-normal">({lang === 'fr' ? 'Optionnel' : 'Optional'})</span>
              </label>
              <input
                type="text"
                placeholder="e.g. 00124-116-16"
                value={plate}
                onChange={(e) => setPlate(e.target.value)}
                className="w-full uppercase rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-mono font-bold tracking-wider text-amber-400 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/30"
              />
            </div>

            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                {t.vehicles.fuelType} *
              </label>
              <select
                value={fuelType}
                onChange={(e) => setFuelType(e.target.value as FuelType)}
                className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-semibold text-slate-100 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/30"
              >
                {(Object.keys(t.fuelTypes) as FuelType[]).map((ft) => (
                  <option key={ft} value={ft} className="bg-slate-900 text-slate-100">
                    {t.fuelTypes[ft]}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                {t.vehicles.currentOdometer} ({distUnit}) *
              </label>
              <input
                type="number"
                placeholder="e.g. 45000"
                value={odometer}
                onChange={(e) => setOdometer(e.target.value === '' ? '' : Number(e.target.value))}
                className={`w-full rounded-xl border bg-slate-900/90 px-3.5 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 ${
                  errors.odometer 
                    ? 'border-red-500/80 focus:border-red-500 focus:ring-red-500/30' 
                    : 'border-slate-700 focus:border-amber-500 focus:ring-amber-500/30'
                }`}
              />
              {errors.odometer && <p className="mt-1 text-xs text-red-400 font-mono">{errors.odometer}</p>}
            </div>

            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                {t.vehicles.vin} <span className="text-slate-500 font-normal">({lang === 'fr' ? 'Optionnel' : 'Optional'})</span>
              </label>
              <input
                type="text"
                placeholder="e.g. WVWZZZ3CZWE000000"
                value={vin}
                onChange={(e) => setVin(e.target.value)}
                className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/30"
              />
            </div>
          </div>
        </div>

        {/* Section 3: Photo & Notes */}
        <div className="border-t border-slate-800 pt-6 space-y-4">
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-2">
            <ImageIcon className="h-4 w-4 text-purple-400" />
            <span>{lang === 'fr' ? 'Photo & Notes Personnelles' : 'Photo & Personal Notes'}</span>
          </h2>

          <div>
            <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
              {t.vehicles.photoUrl}
            </label>
            <input
              type="url"
              placeholder="https://example.com/car-photo.jpg"
              value={photoUrl}
              onChange={(e) => setPhotoUrl(e.target.value)}
              className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/30"
            />
            {/* Quick Presets */}
            <div className="mt-2.5 flex flex-wrap items-center gap-2">
              <span className="text-[11px] font-mono text-slate-500">{lang === 'fr' ? 'Préréglages rapides :' : 'Quick presets:'}</span>
              {PRESET_PHOTOS.map((p) => (
                <button
                  type="button"
                  key={p.label}
                  onClick={() => setPhotoUrl(p.url)}
                  className="rounded-lg border border-slate-800 bg-slate-900 px-2.5 py-1 text-[11px] font-mono text-slate-300 hover:border-amber-500/40 hover:text-amber-400 transition"
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>

          {photoUrl && (
            <div className="relative h-44 w-full overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={photoUrl}
                alt="Preview"
                className="h-full w-full object-cover"
                onError={(e) => {
                  (e.target as HTMLElement).style.display = 'none';
                }}
              />
            </div>
          )}

          <div>
            <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
              {t.vehicles.notes}
            </label>
            <textarea
              rows={3}
              placeholder={lang === 'fr' ? "Détails d'achat, fin de garantie, dimensions pneus, indice d'huile..." : "Purchase details, warranty expiration, tire sizes, or oil specs..."}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/30"
            />
          </div>
        </div>

        {/* Buttons */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
          <Link
            href="/vehicles"
            className="rounded-xl border border-slate-700 px-5 py-2.5 text-sm font-semibold text-slate-300 hover:bg-slate-800 transition"
          >
            {t.common.cancel}
          </Link>
          <button
            type="submit"
            disabled={submitting}
            className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-6 py-2.5 text-sm font-bold text-slate-950 shadow-lg shadow-amber-500/20 hover:from-amber-400 hover:to-amber-500 disabled:opacity-50 transition"
          >
            <Check className="h-4 w-4" />
            <span>{submitting ? t.common.loading : t.common.save}</span>
          </button>
        </div>
      </form>
    </div>
  );
}