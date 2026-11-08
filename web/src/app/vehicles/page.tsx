'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useApp } from '@/lib/app-context';
import { Vehicle, FuelType } from '@/types';
import { 
  Car, 
  Plus, 
  Edit3, 
  Trash2, 
  CheckCircle, 
  Gauge, 
  Fuel, 
  Calendar, 
  Hash, 
  X, 
  AlertTriangle,
  Sparkles,
  ShieldCheck,
  Activity
} from 'lucide-react';

export default function VehiclesPage() {
  const { 
    vehicles, 
    activeVehicle, 
    setActiveVehicleId, 
    updateVehicle, 
    deleteVehicle, 
    fillUps, 
    services, 
    expenses, 
    userProfile, 
    t 
  } = useApp();

  const [editingVehicle, setEditingVehicle] = useState<Vehicle | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  // Edit form state
  const [editForm, setEditForm] = useState<Partial<Vehicle>>({});
  const [editErrors, setEditErrors] = useState<Record<string, string>>({});

  const distUnit = userProfile.units === 'metric' ? 'km' : 'mi';

  const handleOpenEdit = (v: Vehicle) => {
    setEditingVehicle(v);
    setEditForm({
      name: v.name || '',
      make: v.make,
      model: v.model,
      year: v.year,
      plate: v.plate || '',
      vin: v.vin || '',
      fuelType: v.fuelType,
      currentOdometer: v.currentOdometer,
      photoUrl: v.photoUrl || '',
      notes: v.notes || '',
    });
    setEditErrors({});
  };

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingVehicle) return;

    const errors: Record<string, string> = {};
    if (!editForm.make?.trim()) errors.make = 'Make is required';
    if (!editForm.model?.trim()) errors.model = 'Model is required';
    if (!editForm.year || editForm.year < 1900 || editForm.year > new Date().getFullYear() + 1) {
      errors.year = 'Valid year is required';
    }
    if (editForm.currentOdometer === undefined || editForm.currentOdometer < 0) {
      errors.currentOdometer = 'Valid odometer is required';
    }

    if (Object.keys(errors).length > 0) {
      setEditErrors(errors);
      return;
    }

    await updateVehicle(editingVehicle.id, {
      name: editForm.name?.trim() || undefined,
      make: editForm.make!.trim(),
      model: editForm.model!.trim(),
      year: Number(editForm.year),
      plate: editForm.plate?.trim() ? editForm.plate.trim().toUpperCase() : '',
      vin: editForm.vin?.trim() || undefined,
      fuelType: editForm.fuelType as FuelType,
      currentOdometer: Number(editForm.currentOdometer),
      photoUrl: editForm.photoUrl?.trim() || undefined,
      notes: editForm.notes?.trim() || undefined,
    });

    setEditingVehicle(null);
  };

  const handleDelete = async (id: string) => {
    await deleteVehicle(id);
    setDeleteConfirmId(null);
  };

  const fuelTypeBadgeColors: Record<FuelType, string> = {
    gasoline: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
    diesel: 'bg-slate-700/50 text-slate-200 border-slate-600/50',
    electric: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
    hybrid: 'bg-teal-500/15 text-teal-300 border-teal-500/30',
    lpg: 'bg-cyan-500/15 text-cyan-300 border-cyan-500/30',
    ethanol: 'bg-orange-500/15 text-orange-300 border-orange-500/30',
    cng: 'bg-indigo-500/15 text-indigo-300 border-indigo-500/30',
  };

  return (
    <div className="space-y-8">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white flex items-center gap-3">
            <span>{t.vehicles.title}</span>
            <span className="rounded-full bg-slate-800 border border-slate-700 px-2.5 py-0.5 text-xs font-mono text-amber-400">
              {vehicles.length}
            </span>
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            {t.vehicles.subtitle} ({vehicles.length} {vehicles.length === 1 ? 'véhicule' : 'véhicules'})
          </p>
        </div>
        <Link
          href="/vehicles/new"
          className="inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-600 px-5 py-2.5 text-sm font-black text-slate-950 shadow-lg shadow-orange-500/20 hover:opacity-95 transition"
        >
          <Plus className="h-4 w-4" />
          <span>{t.vehicles.addVehicle}</span>
        </Link>
      </div>

      {/* Empty State */}
      {vehicles.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-slate-800 bg-[#121826]/60 p-12 text-center shadow-xl">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
            <Car className="h-8 w-8" />
          </div>
          <h2 className="mt-4 text-xl font-bold text-white">{t.dashboard.addFirstVehicle}</h2>
          <p className="mt-2 text-sm text-slate-400 max-w-md mx-auto">
            Ajoutez votre véhicule quotidien, de collection ou votre flotte pour suivre consommation, vidanges et dépenses.
          </p>
          <Link
            href="/vehicles/new"
            className="mt-6 inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-600 px-6 py-3 text-sm font-black text-slate-950 shadow-lg shadow-orange-500/25 hover:opacity-95 transition"
          >
            <Plus className="h-4 w-4" />
            <span>{t.vehicles.addVehicle}</span>
          </Link>
        </div>
      ) : (
        /* Vehicles Grid */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {vehicles.map((v) => {
            const isActive = activeVehicle?.id === v.id;
            const vehFillups = fillUps.filter(f => f.vehicleId === v.id);
            const vehServices = services.filter(s => s.vehicleId === v.id);
            const vehExpenses = expenses.filter(e => e.vehicleId === v.id);

            return (
              <div
                key={v.id}
                className={`group relative flex flex-col justify-between overflow-hidden rounded-3xl border bg-[#121826]/80 backdrop-blur-xl shadow-xl transition-all duration-300 hover:shadow-2xl hover:scale-[1.01] ${
                  isActive 
                    ? 'border-amber-500/80 ring-2 ring-amber-500/20' 
                    : 'border-slate-800/80 hover:border-slate-700'
                }`}
              >
                {/* Photo Header */}
                <div className="relative h-48 w-full overflow-hidden bg-gradient-to-br from-[#0B0F19] to-[#1a2233]">
                  {v.photoUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={v.photoUrl}
                      alt={`${v.make} ${v.model}`}
                      className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
                      onError={(e) => {
                        (e.target as HTMLElement).style.display = 'none';
                      }}
                    />
                  ) : null}
                  
                  {/* Fallback pattern */}
                  <div className={`absolute inset-0 flex items-center justify-center ${v.photoUrl ? '-z-10' : ''}`}>
                    <Car className="h-16 w-16 text-slate-700" />
                  </div>

                  {/* Badges on image */}
                  <div className="absolute top-3 left-3 flex flex-wrap gap-1.5">
                    {isActive ? (
                      <span className="flex items-center gap-1.5 rounded-full bg-gradient-to-r from-amber-500 to-orange-500 px-3 py-1 text-xs font-black text-slate-950 shadow-md">
                        <CheckCircle className="h-3.5 w-3.5" />
                        <span>Actif</span>
                      </span>
                    ) : (
                      <button
                        onClick={() => setActiveVehicleId(v.id)}
                        className="rounded-full bg-slate-950/80 border border-slate-700 backdrop-blur px-3 py-1 text-xs font-bold text-slate-300 hover:text-amber-400 hover:border-amber-500/50 transition"
                      >
                        Sélectionner
                      </button>
                    )}
                  </div>

                  {/* Fuel type badge */}
                  <div className="absolute top-3 right-3">
                    <span className={`rounded-xl border px-2.5 py-1 text-xs font-bold backdrop-blur-md shadow-sm ${fuelTypeBadgeColors[v.fuelType]}`}>
                      {t.fuelTypes[v.fuelType]}
                    </span>
                  </div>

                  {/* Plate pill on bottom right (if available) */}
                  {v.plate ? (
                    <div className="absolute bottom-3 right-3">
                      <span className="rounded-xl border border-slate-700 bg-slate-950/90 backdrop-blur px-2.5 py-1 text-xs font-mono font-black uppercase tracking-wider text-amber-300 shadow-sm">
                        {v.plate}
                      </span>
                    </div>
                  ) : null}
                </div>

                {/* Card Body */}
                <div className="p-5 flex-1 flex flex-col justify-between">
                  <div>
                    <div className="flex items-start justify-between">
                      <div>
                        {v.name && (
                          <span className="text-[10px] font-black text-amber-400 uppercase tracking-widest block">
                            {v.name}
                          </span>
                        )}
                        <h2 className="text-lg font-black text-white group-hover:text-amber-400 transition">
                          {v.make} {v.model}
                        </h2>
                        <p className="text-xs text-slate-400 font-medium mt-0.5">
                          Année {v.year} {v.vin ? `· VIN: ${v.vin}` : ''}
                        </p>
                      </div>
                    </div>

                    {/* Mileage and Info */}
                    <div className="mt-4 grid grid-cols-2 gap-2 rounded-2xl border border-slate-800 bg-[#0B0F19]/80 p-3 text-xs">
                      <div>
                        <span className="text-slate-500 block font-bold text-[10px] uppercase tracking-wider">{t.dashboard.statsOdometer}</span>
                        <span className="font-mono font-black text-cyan-300 text-sm">
                          {v.currentOdometer.toLocaleString()} {distUnit}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-500 block font-bold text-[10px] uppercase tracking-wider">Activité</span>
                        <span className="font-mono font-bold text-slate-300">
                          {vehFillups.length} pleins · {vehServices.length} serv.
                        </span>
                      </div>
                    </div>

                    {v.notes && (
                      <p className="mt-3 text-xs text-slate-400 italic bg-slate-900/60 border border-slate-800 rounded-xl p-2.5 line-clamp-2">
                        &quot;{v.notes}&quot;
                      </p>
                    )}
                  </div>

                  {/* Actions footer */}
                  <div className="mt-5 flex items-center justify-between border-t border-slate-800/80 pt-3">
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => handleOpenEdit(v)}
                        className="flex items-center gap-1.5 rounded-xl px-2.5 py-1.5 text-xs font-bold text-slate-300 hover:bg-slate-800 hover:text-white transition"
                      >
                        <Edit3 className="h-3.5 w-3.5 text-slate-400" />
                        <span>{t.common.edit}</span>
                      </button>
                      <button
                        onClick={() => setDeleteConfirmId(v.id)}
                        className="flex items-center gap-1.5 rounded-xl px-2.5 py-1.5 text-xs font-bold text-red-400 hover:bg-red-500/10 transition"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                        <span>{t.common.delete}</span>
                      </button>
                    </div>

                    {!isActive && (
                      <button
                        onClick={() => setActiveVehicleId(v.id)}
                        className="text-xs font-bold text-amber-400 hover:text-amber-300 transition"
                      >
                        Activer →
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Edit Vehicle Modal */}
      {editingVehicle && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-md overflow-y-auto">
          <div className="relative w-full max-w-lg rounded-3xl border border-slate-800 bg-[#0F1522] p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <h2 className="text-lg font-black text-white">{t.vehicles.editVehicle}</h2>
              <button
                onClick={() => setEditingVehicle(null)}
                className="rounded-xl p-1 text-slate-400 hover:bg-slate-800 hover:text-white transition"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSaveEdit} className="mt-4 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1">
                    {t.vehicles.make} *
                  </label>
                  <input
                    type="text"
                    value={editForm.make || ''}
                    onChange={(e) => setEditForm({ ...editForm, make: e.target.value })}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2 text-sm text-white font-medium focus:border-amber-500 focus:outline-none"
                  />
                  {editErrors.make && <p className="mt-1 text-xs text-red-400">{editErrors.make}</p>}
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1">
                    {t.vehicles.model} *
                  </label>
                  <input
                    type="text"
                    value={editForm.model || ''}
                    onChange={(e) => setEditForm({ ...editForm, model: e.target.value })}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2 text-sm text-white font-medium focus:border-amber-500 focus:outline-none"
                  />
                  {editErrors.model && <p className="mt-1 text-xs text-red-400">{editErrors.model}</p>}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1">
                    {t.vehicles.year} *
                  </label>
                  <input
                    type="number"
                    value={editForm.year || ''}
                    onChange={(e) => setEditForm({ ...editForm, year: Number(e.target.value) })}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2 text-sm text-white font-medium focus:border-amber-500 focus:outline-none"
                  />
                  {editErrors.year && <p className="mt-1 text-xs text-red-400">{editErrors.year}</p>}
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1">
                    {t.vehicles.plate} <span className="text-slate-500 font-normal">({t.common.filter === 'Filtrer' ? 'Optionnel' : 'Optional'})</span>
                  </label>
                  <input
                    type="text"
                    value={editForm.plate || ''}
                    onChange={(e) => setEditForm({ ...editForm, plate: e.target.value })}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2 text-sm text-white font-mono font-bold focus:border-amber-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1">
                    {t.vehicles.fuelType} *
                  </label>
                  <select
                    value={editForm.fuelType || 'gasoline'}
                    onChange={(e) => setEditForm({ ...editForm, fuelType: e.target.value as FuelType })}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2 text-sm text-white font-medium focus:border-amber-500 focus:outline-none"
                  >
                    {(Object.keys(t.fuelTypes) as FuelType[]).map((ft) => (
                      <option key={ft} value={ft} className="bg-slate-900 text-white">
                        {t.fuelTypes[ft]}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-300 mb-1">
                    {t.vehicles.currentOdometer} ({distUnit}) *
                  </label>
                  <input
                    type="number"
                    value={editForm.currentOdometer || ''}
                    onChange={(e) => setEditForm({ ...editForm, currentOdometer: Number(e.target.value) })}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2 text-sm text-white font-mono font-bold focus:border-amber-500 focus:outline-none"
                  />
                  {editErrors.currentOdometer && <p className="mt-1 text-xs text-red-400">{editErrors.currentOdometer}</p>}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-300 mb-1">
                  {t.vehicles.photoUrl}
                </label>
                <input
                  type="url"
                  placeholder="https://..."
                  value={editForm.photoUrl || ''}
                  onChange={(e) => setEditForm({ ...editForm, photoUrl: e.target.value })}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2 text-sm text-white font-medium focus:border-amber-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-300 mb-1">
                  {t.vehicles.notes}
                </label>
                <textarea
                  rows={2}
                  value={editForm.notes || ''}
                  onChange={(e) => setEditForm({ ...editForm, notes: e.target.value })}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2 text-sm text-white font-medium focus:border-amber-500 focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setEditingVehicle(null)}
                  className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-bold text-slate-300 hover:bg-slate-800 transition"
                >
                  {t.common.cancel}
                </button>
                <button
                  type="submit"
                  className="rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 px-5 py-2 text-sm font-black text-slate-950 shadow-md shadow-orange-500/20 hover:opacity-95 transition"
                >
                  {t.common.save}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      {deleteConfirmId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-3xl border border-slate-800 bg-[#0F1522] p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3 text-red-400">
              <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-red-500/15 border border-red-500/30">
                <AlertTriangle className="h-5 w-5" />
              </div>
              <h2 className="text-lg font-black text-white">{t.common.delete}</h2>
            </div>
            <p className="mt-3 text-sm text-slate-400 leading-relaxed">
              {t.vehicles.deleteConfirm}
            </p>
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                onClick={() => setDeleteConfirmId(null)}
                className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-bold text-slate-300 hover:bg-slate-800 transition"
              >
                {t.common.cancel}
              </button>
              <button
                onClick={() => handleDelete(deleteConfirmId)}
                className="rounded-xl bg-red-600 px-5 py-2 text-sm font-black text-white shadow-md hover:bg-red-700 transition"
              >
                {t.common.confirm}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
