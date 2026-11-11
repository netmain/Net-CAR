'use client';

import React, { useState } from 'react';
import { useApp } from '@/lib/app-context';
import {
  getLatestVehicleOdometer,
  validateVehicleOdometer,
} from '@/lib/telemetry-math';
import { 
  Wrench, 
  Plus, 
  Search, 
  Trash2, 
  DollarSign, 
  Gauge, 
  CheckCircle2, 
  Package, 
  X, 
  AlertTriangle,
  ExternalLink,
  Tag
} from 'lucide-react';

export default function MaintenancePage() {
  const { 
    activeVehicle, 
    fillUps,
    services, 
    expenses,
    addService, 
    deleteService, 
    addCustomServiceType,
    userProfile, 
    lang,
    t 
  } = useApp();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState<string>('all');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);
  const [showCategoryDialog, setShowCategoryDialog] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [categoryError, setCategoryError] = useState('');

  // Form State
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [type, setType] = useState<string>('oilChange');
  const [odometer, setOdometer] = useState<number | ''>(activeVehicle?.currentOdometer || '');
  const [cost, setCost] = useState<number | ''>('');
  const [description, setDescription] = useState('');
  const [partsInput, setPartsInput] = useState('');
  const [receiptUrl, setReceiptUrl] = useState('');
  const [notes, setNotes] = useState('');
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const currencySymbol = userProfile.currency === 'EUR' ? '€' : userProfile.currency === 'USD' ? '$' : userProfile.currency;
  const distUnit = userProfile.units === 'metric' ? 'km' : 'mi';

  // Filter services by active vehicle
  const vehicleServices = services
    .filter(s => !activeVehicle || s.vehicleId === activeVehicle.id)
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

  // Search & Type filter
  const filteredServices = vehicleServices.filter(s => {
    const matchesType = selectedType === 'all' || s.type === selectedType;
    const q = searchQuery.toLowerCase();
    const matchesSearch = 
      s.description.toLowerCase().includes(q) ||
      (s.notes && s.notes.toLowerCase().includes(q)) ||
      (t.serviceTypes[s.type] && t.serviceTypes[s.type].toLowerCase().includes(q)) ||
      (s.parts && s.parts.some(p => p.toLowerCase().includes(q))) ||
      s.odometer.toString().includes(q);
    return matchesType && matchesSearch;
  });

  // KPI Calculations
  const totalCost = vehicleServices.reduce((acc, s) => acc + s.cost, 0);
  const latestService = vehicleServices[0] || null;

  const latestVehicleOdometer = activeVehicle
    ? getLatestVehicleOdometer({
        currentOdometer: activeVehicle.currentOdometer,
        fillUps: fillUps.filter((f) => f.vehicleId === activeVehicle.id),
        services: services.filter((s) => s.vehicleId === activeVehicle.id),
        expenses: expenses.filter((e) => e.vehicleId === activeVehicle.id),
      })
    : 0;

  const handleOpenAdd = () => {
    setDate(new Date().toISOString().split('T')[0]);
    setType('oilChange');
    setOdometer(activeVehicle ? latestVehicleOdometer : '');
    setCost('');
    setDescription('');
    setPartsInput('');
    setReceiptUrl('');
    setNotes('');
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleSaveService = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeVehicle) return;

    const errors: Record<string, string> = {};
    if (!date) errors.date = lang === 'fr' ? 'La date est requise' : 'Date is required';
    if (!description.trim()) {
      errors.description = lang === 'fr' ? 'La description est requise' : 'Description is required';
    }

    const odoValidation = validateVehicleOdometer({
      enteredOdometer: odometer,
      latestOdometer: latestVehicleOdometer,
      distUnit,
      lang,
      requirePositive: false,
    });
    if (!odoValidation.valid && odoValidation.error) {
      errors.odometer = odoValidation.error;
    }

    if (cost === '' || Number(cost) < 0) {
      errors.cost = lang === 'fr' ? 'Un coût valide est requis' : 'Valid cost is required';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    try {
      const parts = partsInput
        .split(',')
        .map(p => p.trim())
        .filter(p => p.length > 0);

      await addService({
        vehicleId: activeVehicle.id,
        date: new Date(date).toISOString(),
        type,
        odometer: Number(odometer),
        cost: Number(cost),
        description: description.trim(),
        parts: parts.length > 0 ? parts : undefined,
        receiptUrl: receiptUrl.trim() || undefined,
        notes: notes.trim() || undefined,
      });

      setIsModalOpen(false);
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    await deleteService(id);
    setDeleteConfirmId(null);
  };

  const allServiceTypes = Array.from(
    new Set([
      ...Object.keys(t.serviceTypes),
      ...(userProfile.customServiceTypes || []),
      ...services.map((s) => s.type).filter(Boolean),
    ])
  );

  const handleServiceTypeSelect = (selected: string) => {
    if (selected === '__add_custom__') {
      setNewCategoryName('');
      setCategoryError('');
      setShowCategoryDialog(true);
      return;
    }
    setType(selected);
  };

  const handleAddCategory = () => {
    const clean = newCategoryName.trim().replace(/\s+/g, ' ');
    if (!clean) {
      setCategoryError(lang === 'fr' ? 'Saisissez un nom de catégorie' : 'Enter a category name');
      return;
    }
    addCustomServiceType(clean);
    setType(clean);
    setShowCategoryDialog(false);
    setNewCategoryName('');
    setCategoryError('');
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-300">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-100 font-mono">
              {t.maintenance.title}
            </h1>
            {activeVehicle && (
              <span className="rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-mono font-bold px-2.5 py-1">
                {activeVehicle.make} {activeVehicle.model} {activeVehicle.plate ? `(${activeVehicle.plate})` : ''}
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-slate-400">
            {t.maintenance.subtitle}
          </p>
        </div>

        <button
          onClick={handleOpenAdd}
          disabled={!activeVehicle}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2.5 text-sm font-bold text-slate-950 shadow-lg shadow-amber-500/20 hover:from-amber-400 hover:to-amber-500 disabled:opacity-50 transition"
        >
          <Plus className="h-4 w-4" />
          <span>{t.maintenance.addService}</span>
        </button>
      </div>

      {/* KPI Cockpit Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {/* Total Cost */}
        <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-5 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400">
              {t.maintenance.serviceCost} {lang === 'fr' ? 'Cumulé' : 'Total'}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
              <DollarSign className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-1">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-slate-100">
              {totalCost.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
            <span className="text-sm font-bold text-amber-400 font-mono ml-1">{currencySymbol}</span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400 font-mono">
            {lang === 'fr' ? `Sur ${vehicleServices.length} interventions d'atelier` : `Across ${vehicleServices.length} maintenance records`}
          </p>
        </div>

        {/* Total Service Count */}
        <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-5 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400">
              {lang === 'fr' ? 'Services Réalisés' : 'Completed Services'}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-400">
              <CheckCircle2 className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-emerald-400">
              {vehicleServices.length}
            </span>
            <span className="text-xs font-mono text-slate-400 uppercase">
              {lang === 'fr' ? 'interventions' : 'entries'}
            </span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400 font-mono">
            {lang === 'fr' ? "Carnet d'entretien officiel à jour" : 'Logged in vehicle service booklet'}
          </p>
        </div>

        {/* Last Service Odometer */}
        <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-5 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400">
              {lang === 'fr' ? 'Dernière Révision' : 'Last Service'}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
              <Gauge className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-slate-100">
              {latestService ? `${latestService.odometer.toLocaleString()} ${distUnit}` : '—'}
            </span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400 font-mono">
            {latestService ? `${new Date(latestService.date).toLocaleDateString()} · ${t.serviceTypes[latestService.type] || latestService.type}` : (lang === 'fr' ? 'Aucune révision enregistrée' : 'No services logged')}
          </p>
        </div>
      </div>

      {/* Filter by Type & Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="relative w-full sm:w-80">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder={lang === 'fr' ? 'Rechercher description, pièces...' : 'Search description, parts, mileage...'}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-xl border border-slate-800 bg-[#0F1522] pl-10 pr-4 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm"
          />
        </div>

        {/* Service Type selector */}
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <select
            value={selectedType}
            onChange={(e) => setSelectedType(e.target.value)}
            className="w-full sm:w-auto rounded-xl border border-slate-800 bg-[#0F1522] px-3.5 py-2 text-sm font-mono font-semibold text-slate-200 shadow-sm focus:border-amber-500 focus:outline-none"
          >
            <option value="all" className="bg-slate-900">
              {lang === 'fr' ? 'Tous les types' : 'All Service Types'} ({vehicleServices.length})
            </option>
            {allServiceTypes.map((st) => (
              <option key={st} value={st} className="bg-slate-900">
                {t.serviceTypes[st] || st}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Timeline / Card List */}
      {filteredServices.length === 0 ? (
        <div className="rounded-2xl border-2 border-dashed border-slate-800 bg-[#0F1522]/50 p-12 text-center shadow-xl">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
            <Wrench className="h-7 w-7" />
          </div>
          <h2 className="mt-4 text-base font-bold font-mono text-slate-200">
            {vehicleServices.length === 0 
              ? (lang === 'fr' ? 'Aucune intervention enregistrée' : 'No maintenance records found') 
              : (lang === 'fr' ? 'Aucun résultat correspondant' : 'No records match your filter')}
          </h2>
          <p className="mt-1 text-xs text-slate-400 max-w-sm mx-auto">
            {vehicleServices.length === 0
              ? (lang === 'fr' ? 'Préservez la santé de votre moteur et sa valeur en enregistrant vos vidanges, disques, filtres et révisions.' : 'Keep your car healthy and preserve its resale value by tracking oil changes, brake pads, tire rotations, and scheduled inspections.')
              : (lang === 'fr' ? 'Essayez de réinitialiser la recherche ou le type sélectionné.' : 'Try clearing your search query or choosing another service type.')}
          </p>
          {vehicleServices.length === 0 && (
            <button
              onClick={handleOpenAdd}
              className="mt-5 inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2.5 text-sm font-bold text-slate-950 shadow-md hover:from-amber-400 hover:to-amber-500 transition"
            >
              <Plus className="h-4 w-4" />
              <span>{t.maintenance.addService}</span>
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {filteredServices.map((service) => {
            return (
              <div
                key={service.id}
                className="relative overflow-hidden rounded-2xl border border-slate-800 bg-[#0F1522] p-6 shadow-xl shadow-black/40 hover:border-slate-700 transition"
              >
                <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
                  {/* Left: Icon, Type, Description */}
                  <div className="flex items-start gap-4">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
                      <Wrench className="h-6 w-6" />
                    </div>
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="rounded-lg bg-amber-500/15 border border-amber-500/30 text-amber-400 px-2.5 py-0.5 text-xs font-mono font-bold">
                          {t.serviceTypes[service.type] || service.type}
                        </span>
                        <span className="text-xs text-slate-400 font-mono">
                          {new Date(service.date).toLocaleDateString()}
                        </span>
                      </div>

                      <h3 className="mt-1.5 text-base font-bold text-slate-100">
                        {service.description}
                      </h3>

                      {/* Mileage & Cost badges */}
                      <div className="mt-2.5 flex flex-wrap items-center gap-3 text-xs font-mono">
                        <div className="flex items-center gap-1.5 bg-slate-900 rounded-lg px-2.5 py-1 border border-slate-800 text-slate-300">
                          <Gauge className="h-3.5 w-3.5 text-cyan-400" />
                          <span>{service.odometer.toLocaleString()} {distUnit}</span>
                        </div>
                        <div className="flex items-center gap-1.5 bg-emerald-500/10 rounded-lg px-2.5 py-1 text-emerald-400 font-bold border border-emerald-500/30">
                          <DollarSign className="h-3.5 w-3.5 text-emerald-400" />
                          <span>{service.cost.toFixed(2)} {currencySymbol}</span>
                        </div>
                      </div>

                      {/* Replaced Parts Chips */}
                      {service.parts && service.parts.length > 0 && (
                        <div className="mt-3">
                          <span className="text-[11px] font-mono font-bold uppercase tracking-wider text-slate-400 block mb-1">
                            {lang === 'fr' ? 'Pièces Remplacées :' : 'Replaced Parts:'}
                          </span>
                          <div className="flex flex-wrap gap-1.5">
                            {service.parts.map((part, pIdx) => (
                              <span
                                key={pIdx}
                                className="inline-flex items-center gap-1 rounded-md bg-slate-900 border border-slate-800 px-2.5 py-1 text-xs font-mono text-slate-300"
                              >
                                <Package className="h-3 w-3 text-amber-400" />
                                <span>{part}</span>
                              </span>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Notes & Receipt */}
                      {service.notes && (
                        <p className="mt-2 text-xs text-slate-400 italic font-sans">
                          &quot;{service.notes}&quot;
                        </p>
                      )}

                      {service.receiptUrl && (
                        <div className="mt-2">
                          <a
                            href={service.receiptUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1 text-xs font-mono font-semibold text-cyan-400 hover:text-cyan-300 hover:underline"
                          >
                            <span>{lang === 'fr' ? 'Voir la facture' : 'View service receipt'}</span>
                            <ExternalLink className="h-3 w-3" />
                          </a>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Right Actions */}
                  <div className="flex sm:flex-col items-center sm:items-end justify-between sm:justify-start gap-2 border-t sm:border-t-0 border-slate-800 pt-3 sm:pt-0">
                    <span className="text-xl font-extrabold font-mono text-slate-100">
                      {service.cost.toFixed(2)} <span className="text-xs font-bold text-amber-400">{currencySymbol}</span>
                    </span>
                    <button
                      onClick={() => setDeleteConfirmId(service.id)}
                      className="rounded-lg p-2 text-slate-500 hover:bg-red-500/20 hover:text-red-400 transition"
                      title={t.common.delete}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Add Service Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-md overflow-y-auto">
          <div className="relative w-full max-w-lg rounded-2xl bg-[#0F1522] border border-slate-800 p-6 shadow-2xl shadow-black/80 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
                  <Wrench className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-slate-100 font-mono tracking-tight">{t.maintenance.addService}</h2>
                  <p className="text-xs text-slate-400">
                    {activeVehicle?.make} {activeVehicle?.model} {activeVehicle?.plate ? `(${activeVehicle.plate})` : ''}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setIsModalOpen(false)}
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSaveService} noValidate className="mt-5 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.maintenance.serviceType} *
                  </label>
                  <select
                    value={type}
                    onChange={(e) => handleServiceTypeSelect(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-semibold text-slate-100 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  >
                    {allServiceTypes.map((st) => (
                      <option key={st} value={st} className="bg-slate-900 text-slate-100">
                        {t.serviceTypes[st] || st}
                      </option>
                    ))}
                    <option value="__add_custom__" className="bg-slate-900 text-amber-400">
                      {lang === 'fr' ? '＋ Ajouter une catégorie…' : '＋ Add a category…'}
                    </option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.maintenance.serviceCost} ({currencySymbol}) *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="120.00"
                    value={cost}
                    onChange={(e) => setCost(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-mono font-bold text-amber-400 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                  {formErrors.cost && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.cost}</p>}
                </div>
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {t.maintenance.description} *
                </label>
                <input
                  type="text"
                  placeholder="e.g. Vidange complète 60.000 km, Huile Motul 5W30, filtre huile"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
                {formErrors.description && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.description}</p>}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.common.date} *
                  </label>
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-mono text-slate-100 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                  {formErrors.date && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.date}</p>}
                </div>

                <div>
                  <div className="flex items-center justify-between gap-1 mb-1.5">
                    <label
                      htmlFor="service-odometer-input"
                      className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider"
                    >
                      {t.common.odometer} ({distUnit}) *
                    </label>
                    {latestVehicleOdometer > 0 && (
                      <span className="text-[10px] font-mono text-slate-400">
                        Min: {latestVehicleOdometer.toLocaleString()} {distUnit}
                      </span>
                    )}
                  </div>
                  <input
                    id="service-odometer-input"
                    type="number"
                    placeholder={`>= ${latestVehicleOdometer}`}
                    value={odometer}
                    onChange={(e) => {
                      const nextVal = e.target.value === '' ? '' : Number(e.target.value);
                      setOdometer(nextVal);
                      const check = validateVehicleOdometer({
                        enteredOdometer: nextVal,
                        latestOdometer: latestVehicleOdometer,
                        distUnit,
                        lang,
                        requirePositive: false,
                      });
                      setFormErrors((prev) => {
                        const updated = { ...prev };
                        if (!check.valid && check.error) {
                          updated.odometer = check.error;
                        } else {
                          delete updated.odometer;
                        }
                        return updated;
                      });
                    }}
                    className={`w-full rounded-xl border bg-slate-900/90 px-3 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 ${
                      formErrors.odometer
                        ? 'border-red-500/80 focus:border-red-500 focus:ring-red-500/40'
                        : 'border-slate-700 focus:border-amber-500 focus:ring-amber-500'
                    }`}
                  />
                  {formErrors.odometer && (
                    <p data-testid="service-odometer-error" className="mt-1 text-xs text-red-400 font-mono">
                      {formErrors.odometer}
                    </p>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {t.maintenance.partsReplaced}
                </label>
                <input
                  type="text"
                  placeholder="e.g. Filtre à huile, Filtre à air, Plaquettes avant"
                  value={partsInput}
                  onChange={(e) => setPartsInput(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
                <p className="mt-1 text-[11px] text-slate-500 font-mono">
                  {lang === 'fr' ? 'Séparez les pièces par des virgules' : 'Separate multiple items with commas'}
                </p>
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {t.common.notes}
                </label>
                <input
                  type="text"
                  placeholder="Garage, notes mécanicien, référence pièces..."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {lang === 'fr' ? 'URL de facture / reçu' : 'Receipt or Invoice URL'}
                </label>
                <input
                  type="url"
                  placeholder="https://example.com/invoice.pdf"
                  value={receiptUrl}
                  onChange={(e) => setReceiptUrl(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-300 hover:bg-slate-800 transition"
                >
                  {t.common.cancel}
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2 text-sm font-bold text-slate-950 shadow-lg shadow-amber-500/20 hover:from-amber-400 hover:to-amber-500 disabled:opacity-50 transition"
                >
                  {submitting ? t.common.loading : t.common.save}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Custom Category Dialog */}
      {showCategoryDialog && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/80 p-4 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-2xl bg-[#0F1522] border border-slate-800 p-6 shadow-2xl shadow-black/80">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
                <Tag className="h-5 w-5" />
              </div>
              <h2 className="text-lg font-bold text-slate-100 font-mono">
                {lang === 'fr' ? 'Nouvelle catégorie' : 'New category'}
              </h2>
            </div>
            <p className="mt-3 text-sm text-slate-300">
              {lang === 'fr'
                ? 'Ajoutez un type de service ou de rappel personnalisé.'
                : 'Add a custom service or reminder type.'}
            </p>
            <input
              type="text"
              autoFocus
              value={newCategoryName}
              onChange={(e) => {
                setNewCategoryName(e.target.value);
                setCategoryError('');
              }}
              placeholder={lang === 'fr' ? 'ex. Assurance flotte' : 'e.g. Fleet insurance'}
              className="mt-4 w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
            />
            {categoryError && <p className="mt-1 text-xs text-red-400 font-mono">{categoryError}</p>}
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                onClick={() => setShowCategoryDialog(false)}
                className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-300 hover:bg-slate-800 transition"
              >
                {t.common.cancel}
              </button>
              <button
                onClick={handleAddCategory}
                className="rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2 text-sm font-bold text-slate-950 shadow-lg shadow-amber-500/20 hover:from-amber-400 hover:to-amber-500 transition"
              >
                {lang === 'fr' ? 'Ajouter' : 'Add'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      {deleteConfirmId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-2xl bg-[#0F1522] border border-slate-800 p-6 shadow-2xl shadow-black/80 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3 text-red-400">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-red-500/15 border border-red-500/30 text-red-400">
                <AlertTriangle className="h-5 w-5" />
              </div>
              <h2 className="text-lg font-bold text-slate-100 font-mono">{t.common.delete}</h2>
            </div>
            <p className="mt-3 text-sm text-slate-300 leading-relaxed">
              {t.common.confirmDelete}
            </p>
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                onClick={() => setDeleteConfirmId(null)}
                className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-300 hover:bg-slate-800 transition"
              >
                {t.common.cancel}
              </button>
              <button
                onClick={() => handleDelete(deleteConfirmId)}
                className="rounded-xl bg-gradient-to-r from-red-600 to-red-700 px-5 py-2 text-sm font-bold text-white shadow-lg shadow-red-600/25 hover:from-red-500 hover:to-red-600 transition"
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