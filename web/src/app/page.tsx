'use client';

import React from 'react';
import Link from 'next/link';
import { useApp } from '@/lib/app-context';
import {
  calculateAverageEconomy,
  calculateCostShares,
  calculateReminderProgress,
} from '@/lib/telemetry-math';
import { 
  Car, 
  Fuel, 
  Receipt, 
  Wrench, 
  Bell, 
  ArrowUpRight, 
  CheckCircle2, 
  Clock, 
  Plus, 
  TrendingDown, 
  Gauge, 
  Activity, 
  ShieldCheck, 
  Zap, 
  Sparkles,
  ChevronRight
} from 'lucide-react';

export default function DashboardPage() {
  const { 
    t, 
    lang, 
    userProfile, 
    vehicles, 
    activeVehicle, 
    setActiveVehicleId,
    setAddVehicleModalOpen,
    fillUps, 
    expenses, 
    services, 
    reminders,
    completeReminder
  } = useApp();

  const vehicleFillups = fillUps.filter(f => !activeVehicle || f.vehicleId === activeVehicle.id);
  const vehicleExpenses = expenses.filter(e => !activeVehicle || e.vehicleId === activeVehicle.id);
  const vehicleServices = services.filter(s => !activeVehicle || s.vehicleId === activeVehicle.id);
  const vehicleReminders = reminders.filter(r => !activeVehicle || r.vehicleId === activeVehicle.id);

  // Financial Calculations
  const totalFuelCost = vehicleFillups.reduce((acc, f) => acc + f.totalCost, 0);
  const totalExpenseCost = vehicleExpenses.reduce((acc, e) => acc + e.amount, 0);
  const totalServiceCost = vehicleServices.reduce((acc, s) => acc + s.cost, 0);

  const avgEconomy = calculateAverageEconomy(vehicleFillups, userProfile.units);

  const activeReminders = vehicleReminders.filter(r => r.isActive);
  const currencySymbol = userProfile.currency === 'DZD' ? 'DA ' : userProfile.currency === 'EUR' ? '€' : userProfile.currency === 'USD' ? '$' : userProfile.currency + ' ';
  const distUnit = userProfile.units === 'metric' ? 'km' : 'mi';
  const volumeUnit = userProfile.units === 'metric' ? 'L' : 'gal';
  const economyUnit = userProfile.units === 'metric' ? 'L/100km' : 'MPG';

  const { fuelShare, serviceShare, expenseShare, totalSpent } = calculateCostShares(
    totalFuelCost,
    totalServiceCost,
    totalExpenseCost
  );

  // Dynamic distance since previous fillup for the active vehicle
  const sortedVehFillups = [...vehicleFillups].sort((a, b) => b.odometer - a.odometer);
  const lastDistanceDelta =
    sortedVehFillups.length >= 2
      ? Math.max(0, sortedVehFillups[0].odometer - sortedVehFillups[1].odometer)
      : 0;

  // Dynamic next reminder summary for the active vehicle
  const primaryReminder = activeReminders[0];
  const nextReminderLabel = primaryReminder
    ? primaryReminder.nextDueOdometer && activeVehicle
      ? `${primaryReminder.title || primaryReminder.type} (${Math.max(
          0,
          primaryReminder.nextDueOdometer - activeVehicle.currentOdometer
        ).toLocaleString()} ${distUnit})`
      : `${primaryReminder.title || primaryReminder.type}`
    : lang === 'fr'
      ? 'Aucun rappel en attente'
      : 'No pending reminders';

  return (
    <div className="space-y-8">
      {/* Hero Vehicle Cockpit Card */}
      {activeVehicle ? (
        <div className="relative overflow-hidden rounded-3xl border border-slate-800/80 bg-gradient-to-br from-[#121826] via-[#0F1522] to-[#0A0E18] p-6 sm:p-8 shadow-lg">
          {/* Quick Vehicle Switcher Bar inside Cockpit Hero */}
          {vehicles.length > 0 && (
            <div className="relative z-10 mb-5 flex flex-wrap items-center gap-2 pb-4 border-b border-slate-800/80">
              <span className="text-[11px] font-mono font-bold uppercase tracking-wider text-slate-400 mr-1">
                {lang === 'fr' ? 'Véhicule actif :' : 'Active Vehicle:'}
              </span>
              {vehicles.map((v) => {
                const isSelected = v.id === activeVehicle.id;
                return (
                  <button
                    key={v.id}
                    type="button"
                    onClick={() => setActiveVehicleId(v.id)}
                    className={`inline-flex items-center gap-2 rounded-xl px-3.5 py-1.5 text-xs font-mono font-bold transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-slate-950 shadow-md shadow-amber-500/20'
                        : 'border border-slate-800 bg-slate-900/80 text-slate-300 hover:border-amber-500/40 hover:text-amber-300'
                    }`}
                  >
                    <Car className="h-3.5 w-3.5" />
                    <span>
                      {v.make} {v.model}
                    </span>
                    {v.plate && (
                      <span
                        className={`text-[10px] px-1.5 py-0.5 rounded ${
                          isSelected ? 'bg-slate-950/20 text-slate-950' : 'bg-slate-800 text-amber-400'
                        }`}
                      >
                        {v.plate}
                      </span>
                    )}
                  </button>
                );
              })}
              <button
                type="button"
                onClick={() => setAddVehicleModalOpen(true)}
                className="inline-flex items-center gap-1.5 rounded-xl border border-dashed border-amber-500/40 bg-amber-500/10 px-3 py-1.5 text-xs font-mono font-bold text-amber-400 hover:bg-amber-500/20 transition cursor-pointer"
              >
                <Plus className="h-3.5 w-3.5" />
                <span>{t.vehicles.addVehicle}</span>
              </button>
            </div>
          )}

          <div className="relative z-10 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
            <div className="space-y-3">
              {/* Telemetry Status Badges */}
              <div className="flex flex-wrap items-center gap-2">
                <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-0.5 text-xs font-bold text-emerald-400 backdrop-blur-md">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  <span>96% {lang === 'fr' ? 'Santé Véhicule' : 'Vehicle Health'}</span>
                </span>
                
                <span className="rounded-full border border-slate-700/80 bg-slate-800/80 px-3 py-0.5 text-xs font-semibold text-slate-300 uppercase tracking-wider backdrop-blur-md">
                  {t.fuelTypes[activeVehicle.fuelType]}
                </span>

                {activeVehicle.plate && (
                  <span className="rounded-full border border-amber-500/40 bg-amber-500/10 px-3 py-0.5 text-xs font-black text-amber-300 tracking-wider">
                    {activeVehicle.plate}
                  </span>
                )}
              </div>

              {/* Title & Specs */}
              <div>
                <h1 className="text-2xl sm:text-4xl font-black tracking-tight text-white flex items-center gap-3">
                  <span>{activeVehicle.make} {activeVehicle.model}</span>
                  <span className="text-base sm:text-xl font-bold text-slate-500">({activeVehicle.year})</span>
                </h1>
                {activeVehicle.name && (
                  <p className="text-xs font-bold uppercase tracking-widest text-amber-500/90 mt-0.5">
                    {activeVehicle.name}
                  </p>
                )}
              </div>

              {/* Odometer HUD Readout */}
              <div className="flex items-center gap-2 text-slate-300 text-sm font-semibold">
                <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
                  <Gauge className="h-4 w-4" />
                </div>
                <span>{t.dashboard.statsOdometer}:</span>
                <span className="font-mono text-base font-black text-cyan-300 tracking-wide">
                  {activeVehicle.currentOdometer.toLocaleString()} {distUnit}
                </span>
              </div>
            </div>

            {/* Tactile Cockpit Quick Action Buttons */}
            <div className="flex flex-wrap items-center gap-2.5">
              <Link
                href="/fuel"
                className="group flex items-center gap-2 rounded-2xl bg-gradient-to-r from-amber-500 via-orange-500 to-amber-600 px-4 py-3 text-sm font-black text-slate-950 shadow-lg shadow-amber-500/25 hover:shadow-amber-500/40 hover:scale-[1.02] active:scale-[0.98] transition-all"
              >
                <Fuel className="h-4 w-4 transition-transform group-hover:rotate-12" />
                <span>{t.dashboard.logFillup}</span>
              </Link>

              <Link
                href="/expenses"
                className="flex items-center gap-2 rounded-2xl border border-slate-700/80 bg-[#121826]/90 px-4 py-3 text-sm font-bold text-slate-200 hover:border-violet-500/50 hover:text-violet-300 hover:bg-violet-500/10 transition-all shadow-sm"
              >
                <Receipt className="h-4 w-4 text-violet-400" />
                <span>{t.dashboard.logExpense}</span>
              </Link>

              <Link
                href="/maintenance"
                className="flex items-center gap-2 rounded-2xl border border-slate-700/80 bg-[#121826]/90 px-4 py-3 text-sm font-bold text-slate-200 hover:border-emerald-500/50 hover:text-emerald-300 hover:bg-emerald-500/10 transition-all shadow-sm"
              >
                <Wrench className="h-4 w-4 text-emerald-400" />
                <span>{t.dashboard.logService}</span>
              </Link>
            </div>
          </div>
        </div>
      ) : (
        <div className="rounded-3xl border border-dashed border-slate-800 bg-[#121826]/60 p-8 text-center shadow-xl">
          <Car className="mx-auto h-12 w-12 text-slate-500" />
          <h2 className="mt-3 text-lg font-bold text-slate-200">{t.dashboard.addFirstVehicle}</h2>
          <Link
            href="/vehicles/new"
            className="mt-4 inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-600 px-5 py-2.5 text-sm font-black text-slate-950 shadow-md shadow-orange-500/25 hover:opacity-95 transition"
          >
            <Plus className="h-4 w-4" />
            <span>{t.vehicles.addVehicle}</span>
          </Link>
        </div>
      )}

      {/* 4 Telemetry HUD KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Fuel Economy Gauge */}
        <div className="relative overflow-hidden rounded-2xl border border-slate-800/80 bg-[#121826]/80 p-5 shadow-lg hover:border-slate-700 transition group">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-extrabold uppercase tracking-widest text-slate-400">
              {t.dashboard.statsFuelEconomy}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
              <Fuel className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="font-mono text-3xl font-black text-white">{avgEconomy}</span>
            <span className="text-xs font-bold text-slate-400">{economyUnit}</span>
          </div>
          <div className="mt-2.5 flex items-center justify-between text-xs">
            <span className="inline-flex items-center gap-1 text-emerald-400 font-bold">
              <TrendingDown className="h-3.5 w-3.5" />
              <span>{lang === 'fr' ? 'Mode Éco Optimal' : 'Optimal Eco'}</span>
            </span>
            <span className="text-[10px] font-mono text-slate-500">{vehicleFillups.length} {lang === 'fr' ? 'pleins' : 'fill-ups'}</span>
          </div>
          <div className="mt-2 h-1 w-full rounded-full bg-slate-800 overflow-hidden">
            <div className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-cyan-500" style={{ width: '85%' }} />
          </div>
        </div>

        {/* Monthly Cost */}
        <div className="relative overflow-hidden rounded-2xl border border-slate-800/80 bg-[#121826]/80 p-5 shadow-lg hover:border-slate-700 transition group">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-extrabold uppercase tracking-widest text-slate-400">
              {t.dashboard.statsMonthlyCost}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
              <Receipt className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-1">
            <span className="font-mono text-3xl font-black text-amber-400">
              {currencySymbol}{totalSpent.toLocaleString()}
            </span>
          </div>
          <p className="mt-2 text-xs text-slate-400 truncate">
            Carburant ({currencySymbol}{totalFuelCost.toFixed(0)}) · Entretien ({currencySymbol}{totalServiceCost.toFixed(0)})
          </p>
          <div className="mt-2 h-1 w-full rounded-full bg-slate-800 overflow-hidden">
            <div className="h-full rounded-full bg-gradient-to-r from-amber-500 to-orange-500" style={{ width: '70%' }} />
          </div>
        </div>

        {/* Due Reminders & Health */}
        <div className="relative overflow-hidden rounded-2xl border border-slate-800/80 bg-[#121826]/80 p-5 shadow-lg hover:border-slate-700 transition group">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-extrabold uppercase tracking-widest text-slate-400">
              {t.dashboard.statsDueReminders}
            </span>
            <div className={`flex h-9 w-9 items-center justify-center rounded-xl border ${
              activeReminders.length > 0 
                ? 'bg-red-500/15 border-red-500/30 text-red-400' 
                : 'bg-emerald-500/15 border-emerald-500/30 text-emerald-400'
            }`}>
              <Bell className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="font-mono text-3xl font-black text-white">{activeReminders.length}</span>
            <span className="text-xs font-bold text-slate-400">{t.reminders.dueSoon}</span>
          </div>
          <p className="mt-2 text-xs text-amber-400 font-semibold truncate flex items-center gap-1.5">
            <span className="h-1.5 w-1.5 rounded-full bg-amber-400 animate-pulse" />
            <span>{nextReminderLabel}</span>
          </p>
          <div className="mt-2 h-1 w-full rounded-full bg-slate-800 overflow-hidden">
            <div className="h-full rounded-full bg-gradient-to-r from-amber-500 to-red-500" style={{ width: '60%' }} />
          </div>
        </div>

        {/* Live Odometer */}
        <div className="relative overflow-hidden rounded-2xl border border-slate-800/80 bg-[#121826]/80 p-5 shadow-lg hover:border-slate-700 transition group">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-extrabold uppercase tracking-widest text-slate-400">
              {t.dashboard.statsOdometer}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-violet-500/15 border border-violet-500/30 text-violet-400">
              <Activity className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-1">
            <span className="font-mono text-3xl font-black text-white">
              {activeVehicle ? activeVehicle.currentOdometer.toLocaleString() : '0'}
            </span>
            <span className="text-xs font-bold text-slate-400">{distUnit}</span>
          </div>
          <p className="mt-2 text-xs text-slate-400">
            {lastDistanceDelta > 0
              ? `+${lastDistanceDelta.toLocaleString()} ${distUnit} ${lang === 'fr' ? 'depuis le dernier plein' : 'since last refuel'}`
              : lang === 'fr'
                ? `${vehicleFillups.length} plein(s) enregistré(s)`
                : `${vehicleFillups.length} fill-up(s) logged`}
          </p>
          <div className="mt-2 h-1 w-full rounded-full bg-slate-800 overflow-hidden">
            <div className="h-full rounded-full bg-gradient-to-r from-violet-500 to-cyan-500" style={{ width: '90%' }} />
          </div>
        </div>
      </div>

      {/* Two-Column Cockpit Telemetry Section */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Service Alerts & Radar */}
        <div className="rounded-3xl border border-slate-800/80 bg-[#121826]/80 p-6 shadow-xl backdrop-blur-xl">
          <div className="flex items-center justify-between pb-4 border-b border-slate-800">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
                <Bell className="h-4 w-4" />
              </div>
              <div>
                <h2 className="font-bold text-white text-base">{t.dashboard.upcomingServices}</h2>
                <p className="text-[11px] text-slate-400">
                  {lang === 'fr' ? 'Maintenance préventive programmée' : 'Scheduled preventative maintenance'}
                </p>
              </div>
            </div>
            <Link href="/reminders" className="text-xs font-bold text-amber-400 hover:text-amber-300 flex items-center gap-1">
              <span>{t.common.viewAll}</span>
              <ArrowUpRight className="h-3.5 w-3.5" />
            </Link>
          </div>

          <div className="mt-4 space-y-3.5">
            {activeReminders.length === 0 ? (
              <p className="py-8 text-center text-sm text-slate-500">{t.common.noData}</p>
            ) : (
              activeReminders.map((r) => {
                const { isOverdue, progressPercent } = calculateReminderProgress(
                  r,
                  activeVehicle?.currentOdometer || 0
                );
                return (
                  <div 
                    key={r.id} 
                    className="rounded-2xl border border-slate-800/80 bg-[#0B0F19]/80 p-4 hover:border-slate-700 transition"
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="font-bold text-slate-200 text-sm">{r.title || r.type}</h3>
                          {isOverdue ? (
                            <span className="rounded-full bg-red-500/20 border border-red-500/40 px-2 py-0.5 text-[10px] font-black text-red-400">
                              {t.reminders.overdue}
                            </span>
                          ) : (
                            <span className="rounded-full bg-amber-500/20 border border-amber-500/40 px-2 py-0.5 text-[10px] font-black text-amber-400">
                              {t.reminders.dueSoon}
                            </span>
                          )}
                        </div>
                        <p className="mt-1 text-xs text-slate-400">
                          {r.nextDueOdometer && (
                            <span>Échéance à <strong className="text-slate-200 font-mono">{r.nextDueOdometer.toLocaleString()} {distUnit}</strong></span>
                          )}
                          {r.nextDueDate && (
                            <span> · Date: {new Date(r.nextDueDate).toLocaleDateString()}</span>
                          )}
                        </p>
                      </div>

                      <button
                        onClick={() => completeReminder(r.id)}
                        className="rounded-xl border border-slate-700 bg-slate-800/80 p-2 text-slate-300 hover:border-emerald-500 hover:text-emerald-400 hover:bg-emerald-500/10 transition shadow-sm"
                        title={t.reminders.markDone}
                      >
                        <CheckCircle2 className="h-4 w-4" />
                      </button>
                    </div>

                    {/* Glowing Progress bar */}
                    <div className="mt-3">
                      <div className="flex justify-between text-[11px] font-mono text-slate-400 mb-1">
                        <span>{t.reminders.progress}</span>
                        <span className="font-bold text-amber-400">{progressPercent}%</span>
                      </div>
                      <div className="h-2 w-full overflow-hidden rounded-full bg-slate-800">
                        <div 
                          className="h-full rounded-full bg-gradient-to-r from-amber-500 via-orange-500 to-red-500 shadow-sm shadow-amber-500/50" 
                          style={{ width: `${progressPercent}%` }} 
                        />
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Cost Distribution & Telemetry */}
        <div className="rounded-3xl border border-slate-800/80 bg-[#121826]/80 p-6 shadow-xl backdrop-blur-xl">
          <div className="flex items-center justify-between pb-4 border-b border-slate-800">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-violet-500/15 border border-violet-500/30 text-violet-400">
                <Receipt className="h-4 w-4" />
              </div>
              <div>
                <h2 className="font-bold text-white text-base">{t.dashboard.spendingChart}</h2>
                <p className="text-[11px] text-slate-400">
                  {lang === 'fr' ? 'Répartition budgétaire totale' : 'Total cost breakdown'}
                </p>
              </div>
            </div>
            <Link href="/reports" className="text-xs font-bold text-amber-400 hover:text-amber-300 flex items-center gap-1">
              <span>{t.common.details}</span>
              <ArrowUpRight className="h-3.5 w-3.5" />
            </Link>
          </div>

          <div className="mt-5 space-y-4">
            {/* Telemetry Segmented Bar */}
            <div>
              <div className="flex h-4 w-full overflow-hidden rounded-xl bg-slate-900 border border-slate-800 p-0.5">
                <div style={{ width: `${fuelShare}%` }} className="bg-gradient-to-r from-amber-500 to-orange-500 rounded-l-lg shadow-sm" title={`Fuel (${fuelShare}%)`} />
                <div style={{ width: `${serviceShare}%` }} className="bg-gradient-to-r from-emerald-500 to-teal-500 shadow-sm" title={`Maintenance (${serviceShare}%)`} />
                <div style={{ width: `${expenseShare}%` }} className="bg-gradient-to-r from-violet-500 to-purple-500 rounded-r-lg shadow-sm" title={`Other Expenses (${expenseShare}%)`} />
              </div>
            </div>

            {/* 3 Metrics Cards */}
            <div className="grid grid-cols-3 gap-2 pt-2 text-center">
              <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-3">
                <span className="block text-[10px] font-extrabold uppercase tracking-widest text-amber-400">{t.reports.fuelShare}</span>
                <span className="font-mono text-base font-black text-amber-300">{currencySymbol}{totalFuelCost.toFixed(0)}</span>
                <span className="block text-[10px] font-mono text-slate-500">{fuelShare}%</span>
              </div>
              <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 p-3">
                <span className="block text-[10px] font-extrabold uppercase tracking-widest text-emerald-400">{t.reports.maintenanceShare}</span>
                <span className="font-mono text-base font-black text-emerald-300">{currencySymbol}{totalServiceCost.toFixed(0)}</span>
                <span className="block text-[10px] font-mono text-slate-500">{serviceShare}%</span>
              </div>
              <div className="rounded-2xl border border-violet-500/20 bg-violet-500/5 p-3">
                <span className="block text-[10px] font-extrabold uppercase tracking-widest text-violet-400">{t.reports.otherExpensesShare}</span>
                <span className="font-mono text-base font-black text-violet-300">{currencySymbol}{totalExpenseCost.toFixed(0)}</span>
                <span className="block text-[10px] font-mono text-slate-500">{expenseShare}%</span>
              </div>
            </div>

            {/* Recent Expenses List */}
            <div className="mt-4 pt-3 border-t border-slate-800 space-y-2">
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
                {t.dashboard.recentExpenses}
              </span>
              {vehicleExpenses.slice(0, 3).map((e) => (
                <div key={e.id} className="flex items-center justify-between text-xs py-1.5 border-b border-slate-800/40 last:border-0">
                  <span className="font-semibold text-slate-300">{e.description}</span>
                  <span className="font-mono font-bold text-amber-400">{currencySymbol}{e.amount.toFixed(2)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Recent Activity Table (Dark Glass) */}
      <div className="rounded-3xl border border-slate-800/80 bg-[#121826]/80 p-6 shadow-xl backdrop-blur-xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
              <Fuel className="h-4 w-4" />
            </div>
            <div>
              <h2 className="font-bold text-white text-base">{t.dashboard.recentFillups}</h2>
              <p className="text-[11px] text-slate-400">
                {lang === 'fr' ? 'Derniers ravitaillements en carburant' : 'Latest fuel refuel entries'}
              </p>
            </div>
          </div>
          <Link href="/fuel" className="text-xs font-bold text-amber-400 hover:text-amber-300 flex items-center gap-1">
            <span>{t.common.viewAll}</span>
            <ArrowUpRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        <div className="mt-4 overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-800 text-[11px] font-black uppercase tracking-wider text-slate-500">
                <th className="pb-3 px-3">{t.common.date}</th>
                <th className="pb-3 px-3">{t.common.odometer}</th>
                <th className="pb-3 px-3">{t.fuel.volume}</th>
                <th className="pb-3 px-3">{t.fuel.pricePerUnit}</th>
                <th className="pb-3 px-3">{t.fuel.totalCost}</th>
                <th className="pb-3 px-3">{t.fuel.station}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-medium">
              {vehicleFillups.slice(0, 5).map((f) => (
                <tr key={f.id} className="hover:bg-slate-800/30 transition">
                  <td className="py-3 px-3 font-semibold text-slate-200">{new Date(f.date).toLocaleDateString()}</td>
                  <td className="py-3 px-3 font-mono font-bold text-cyan-300">{f.odometer.toLocaleString()} {distUnit}</td>
                  <td className="py-3 px-3 text-slate-300 font-mono">{f.volume} {volumeUnit}</td>
                  <td className="py-3 px-3 text-slate-400 font-mono">{currencySymbol}{f.pricePerUnit}</td>
                  <td className="py-3 px-3 font-mono font-black text-amber-400">{currencySymbol}{f.totalCost.toFixed(2)}</td>
                  <td className="py-3 px-3">
                    <span className="inline-flex items-center gap-1 rounded-md bg-slate-800/90 border border-slate-700/80 px-2 py-0.5 text-xs font-bold text-slate-300">
                      {f.station || 'Naftal'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
