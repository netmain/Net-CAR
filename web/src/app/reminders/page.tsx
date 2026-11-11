'use client';

import React, { useState } from 'react';
import { useApp } from '@/lib/app-context';
import { Reminder } from '@/types';
import { 
  Bell, 
  Plus, 
  CheckCircle2, 
  RotateCcw, 
  Trash2, 
  Calendar, 
  Gauge, 
  Clock, 
  AlertTriangle, 
  Check, 
  X, 
  Repeat, 
  AlertCircle,
  Filter,
  Pencil,
  Tag
} from 'lucide-react';

export default function RemindersPage() {
  const { 
    vehicles, 
    activeVehicle, 
    reminders, 
    addReminder, 
    updateReminder,
    completeReminder, 
    deleteReminder, 
    addCustomServiceType,
    userProfile, 
    lang,
    t 
  } = useApp();

  const [activeTab, setActiveTab] = useState<'active' | 'completed'>('active');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingReminderId, setEditingReminderId] = useState<string | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);
  const [showCategoryDialog, setShowCategoryDialog] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [categoryError, setCategoryError] = useState('');

  // Form State
  const [type, setType] = useState<string>('oilChange');
  const [title, setTitle] = useState('');
  const [intervalKm, setIntervalKm] = useState<number | ''>(15000);
  const [intervalDays, setIntervalDays] = useState<number | ''>(365);
  const [dueDate, setDueDate] = useState('');
  const [dueOdometer, setDueOdometer] = useState<number | ''>('');
  const [isRecurring, setIsRecurring] = useState(true);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const distUnit = userProfile.units === 'metric' ? 'km' : 'mi';

  // Filter reminders by active vehicle
  const vehicleReminders = reminders.filter(r => !activeVehicle || r.vehicleId === activeVehicle.id);

  const displayedReminders = vehicleReminders.filter(r => {
    if (activeTab === 'active') return r.isActive;
    return !r.isActive;
  });

  const activeCount = vehicleReminders.filter(r => r.isActive).length;
  const completedCount = vehicleReminders.filter(r => !r.isActive).length;

  const currentOdo = activeVehicle?.currentOdometer || 0;

  // Calculate status and progress for a reminder
  const getReminderStatus = (r: Reminder) => {
    let isOverdue = false;
    let isDueSoon = false;
    let progressPercent = 50;
    let remainingDist: number | null = null;
    let remainingDays: number | null = null;

    if (r.nextDueOdometer) {
      remainingDist = r.nextDueOdometer - currentOdo;
      if (remainingDist <= 0) {
        isOverdue = true;
        progressPercent = 100;
      } else if (remainingDist <= (userProfile.units === 'metric' ? 1500 : 1000)) {
        isDueSoon = true;
      }

      const base = r.lastServiceOdometer || (r.intervalKm ? r.nextDueOdometer - r.intervalKm : currentOdo - 5000);
      const totalInterval = r.nextDueOdometer - base;
      if (totalInterval > 0) {
        const elapsed = currentOdo - base;
        progressPercent = Math.min(100, Math.max(5, Math.round((elapsed / totalInterval) * 100)));
      }
    }

    if (r.nextDueDate) {
      const now = new Date().getTime();
      const dueTime = new Date(r.nextDueDate).getTime();
      const diffMs = dueTime - now;
      remainingDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

      if (diffMs <= 0) {
        isOverdue = true;
        progressPercent = 100;
      } else if (remainingDays <= 30) {
        isDueSoon = true;
        if (!r.nextDueOdometer) progressPercent = 85;
      } else if (!r.nextDueOdometer) {
        progressPercent = 40;
      }
    }

    return {
      isOverdue,
      isDueSoon,
      progressPercent,
      remainingDist,
      remainingDays,
    };
  };

  const handleOpenAdd = () => {
    setEditingReminderId(null);
    setType('oilChange');
    setTitle('');
    setIntervalKm(15000);
    setIntervalDays(365);
    setDueDate('');
    setDueOdometer(activeVehicle ? activeVehicle.currentOdometer + 15000 : '');
    setIsRecurring(true);
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleOpenEdit = (r: Reminder) => {
    setEditingReminderId(r.id);
    setType(r.type || 'other');
    setTitle(r.title || '');
    setIntervalKm(r.intervalKm !== undefined ? Number(r.intervalKm) : '');
    setIntervalDays(r.intervalDays !== undefined ? Number(r.intervalDays) : '');
    setDueDate(r.nextDueDate ? new Date(r.nextDueDate).toISOString().split('T')[0] : '');
    setDueOdometer(r.nextDueOdometer !== undefined ? Number(r.nextDueOdometer) : '');
    setIsRecurring(Boolean(r.isRecurring));
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleTypeChange = (selected: string) => {
    setType(selected);
    // Provide sensible defaults based on service type
    if (selected === 'oilChange') {
      setTitle(t.serviceTypes.oilChange);
      setIntervalKm(15000);
      setIntervalDays(365);
      if (activeVehicle) setDueOdometer(activeVehicle.currentOdometer + 15000);
    } else if (selected === 'tireRotation') {
      setTitle(t.serviceTypes.tireRotation);
      setIntervalKm(10000);
      setIntervalDays(180);
      if (activeVehicle) setDueOdometer(activeVehicle.currentOdometer + 10000);
    } else if (selected === 'inspection') {
      setTitle(t.serviceTypes.inspection);
      setIntervalDays(730);
      setIntervalKm('');
      const inTwoYears = new Date();
      inTwoYears.setFullYear(inTwoYears.getFullYear() + 2);
      setDueDate(inTwoYears.toISOString().split('T')[0]);
    } else {
      setTitle(t.serviceTypes[selected] || selected);
    }
  };

  const handleServiceTypeSelect = (selected: string) => {
    if (selected === '__add_custom__') {
      setNewCategoryName('');
      setCategoryError('');
      setShowCategoryDialog(true);
      return;
    }
    handleTypeChange(selected);
  };

  const handleAddCategory = () => {
    const clean = newCategoryName.trim().replace(/\s+/g, ' ');
    if (!clean) {
      setCategoryError(lang === 'fr' ? 'Saisissez un nom de catégorie' : 'Enter a category name');
      return;
    }
    addCustomServiceType(clean);
    setType(clean);
    if (!title.trim()) setTitle(clean);
    setShowCategoryDialog(false);
    setNewCategoryName('');
    setCategoryError('');
  };

  const handleSaveReminder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeVehicle) return;

    const errors: Record<string, string> = {};
    if (!title.trim()) errors.title = 'Title is required';
    if (!dueDate && (dueOdometer === '' || Number(dueOdometer) <= 0)) {
      errors.due = 'Provide either a due date or due odometer reading';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        type,
        title: title.trim(),
        intervalKm: intervalKm !== '' ? Number(intervalKm) : undefined,
        intervalDays: intervalDays !== '' ? Number(intervalDays) : undefined,
        nextDueDate: dueDate ? new Date(dueDate).toISOString() : undefined,
        nextDueOdometer: dueOdometer !== '' ? Number(dueOdometer) : undefined,
        isRecurring,
        isActive: true,
      };

      if (editingReminderId) {
        await updateReminder(editingReminderId, payload);
      } else {
        await addReminder({
          vehicleId: activeVehicle.id,
          ...payload,
          lastServiceDate: new Date().toISOString(),
          lastServiceOdometer: activeVehicle.currentOdometer,
        });
      }

      setIsModalOpen(false);
      setEditingReminderId(null);
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    await deleteReminder(id);
    setDeleteConfirmId(null);
  };

  const allServiceTypes = Array.from(
    new Set([
      ...Object.keys(t.serviceTypes),
      ...(userProfile.customServiceTypes || []),
      ...vehicleReminders.map((r) => r.type).filter(Boolean),
    ])
  );

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white">
              {t.reminders.title}
            </h1>
            {activeVehicle && (
              <span className="rounded-xl border border-amber-500/40 bg-amber-500/10 text-amber-300 text-xs font-bold px-3 py-1">
                {activeVehicle.make} {activeVehicle.model} {activeVehicle.plate ? `(${activeVehicle.plate})` : ''}
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-slate-400">
            {t.reminders.subtitle}
          </p>
        </div>

        <button
          onClick={handleOpenAdd}
          disabled={!activeVehicle}
          className="inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-600 px-5 py-2.5 text-sm font-black text-slate-950 shadow-lg shadow-orange-500/20 hover:opacity-95 disabled:opacity-50 transition"
        >
          <Plus className="h-4 w-4" />
          <span>{t.reminders.addReminder}</span>
        </button>
      </div>

      {/* Tabs */}
      <div className="flex items-center justify-between border-b border-slate-800">
        <div className="flex gap-2">
          <button
            onClick={() => setActiveTab('active')}
            className={`flex items-center gap-2 pb-3 px-3 text-sm font-black border-b-2 transition ${
              activeTab === 'active'
                ? 'border-amber-500 text-amber-400'
                : 'border-transparent text-slate-500 hover:text-slate-300'
            }`}
          >
            <span>Rappels Actifs</span>
            <span className={`rounded-full px-2 py-0.5 text-xs font-mono font-bold ${
              activeTab === 'active' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' : 'bg-slate-800 text-slate-500'
            }`}>
              {activeCount}
            </span>
          </button>

          <button
            onClick={() => setActiveTab('completed')}
            className={`flex items-center gap-2 pb-3 px-3 text-sm font-black border-b-2 transition ${
              activeTab === 'completed'
                ? 'border-amber-500 text-amber-400'
                : 'border-transparent text-slate-500 hover:text-slate-300'
            }`}
          >
            <span>Historique / Terminés</span>
            <span className={`rounded-full px-2 py-0.5 text-xs font-mono font-bold ${
              activeTab === 'completed' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' : 'bg-slate-800 text-slate-500'
            }`}>
              {completedCount}
            </span>
          </button>
        </div>

        <div className="hidden sm:block text-xs font-mono text-slate-400">
          Odomètre: <strong className="text-cyan-300 font-black">{currentOdo.toLocaleString()} {distUnit}</strong>
        </div>
      </div>

      {/* Reminders List */}
      {displayedReminders.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-slate-800 bg-[#121826]/60 p-12 text-center shadow-xl">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
            <Bell className="h-7 w-7" />
          </div>
          <h2 className="mt-4 text-base font-bold text-white">
            {activeTab === 'active' ? 'Aucun rappel actif programmé' : 'Aucun rappel terminé'}
          </h2>
          <p className="mt-1 text-xs text-slate-400 max-w-sm mx-auto">
            {activeTab === 'active'
              ? 'Programmez vos alertes de vidange, plaquettes de frein ou filtres pour éviter les mauvaises surprises.'
              : 'Les alertes marquées comme faites apparaîtront ici pour historique.'}
          </p>
          {activeTab === 'active' && (
            <button
              onClick={handleOpenAdd}
              className="mt-5 inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-600 px-5 py-2.5 text-sm font-black text-slate-950 shadow-md shadow-orange-500/20 hover:opacity-95 transition"
            >
              <Plus className="h-4 w-4" />
              <span>{t.reminders.addReminder}</span>
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {displayedReminders.map((r) => {
            const status = getReminderStatus(r);

            return (
              <div
                key={r.id}
                className={`flex flex-col justify-between rounded-3xl border bg-[#121826]/80 p-6 shadow-xl backdrop-blur-xl transition hover:shadow-2xl ${
                  status.isOverdue
                    ? 'border-red-500/60 ring-1 ring-red-500/30 bg-red-950/10'
                    : status.isDueSoon
                    ? 'border-amber-500/60 ring-1 ring-amber-500/30 bg-amber-950/10'
                    : 'border-slate-800/80 hover:border-slate-700'
                }`}
              >
                <div>
                  {/* Top line: status badge & actions */}
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex flex-wrap items-center gap-2">
                      {status.isOverdue ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-red-500/20 border border-red-500/40 px-2.5 py-1 text-xs font-black text-red-400">
                          <AlertTriangle className="h-3.5 w-3.5" />
                          <span>{t.reminders.overdue}</span>
                        </span>
                      ) : status.isDueSoon ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/20 border border-amber-500/40 px-2.5 py-1 text-xs font-black text-amber-400">
                          <Clock className="h-3.5 w-3.5" />
                          <span>{t.reminders.dueSoon}</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/20 border border-emerald-500/40 px-2.5 py-1 text-xs font-black text-emerald-400">
                          <CheckCircle2 className="h-3.5 w-3.5" />
                          <span>{t.reminders.ok}</span>
                        </span>
                      )}

                      {r.isRecurring && (
                        <span className="inline-flex items-center gap-1 rounded-md bg-slate-800 border border-slate-700 px-2 py-0.5 text-[10px] font-bold text-slate-400 uppercase tracking-wider" title="Rappel récurrent">
                          <Repeat className="h-3 w-3" />
                          <span>Récurrent</span>
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-1">
                      {r.isActive && (
                        <button
                          onClick={() => completeReminder(r.id)}
                          className="flex items-center gap-1 rounded-xl border border-emerald-500/40 bg-emerald-500/15 px-3 py-1.5 text-xs font-bold text-emerald-300 hover:bg-emerald-500/30 transition shadow-sm"
                          title={t.reminders.markDone}
                        >
                          <Check className="h-3.5 w-3.5" />
                          <span>Fait</span>
                        </button>
                      )}
                      <button
                        onClick={() => handleOpenEdit(r)}
                        className="rounded-xl p-1.5 text-slate-500 hover:bg-amber-500/10 hover:text-amber-400 transition"
                        title={t.reminders.editReminder}
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => setDeleteConfirmId(r.id)}
                        className="rounded-xl p-1.5 text-slate-500 hover:bg-red-500/10 hover:text-red-400 transition"
                        title={t.common.delete}
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>

                  {/* Title & Service Type */}
                  <h3 className="mt-3.5 text-lg font-black text-white">
                    {r.title || t.serviceTypes[r.type] || r.type}
                  </h3>

                  {/* Due Targets Info */}
                  <div className="mt-2.5 space-y-1.5 text-xs text-slate-400 font-medium">
                    {r.nextDueOdometer && (
                      <div className="flex items-center gap-2">
                        <Gauge className="h-4 w-4 text-cyan-400" />
                        <span>
                          Échéance: <strong className="font-mono text-cyan-300 font-bold">{r.nextDueOdometer.toLocaleString()} {distUnit}</strong>
                          {status.remainingDist !== null && (
                            status.remainingDist <= 0 ? (
                              <span className="text-red-400 font-mono font-bold ml-1.5">
                                ({Math.abs(status.remainingDist).toLocaleString()} {distUnit} en retard)
                              </span>
                            ) : (
                              <span className="text-slate-400 font-mono ml-1.5">
                                (dans ~{status.remainingDist.toLocaleString()} {distUnit})
                              </span>
                            )
                          )}
                        </span>
                      </div>
                    )}

                    {r.nextDueDate && (
                      <div className="flex items-center gap-2">
                        <Calendar className="h-4 w-4 text-amber-400" />
                        <span>
                          Date limite: <strong className="text-slate-200 font-semibold">{new Date(r.nextDueDate).toLocaleDateString()}</strong>
                          {status.remainingDays !== null && (
                            status.remainingDays <= 0 ? (
                              <span className="text-red-400 font-mono font-bold ml-1.5">
                                ({Math.abs(status.remainingDays)} jours en retard)
                              </span>
                            ) : (
                              <span className="text-slate-400 font-mono ml-1.5">
                                ({status.remainingDays} jours restants)
                              </span>
                            )
                          )}
                        </span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Neon Progress Bar */}
                <div className="mt-5 pt-3.5 border-t border-slate-800/80">
                  <div className="flex items-center justify-between text-[11px] font-mono text-slate-400 mb-1.5">
                    <span>{t.reminders.progress}</span>
                    <span className={`font-bold ${status.isOverdue ? 'text-red-400' : status.isDueSoon ? 'text-amber-400' : 'text-emerald-400'}`}>
                      {status.progressPercent}%
                    </span>
                  </div>
                  <div className="h-2.5 w-full overflow-hidden rounded-full bg-slate-900 border border-slate-800">
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${
                        status.isOverdue 
                          ? 'bg-gradient-to-r from-orange-500 to-red-500 shadow-sm shadow-red-500/50' 
                          : status.isDueSoon 
                          ? 'bg-gradient-to-r from-amber-500 to-orange-500 shadow-sm shadow-amber-500/50' 
                          : 'bg-gradient-to-r from-teal-500 to-emerald-500 shadow-sm shadow-emerald-500/50'
                      }`}
                      style={{ width: `${Math.min(100, status.progressPercent)}%` }}
                    />
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Add Reminder Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-md overflow-y-auto">
          <div className="relative w-full max-w-lg rounded-2xl bg-[#0F1522] border border-slate-800 p-6 shadow-2xl shadow-black/80 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
                  <Bell className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-slate-100 font-mono tracking-tight">
                    {editingReminderId ? t.reminders.editReminder : t.reminders.addReminder}
                  </h2>
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

            <form onSubmit={handleSaveReminder} className="mt-5 space-y-4">
              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {t.reminders.reminderType} *
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
                  {lang === 'fr' ? 'Titre du rappel *' : 'Reminder Title *'}
                </label>
                <input
                  type="text"
                  placeholder="e.g. Engine Oil & Filter Change"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
                {formErrors.title && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.title}</p>}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.reminders.dueOdometer} ({distUnit})
                  </label>
                  <input
                    type="number"
                    placeholder="e.g. 75000"
                    value={dueOdometer}
                    onChange={(e) => setDueOdometer(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.reminders.dueDate}
                  </label>
                  <input
                    type="date"
                    value={dueDate}
                    onChange={(e) => setDueDate(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-mono text-slate-100 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                </div>
              </div>

              {formErrors.due && <p className="text-xs text-red-400 font-mono">{formErrors.due}</p>}

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.reminders.intervalKm} ({distUnit})
                  </label>
                  <input
                    type="number"
                    placeholder="15000"
                    value={intervalKm}
                    onChange={(e) => setIntervalKm(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.reminders.intervalDays}
                  </label>
                  <input
                    type="number"
                    placeholder="365"
                    value={intervalDays}
                    onChange={(e) => setIntervalDays(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                </div>
              </div>

              {/* Recurring Switch */}
              <div className="flex items-center gap-3 rounded-xl bg-slate-900/60 p-3.5 border border-slate-800">
                <input
                  type="checkbox"
                  id="recurring-reminder"
                  checked={isRecurring}
                  onChange={(e) => setIsRecurring(e.target.checked)}
                  className="h-4 w-4 rounded border-slate-700 bg-slate-800 text-amber-500 focus:ring-amber-500 focus:ring-offset-slate-900"
                />
                <label htmlFor="recurring-reminder" className="cursor-pointer select-none text-xs">
                  <span className="font-bold text-slate-200 block">{t.reminders.isRecurring}</span>
                  <span className="text-slate-400 text-[11px]">
                    {lang === 'fr' 
                      ? 'Reprogrammer automatiquement le prochain rappel une fois marqué terminé'
                      : 'Automatically schedule the next reminder once marked as completed'}
                  </span>
                </label>
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
                  {submitting
                    ? t.common.loading
                    : editingReminderId
                      ? t.reminders.saveChanges
                      : t.common.save}
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
