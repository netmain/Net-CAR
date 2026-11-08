'use client';

import React, { useState } from 'react';
import { useApp } from '@/lib/app-context';
import { 
  Download, 
  Printer, 
  DollarSign, 
  Fuel, 
  Wrench, 
  TrendingUp, 
  Calendar,
  PieChart
} from 'lucide-react';

export default function ReportsPage() {
  const { 
    vehicles, 
    activeVehicle, 
    fillUps, 
    expenses, 
    services, 
    userProfile, 
    lang,
    t 
  } = useApp();

  const [selectedVehicleId, setSelectedVehicleId] = useState<string>(activeVehicle?.id || 'all');

  const currencySymbol = userProfile.currency === 'EUR' ? '€' : userProfile.currency === 'USD' ? '$' : userProfile.currency;
  const distUnit = userProfile.units === 'metric' ? 'km' : 'mi';

  // Filter datasets
  const filteredFillups = fillUps.filter(f => selectedVehicleId === 'all' || f.vehicleId === selectedVehicleId);
  const filteredExpenses = expenses.filter(e => selectedVehicleId === 'all' || e.vehicleId === selectedVehicleId);
  const filteredServices = services.filter(s => selectedVehicleId === 'all' || s.vehicleId === selectedVehicleId);

  // Total Calculations
  const fuelTotal = filteredFillups.reduce((acc, f) => acc + f.totalCost, 0);
  const expenseTotal = filteredExpenses.reduce((acc, e) => acc + e.amount, 0);
  const serviceTotal = filteredServices.reduce((acc, s) => acc + s.cost, 0);
  const grandTotal = fuelTotal + expenseTotal + serviceTotal;

  // Percentage Shares
  const fuelShare = grandTotal > 0 ? Math.round((fuelTotal / grandTotal) * 100) : 0;
  const serviceShare = grandTotal > 0 ? Math.round((serviceTotal / grandTotal) * 100) : 0;
  const expenseShare = grandTotal > 0 ? Math.max(0, 100 - fuelShare - serviceShare) : 0;

  // Mileage and Cost per km
  let totalDistanceTracked = 0;
  if (filteredFillups.length >= 2) {
    const sorted = [...filteredFillups].sort((a, b) => a.odometer - b.odometer);
    totalDistanceTracked = sorted[sorted.length - 1].odometer - sorted[0].odometer;
  }
  const costPerKm = totalDistanceTracked > 0 ? (grandTotal / totalDistanceTracked).toFixed(2) : null;

  // Monthly Aggregation
  interface MonthlyRow {
    monthKey: string;
    label: string;
    fuel: number;
    maintenance: number;
    expenses: number;
    total: number;
  }

  const monthlyMap = new Map<string, MonthlyRow>();

  const getMonthKey = (isoDate: string) => {
    const d = new Date(isoDate);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    return `${y}-${m}`;
  };

  const getMonthLabel = (isoDate: string) => {
    const d = new Date(isoDate);
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short' });
  };

  filteredFillups.forEach(f => {
    const k = getMonthKey(f.date);
    const existing = monthlyMap.get(k) || { monthKey: k, label: getMonthLabel(f.date), fuel: 0, maintenance: 0, expenses: 0, total: 0 };
    existing.fuel += f.totalCost;
    existing.total += f.totalCost;
    monthlyMap.set(k, existing);
  });

  filteredExpenses.forEach(e => {
    const k = getMonthKey(e.date);
    const existing = monthlyMap.get(k) || { monthKey: k, label: getMonthLabel(e.date), fuel: 0, maintenance: 0, expenses: 0, total: 0 };
    existing.expenses += e.amount;
    existing.total += e.amount;
    monthlyMap.set(k, existing);
  });

  filteredServices.forEach(s => {
    const k = getMonthKey(s.date);
    const existing = monthlyMap.get(k) || { monthKey: k, label: getMonthLabel(s.date), fuel: 0, maintenance: 0, expenses: 0, total: 0 };
    existing.maintenance += s.cost;
    existing.total += s.cost;
    monthlyMap.set(k, existing);
  });

  const monthlyRows = Array.from(monthlyMap.values()).sort((a, b) => b.monthKey.localeCompare(a.monthKey));

  // Export CSV Handler
  const handleExportCSV = () => {
    const rows = [
      ['Date', 'Vehicle', 'Type', 'Category / Detail', 'Amount', 'Odometer', 'Notes'].join(',')
    ];

    filteredFillups.forEach(f => {
      const v = vehicles.find(veh => veh.id === f.vehicleId);
      rows.push([
        new Date(f.date).toLocaleDateString(),
        `"${v ? `${v.make} ${v.model}` : f.vehicleId}"`,
        'Fuel',
        `"${f.fuelType} (${f.volume}L @ ${f.pricePerUnit})"`,
        f.totalCost.toFixed(2),
        f.odometer,
        `"${(f.station || '') + (f.notes ? ` - ${f.notes}` : '')}"`
      ].join(','));
    });

    filteredExpenses.forEach(e => {
      const v = vehicles.find(veh => veh.id === e.vehicleId);
      rows.push([
        new Date(e.date).toLocaleDateString(),
        `"${v ? `${v.make} ${v.model}` : e.vehicleId}"`,
        'Expense',
        `"${e.category} - ${e.description.replace(/"/g, '""')}"`,
        e.amount.toFixed(2),
        e.odometer || '',
        `"${(e.notes || '').replace(/"/g, '""')}"`
      ].join(','));
    });

    filteredServices.forEach(s => {
      const v = vehicles.find(veh => veh.id === s.vehicleId);
      rows.push([
        new Date(s.date).toLocaleDateString(),
        `"${v ? `${v.make} ${v.model}` : s.vehicleId}"`,
        'Maintenance',
        `"${s.type} - ${s.description.replace(/"/g, '""')}"`,
        s.cost.toFixed(2),
        s.odometer,
        `"${(s.parts?.join(', ') || '') + (s.notes ? ` - ${s.notes}` : '')}"`
      ].join(','));
    });

    const csvContent = 'data:text/csv;charset=utf-8,' + rows.join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `NetCAR_Report_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handlePrint = () => {
    if (typeof window !== 'undefined') {
      window.print();
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-300">
      {/* Header and Print/Export Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-100 font-mono">
            {t.reports.title}
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            {t.reports.subtitle}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {/* Vehicle Filter Selector */}
          <select
            value={selectedVehicleId}
            onChange={(e) => setSelectedVehicleId(e.target.value)}
            className="rounded-xl border border-slate-800 bg-[#0F1522] px-3.5 py-2 text-sm font-mono font-semibold text-slate-200 shadow-sm focus:border-amber-500 focus:outline-none"
          >
            <option value="all" className="bg-slate-900">
              {lang === 'fr' ? 'Tous les véhicules (Flotte)' : 'All Vehicles (Fleet)'}
            </option>
            {vehicles.map((v) => (
              <option key={v.id} value={v.id} className="bg-slate-900">
                {v.make} {v.model} {v.plate ? `(${v.plate})` : ''}
              </option>
            ))}
          </select>

          {/* Export CSV Button */}
          <button
            onClick={handleExportCSV}
            className="inline-flex items-center gap-1.5 rounded-xl border border-slate-800 bg-[#0F1522] px-4 py-2 text-sm font-mono font-bold text-slate-300 shadow-sm hover:bg-slate-800/70 hover:text-white transition"
          >
            <Download className="h-4 w-4 text-cyan-400" />
            <span>{t.common.export}</span>
          </button>

          {/* Print Button */}
          <button
            onClick={handlePrint}
            className="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-4 py-2 text-sm font-mono font-bold text-slate-950 shadow-lg shadow-amber-500/20 hover:from-amber-400 hover:to-amber-500 transition"
          >
            <Printer className="h-4 w-4" />
            <span>{lang === 'fr' ? 'Imprimer le rapport' : 'Print Report'}</span>
          </button>
        </div>
      </div>

      {/* KPI Overview Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Total Cost of Ownership */}
        <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-5 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400">
              {t.reports.totalSpent}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
              <DollarSign className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-1">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-slate-100">
              {grandTotal.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
            <span className="text-sm font-bold text-amber-400 font-mono ml-1">{currencySymbol}</span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400 font-mono">
            {lang === 'fr' ? 'Toutes dépenses cumulées' : 'All categories combined'}
          </p>
        </div>

        {/* Cost per km */}
        <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-5 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400">
              {t.fuel.costPerKm}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-400">
              <TrendingUp className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-1">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-emerald-400">
              {costPerKm ? `${costPerKm}` : '—'}
            </span>
            {costPerKm && <span className="text-xs font-mono font-bold text-slate-400"> {currencySymbol}/{distUnit}</span>}
          </div>
          <p className="mt-1.5 text-xs text-slate-400 font-mono">
            {totalDistanceTracked > 0 ? (lang === 'fr' ? `Sur ${totalDistanceTracked.toLocaleString()} ${distUnit}` : `Across ${totalDistanceTracked.toLocaleString()} ${distUnit}`) : (lang === 'fr' ? 'Nécessite au moins 2 pleins' : 'Need multiple fill-ups')}
          </p>
        </div>

        {/* Fuel Share */}
        <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-5 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400">
              {t.reports.fuelShare}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
              <Fuel className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-cyan-400">{fuelShare}%</span>
            <span className="text-xs font-mono font-semibold text-slate-400">({fuelTotal.toFixed(0)} {currencySymbol})</span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400 font-mono">
            {filteredFillups.length} {lang === 'fr' ? 'pleins enregistrés' : 'refills logged'}
          </p>
        </div>

        {/* Maintenance & Services */}
        <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-5 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400">
              {t.reports.maintenanceShare}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-purple-500/15 border border-purple-500/30 text-purple-400">
              <Wrench className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-purple-400">{serviceShare}%</span>
            <span className="text-xs font-mono font-semibold text-slate-400">({serviceTotal.toFixed(0)} {currencySymbol})</span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400 font-mono">
            {filteredServices.length} {lang === 'fr' ? 'services effectués' : 'maintenance records'}
          </p>
        </div>
      </div>

      {/* Spending Distribution Visual Card */}
      <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-6 sm:p-8 shadow-xl shadow-black/40">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
              <PieChart className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100 font-mono tracking-tight">
                {lang === 'fr' ? 'Distribution Budgétaire Globale' : 'Total Spending Distribution'}
              </h2>
              <p className="text-xs text-slate-400">
                {lang === 'fr' ? 'Répartition détaillée des coûts opérationnels' : 'Detailed share of operational costs'}
              </p>
            </div>
          </div>
          <span className="text-base font-extrabold font-mono text-slate-100">
            {grandTotal.toFixed(2)} <span className="text-xs text-amber-400">{currencySymbol}</span>
          </span>
        </div>

        {/* Multi-segment visual bar */}
        <div className="mt-6">
          <div className="flex h-5 w-full overflow-hidden rounded-full bg-slate-900 border border-slate-800 p-0.5 shadow-inner">
            <div
              style={{ width: `${fuelShare}%` }}
              className="bg-gradient-to-r from-cyan-500 to-cyan-400 rounded-l-full transition-all duration-500 shadow-sm shadow-cyan-500/50"
              title={`Fuel: ${fuelShare}%`}
            />
            <div
              style={{ width: `${serviceShare}%` }}
              className="bg-gradient-to-r from-purple-500 to-indigo-500 transition-all duration-500 shadow-sm shadow-purple-500/50"
              title={`Maintenance: ${serviceShare}%`}
            />
            <div
              style={{ width: `${expenseShare}%` }}
              className="bg-gradient-to-r from-amber-500 to-amber-400 rounded-r-full transition-all duration-500 shadow-sm shadow-amber-500/50"
              title={`Expenses: ${expenseShare}%`}
            />
          </div>

          {/* Legend and cards */}
          <div className="mt-6 grid grid-cols-1 sm:grid-cols-3 gap-4">
            {/* Fuel Card */}
            <div className="flex items-center justify-between rounded-xl border border-cyan-500/30 bg-cyan-500/10 p-4">
              <div className="flex items-center gap-3">
                <div className="h-3.5 w-3.5 rounded-full bg-cyan-400 shadow-sm shadow-cyan-400/50" />
                <div>
                  <span className="text-xs font-mono font-bold text-slate-200 block">{t.reports.fuelShare}</span>
                  <span className="text-xs text-slate-400 font-mono">{fuelShare}% {lang === 'fr' ? 'du budget' : 'of budget'}</span>
                </div>
              </div>
              <span className="text-base font-extrabold font-mono text-cyan-300">
                {fuelTotal.toFixed(2)} <span className="text-xs">{currencySymbol}</span>
              </span>
            </div>

            {/* Maintenance Card */}
            <div className="flex items-center justify-between rounded-xl border border-purple-500/30 bg-purple-500/10 p-4">
              <div className="flex items-center gap-3">
                <div className="h-3.5 w-3.5 rounded-full bg-purple-400 shadow-sm shadow-purple-400/50" />
                <div>
                  <span className="text-xs font-mono font-bold text-slate-200 block">{t.reports.maintenanceShare}</span>
                  <span className="text-xs text-slate-400 font-mono">{serviceShare}% {lang === 'fr' ? 'du budget' : 'of budget'}</span>
                </div>
              </div>
              <span className="text-base font-extrabold font-mono text-purple-300">
                {serviceTotal.toFixed(2)} <span className="text-xs">{currencySymbol}</span>
              </span>
            </div>

            {/* Other Expenses Card */}
            <div className="flex items-center justify-between rounded-xl border border-amber-500/30 bg-amber-500/10 p-4">
              <div className="flex items-center gap-3">
                <div className="h-3.5 w-3.5 rounded-full bg-amber-400 shadow-sm shadow-amber-400/50" />
                <div>
                  <span className="text-xs font-mono font-bold text-slate-200 block">{t.reports.otherExpensesShare}</span>
                  <span className="text-xs text-slate-400 font-mono">{expenseShare}% {lang === 'fr' ? 'du budget' : 'of budget'}</span>
                </div>
              </div>
              <span className="text-base font-extrabold font-mono text-amber-300">
                {expenseTotal.toFixed(2)} <span className="text-xs">{currencySymbol}</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Monthly Summary Breakdown Table */}
      <div className="overflow-hidden rounded-2xl border border-slate-800 bg-[#0F1522] shadow-xl shadow-black/40">
        <div className="flex items-center justify-between p-5 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-400">
              <Calendar className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100 font-mono tracking-tight">
                {lang === 'fr' ? 'Historique Mensuel Détaillé' : 'Monthly Spending History'}
              </h2>
              <p className="text-xs text-slate-400">
                {lang === 'fr' ? 'Coûts consolidés par mois calendaire' : 'Aggregated costs per calendar month'}
              </p>
            </div>
          </div>
        </div>

        {monthlyRows.length === 0 ? (
          <div className="p-8 text-center text-sm font-mono text-slate-500">
            {t.common.noData}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900/80 border-b border-slate-800 text-[11px] font-mono font-bold uppercase tracking-wider text-slate-400">
                <tr>
                  <th className="px-6 py-3.5">{lang === 'fr' ? 'Mois' : 'Month'}</th>
                  <th className="px-6 py-3.5">{t.reports.fuelShare}</th>
                  <th className="px-6 py-3.5">{t.reports.maintenanceShare}</th>
                  <th className="px-6 py-3.5">{t.reports.otherExpensesShare}</th>
                  <th className="px-6 py-3.5 text-right font-extrabold">{t.common.total}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/70">
                {monthlyRows.map((row) => (
                  <tr key={row.monthKey} className="hover:bg-slate-800/40 transition">
                    <td className="px-6 py-4 font-bold font-mono text-slate-100">
                      {row.label}
                    </td>
                    <td className="px-6 py-4 text-slate-300 font-mono">
                      {row.fuel.toFixed(2)} <span className="text-xs text-slate-500">{currencySymbol}</span>
                    </td>
                    <td className="px-6 py-4 text-slate-300 font-mono">
                      {row.maintenance.toFixed(2)} <span className="text-xs text-slate-500">{currencySymbol}</span>
                    </td>
                    <td className="px-6 py-4 text-slate-300 font-mono">
                      {row.expenses.toFixed(2)} <span className="text-xs text-slate-500">{currencySymbol}</span>
                    </td>
                    <td className="px-6 py-4 text-right font-extrabold font-mono text-amber-400 text-base">
                      {row.total.toFixed(2)} {currencySymbol}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}