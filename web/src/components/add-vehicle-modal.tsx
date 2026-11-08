'use client';

import React, { useState } from 'react';
import { useApp } from '@/lib/app-context';
import { FuelType } from '@/types';
import { Car, Check, Sparkles, Image as ImageIcon, X } from 'lucide-react';

const PRESET_PHOTOS = [
  {
    label: 'Sedan (Blue)',
    url: 'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?auto=format&fit=crop&w=800&q=80',
  },
  {
    label: 'Hatchback (Grey)',
    url: 'https://images.unsplash.com/photo-1619767886558-efdc259cde1a?auto=format&fit=crop&w=800&q=80',
  },
  {
    label: 'SUV (White)',
    url: 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=800&q=80',
  },
  {
    label: 'Electric / Modern',
    url: 'https://images.unsplash.com/photo-1563720223185-11003d516935?auto=format&fit=crop&w=800&q=80',
  },
];

export function AddVehicleModal() {
  const {
    addVehicleModalOpen,
    setAddVehicleModalOpen,
    addVehicle,
    userProfile,
    lang,
    t,
  } = useApp();

  const [name, setName] = useState('');
  const [make, setMake] = useState('');
  const [model, setModel] = useState('');
  const [year, setYear] = useState<number>(new Date().getFullYear());
  const [plate, setPlate] = useState('');
  const [vin, setVin] = useState('');
  const [fuelType, setFuelType] = useState<FuelType>('gasoline');
  const [odometer, setOdometer] = useState<number | ''>(0);
  const [photoUrl, setPhotoUrl] = useState('');
  const [notes, setNotes] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  if (!addVehicleModalOpen) return null;

  const distUnit = userProfile.units === 'metric' ? 'km' : 'mi';

  const resetForm = () => {
    setName('');
    setMake('');
    setModel('');
    setYear(new Date().getFullYear());
    setPlate('');
    setVin('');
    setFuelType('gasoline');
    setOdometer(0);
    setPhotoUrl('');
    setNotes('');
    setErrors({});
  };

  const handleClose = () => {
    resetForm();
    setAddVehicleModalOpen(false);
  };

  const validate = () => {
    const errs: Record<string, string> = {};
    if (!make.trim()) {
      errs.make = lang === 'fr' ? 'La marque est requise' : 'Make is required';
    }
    if (!model.trim()) {
      errs.model = lang === 'fr' ? 'Le modèle est requis' : 'Model is required';
    }
    if (!year || year < 1900 || year > new Date().getFullYear() + 2) {
      errs.year = lang === 'fr' ? 'Année invalide' : 'Please enter a valid year';
    }
    if (odometer === '' || Number(odometer) < 0) {
      errs.odometer =
        lang === 'fr'
          ? 'Kilométrage initial requis'
          : 'Initial odometer reading is required';
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
        currentOdometer: Number(odometer) || 0,
        photoUrl: photoUrl.trim() || undefined,
        notes: notes.trim() || undefined,
      });
      resetForm();
      setAddVehicleModalOpen(false);
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md overflow-y-auto">
      <div className="relative w-full max-w-2xl rounded-3xl border border-slate-800 bg-[#0F1522] p-6 sm:p-8 shadow-2xl my-8 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
              <Car className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-black text-white">{t.vehicles.addVehicle}</h2>
              <p className="text-xs text-slate-400">{t.vehicles.subtitle}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleClose}
            className="rounded-xl p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white transition cursor-pointer"
            aria-label="Close"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-5 space-y-5">
          {/* Basic Specs */}
          <div>
            <h3 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-2">
              <Car className="h-4 w-4 text-cyan-400" />
              <span>
                {lang === 'fr' ? 'Spécifications du Véhicule' : 'Vehicle Specifications'}
              </span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
                  {t.vehicles.make} *
                </label>
                <input
                  type="text"
                  placeholder="e.g. Toyota, BMW, Peugeot"
                  value={make}
                  onChange={(e) => setMake(e.target.value)}
                  className={`w-full rounded-xl border bg-slate-900/90 px-3.5 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:outline-none ${
                    errors.make
                      ? 'border-red-500/80 focus:border-red-500'
                      : 'border-slate-700 focus:border-amber-500'
                  }`}
                />
                {errors.make && (
                  <p className="mt-1 text-xs text-red-400 font-mono">{errors.make}</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
                  {t.vehicles.model} *
                </label>
                <input
                  type="text"
                  placeholder="e.g. Golf VII, Corolla, Clio"
                  value={model}
                  onChange={(e) => setModel(e.target.value)}
                  className={`w-full rounded-xl border bg-slate-900/90 px-3.5 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:outline-none ${
                    errors.model
                      ? 'border-red-500/80 focus:border-red-500'
                      : 'border-slate-700 focus:border-amber-500'
                  }`}
                />
                {errors.model && (
                  <p className="mt-1 text-xs text-red-400 font-mono">{errors.model}</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
                  {t.vehicles.year} *
                </label>
                <input
                  type="number"
                  value={year}
                  onChange={(e) => setYear(Number(e.target.value))}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-mono text-slate-100 focus:border-amber-500 focus:outline-none"
                />
                {errors.year && (
                  <p className="mt-1 text-xs text-red-400 font-mono">{errors.year}</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
                  {lang === 'fr' ? 'Surnom / Libellé (Optionnel)' : 'Nickname / Label (Optional)'}
                </label>
                <input
                  type="text"
                  placeholder="e.g. Daily Driver, Véhicule Pro"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Registration & Fuel */}
          <div className="border-t border-slate-800 pt-4">
            <h3 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-amber-400" />
              <span>
                {lang === 'fr' ? 'Immatriculation & Carburant' : 'Registration & Fuel'}
              </span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
                  {t.vehicles.plate}{' '}
                  <span className="text-slate-500 font-normal">
                    ({lang === 'fr' ? 'Optionnel' : 'Optional'})
                  </span>
                </label>
                <input
                  type="text"
                  placeholder="e.g. 00124-116-16"
                  value={plate}
                  onChange={(e) => setPlate(e.target.value)}
                  className="w-full uppercase rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-mono font-bold tracking-wider text-amber-400 placeholder-slate-500 focus:border-amber-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
                  {t.vehicles.fuelType} *
                </label>
                <select
                  value={fuelType}
                  onChange={(e) => setFuelType(e.target.value as FuelType)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-semibold text-slate-100 focus:border-amber-500 focus:outline-none"
                >
                  {(Object.keys(t.fuelTypes) as FuelType[]).map((ft) => (
                    <option key={ft} value={ft} className="bg-slate-900 text-slate-100">
                      {t.fuelTypes[ft]}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
                  {t.vehicles.currentOdometer} ({distUnit}) *
                </label>
                <input
                  type="number"
                  placeholder="e.g. 45000"
                  value={odometer}
                  onChange={(e) =>
                    setOdometer(e.target.value === '' ? '' : Number(e.target.value))
                  }
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none"
                />
                {errors.odometer && (
                  <p className="mt-1 text-xs text-red-400 font-mono">{errors.odometer}</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
                  {t.vehicles.vin}{' '}
                  <span className="text-slate-500 font-normal">
                    ({lang === 'fr' ? 'Optionnel' : 'Optional'})
                  </span>
                </label>
                <input
                  type="text"
                  placeholder="e.g. WVWZZZ3CZWE000000"
                  value={vin}
                  onChange={(e) => setVin(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Photo & Notes */}
          <div className="border-t border-slate-800 pt-4 space-y-3">
            <h3 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
              <ImageIcon className="h-4 w-4 text-purple-400" />
              <span>
                {lang === 'fr' ? 'Photo & Notes Personnelles' : 'Photo & Personal Notes'}
              </span>
            </h3>

            <div>
              <input
                type="url"
                placeholder="https://example.com/car-photo.jpg"
                value={photoUrl}
                onChange={(e) => setPhotoUrl(e.target.value)}
                className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none"
              />
              <div className="mt-2 flex flex-wrap items-center gap-1.5">
                <span className="text-[11px] font-mono text-slate-500">
                  {lang === 'fr' ? 'Photos rapides :' : 'Quick presets:'}
                </span>
                {PRESET_PHOTOS.map((p) => (
                  <button
                    type="button"
                    key={p.label}
                    onClick={() => setPhotoUrl(p.url)}
                    className="rounded-lg border border-slate-800 bg-slate-900 px-2.5 py-1 text-[11px] font-mono text-slate-300 hover:border-amber-500/40 hover:text-amber-400 transition cursor-pointer"
                  >
                    {p.label}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <textarea
                rows={2}
                placeholder={
                  lang === 'fr'
                    ? "Détails d'achat, fin de garantie, dimensions pneus..."
                    : 'Purchase details, warranty expiration, tire sizes...'
                }
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
            <button
              type="button"
              onClick={handleClose}
              className="rounded-xl border border-slate-700 px-5 py-2.5 text-sm font-semibold text-slate-300 hover:bg-slate-800 transition cursor-pointer"
            >
              {t.common.cancel}
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 px-6 py-2.5 text-sm font-black text-slate-950 shadow-lg shadow-amber-500/20 hover:opacity-95 disabled:opacity-50 transition cursor-pointer"
            >
              <Check className="h-4 w-4" />
              <span>{submitting ? t.common.loading : t.common.save}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
