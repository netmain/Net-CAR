'use client';

import React, { useState, useMemo } from 'react';
import { useApp } from '@/lib/app-context';
import { FuelType } from '@/types';
import { 
  Fuel, 
  Plus, 
  Search, 
  Trash2, 
  Gauge, 
  DollarSign, 
  X, 
  AlertTriangle,
  Calendar,
  MapPin,
  Upload,
  Download,
  CheckCircle2,
  AlertCircle,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';

export default function FuelPage() {
  const { 
    activeVehicle, 
    fillUps, 
    addFillUp, 
    deleteFillUp, 
    importFuelioCsv,
    exportFuelioCsvForVehicle,
    userProfile, 
    lang,
    t 
  } = useApp();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [csvStatus, setCsvStatus] = useState<{ text: string; error?: boolean } | null>(null);
  const [importingCsv, setImportingCsv] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState<number>(30);

  // Form State
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [odometer, setOdometer] = useState<number | ''>(activeVehicle ? activeVehicle.currentOdometer : '');
  const [volume, setVolume] = useState<number | ''>('');
  const [pricePerUnit, setPricePerUnit] = useState<number | ''>('');
  const [totalCost, setTotalCost] = useState<number | ''>('');
  const [isFullTank, setIsFullTank] = useState(true);
  const [station, setStation] = useState('');
  const [notes, setNotes] = useState('');
  const [fuelType, setFuelType] = useState<FuelType>(activeVehicle?.fuelType || 'gasoline');
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const handleExportFuelioCsv = () => {
    if (!activeVehicle) return;
    try {
      const csvContent = exportFuelioCsvForVehicle(activeVehicle.id);
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      const safeName = `${activeVehicle.make}_${activeVehicle.model}`.replace(/[^a-zA-Z0-9_-]/g, '_');
      link.href = url;
      link.download = `vehicle-fuelio-${safeName}-${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      setCsvStatus({
        text:
          lang === 'fr'
            ? `Fichier CSV (format Fuelio) exporté avec succès pour ${activeVehicle.make} ${activeVehicle.model}.`
            : `Fuelio CSV file exported successfully for ${activeVehicle.make} ${activeVehicle.model}.`,
      });
      setTimeout(() => setCsvStatus(null), 6000);
    } catch (err) {
      console.error(err);
      setCsvStatus({
        text: lang === 'fr' ? "Erreur lors de l'export CSV." : 'Error exporting CSV.',
        error: true,
      });
    }
  };

  const handleImportFuelioCsvFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !activeVehicle) return;

    setImportingCsv(true);
    setCsvStatus(null);

    const reader = new FileReader();
    reader.onload = async (event) => {
      try {
        const csvText = (event.target?.result as string) || '';
        const res = await importFuelioCsv(csvText, activeVehicle.id);
        setImportingCsv(false);

        if (res.success) {
          setCurrentPage(1);
          setCsvStatus({
            text:
              lang === 'fr'
                ? `Import Fuelio réussi : ${res.importedFillUps} plein(s) et ${res.importedExpenses} dépense(s) ajoutés à ${activeVehicle.make} ${activeVehicle.model} !`
                : `Fuelio CSV imported: ${res.importedFillUps} fill-up(s) and ${res.importedExpenses} expense(s) added to ${activeVehicle.make} ${activeVehicle.model}!`,
          });
          setTimeout(() => setCsvStatus(null), 8000);
        } else {
          setCsvStatus({
            text:
              res.error ||
              (lang === 'fr'
                ? 'Aucune entrée valide trouvée dans ce fichier CSV Fuelio.'
                : 'No valid entries found in this Fuelio CSV file.'),
            error: true,
          });
        }
      } catch {
        setImportingCsv(false);
        setCsvStatus({
          text:
            lang === 'fr'
              ? 'Erreur lors de la lecture du fichier CSV.'
              : 'Failed to read the CSV file.',
          error: true,
        });
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  // Auto-calculation handlers
  const handleVolumeChange = (val: string) => {
    const v = val === '' ? '' : Number(val);
    setVolume(v);
    if (v !== '' && Number(v) > 0) {
      if (pricePerUnit !== '' && Number(pricePerUnit) > 0) {
        setTotalCost(Number((Number(v) * Number(pricePerUnit)).toFixed(2)));
      } else if (totalCost !== '' && Number(totalCost) > 0) {
        setPricePerUnit(Number((Number(totalCost) / Number(v)).toFixed(3)));
      }
    }
  };

  const handlePriceChange = (val: string) => {
    const p = val === '' ? '' : Number(val);
    setPricePerUnit(p);
    if (p !== '' && Number(p) > 0) {
      if (totalCost !== '' && Number(totalCost) > 0) {
        setVolume(Number((Number(totalCost) / Number(p)).toFixed(2)));
      } else if (volume !== '' && Number(volume) > 0) {
        setTotalCost(Number((Number(volume) * Number(p)).toFixed(2)));
      }
    }
  };

  const handleTotalCostChange = (val: string) => {
    const tc = val === '' ? '' : Number(val);
    setTotalCost(tc);
    if (tc !== '' && Number(tc) > 0) {
      if (pricePerUnit !== '' && Number(pricePerUnit) > 0) {
        setVolume(Number((Number(tc) / Number(pricePerUnit)).toFixed(2)));
      } else if (volume !== '' && Number(volume) > 0) {
        setPricePerUnit(Number((Number(tc) / Number(volume)).toFixed(3)));
      }
    }
  };

  const activeVehicleId = activeVehicle?.id;
  const currentUnits = userProfile.units;

  // Memoize vehicle fill-ups sorted newest first so typing in search or modal inputs never re-sorts
  const vehicleFillups = useMemo(() => {
    return fillUps
      .filter((f) => !activeVehicleId || f.vehicleId === activeVehicleId)
      .sort((a, b) => {
        if (b.odometer !== a.odometer) return b.odometer - a.odometer;
        return b.date.localeCompare(a.date);
      });
  }, [fillUps, activeVehicleId]);

  // Memoize KPIs and per-fillup consumption map in a single O(N) pass over chronological fill-ups
  const { totalCostSum, totalVolumeSum, fullTankCount, avgEconomy, fillupStatsMap } = useMemo(() => {
    let costSum = 0;
    let volSum = 0;
    let fullTanks = 0;
    for (let i = 0; i < vehicleFillups.length; i++) {
      const f = vehicleFillups[i];
      costSum += f.totalCost;
      volSum += f.volume;
      if (f.isFullTank) fullTanks++;
    }

    const chronological = [...vehicleFillups].sort((a, b) => a.odometer - b.odometer);
    let economy = 0;
    if (chronological.length >= 2) {
      const distance =
        chronological[chronological.length - 1].odometer - chronological[0].odometer;
      let vol = 0;
      for (let i = 1; i < chronological.length; i++) {
        vol += chronological[i].volume;
      }
      if (distance > 0 && vol > 0) {
        economy =
          currentUnits === 'metric'
            ? Number(((vol / distance) * 100).toFixed(1))
            : Number((distance / vol).toFixed(1));
      }
    }

    const statsMap = new Map<string, { distance?: number; consumption?: number }>();
    for (let i = 1; i < chronological.length; i++) {
      const prev = chronological[i - 1];
      const curr = chronological[i];
      const dist = curr.odometer - prev.odometer;
      let consumption: number | undefined;

      if (curr.isFullTank && prev.isFullTank && dist > 0 && curr.volume > 0) {
        consumption =
          currentUnits === 'metric'
            ? Number(((curr.volume / dist) * 100).toFixed(1))
            : Number((dist / curr.volume).toFixed(1));
      }
      statsMap.set(curr.id, { distance: dist, consumption });
    }

    return {
      totalCostSum: costSum,
      totalVolumeSum: volSum,
      fullTankCount: fullTanks,
      avgEconomy: economy,
      fillupStatsMap: statsMap,
    };
  }, [vehicleFillups, currentUnits]);

  // Memoize search filter
  const filteredFillups = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return vehicleFillups;
    return vehicleFillups.filter((f) => {
      return (
        (f.station && f.station.toLowerCase().includes(q)) ||
        (f.notes && f.notes.toLowerCase().includes(q)) ||
        (f.date && f.date.toLowerCase().includes(q)) ||
        f.odometer.toString().includes(q)
      );
    });
  }, [vehicleFillups, searchQuery]);

  // Fast client-side pagination so 500+ imported Fuelio entries render in <2ms
  const totalPages = Math.max(1, Math.ceil(filteredFillups.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const paginatedFillups = useMemo(() => {
    const start = (safeCurrentPage - 1) * pageSize;
    return filteredFillups.slice(start, start + pageSize);
  }, [filteredFillups, safeCurrentPage, pageSize]);

  // Units
  const currencySymbol =
    userProfile.currency === 'DZD'
      ? 'DA '
      : userProfile.currency === 'EUR'
        ? '€'
        : userProfile.currency === 'USD'
          ? '$'
          : userProfile.currency + ' ';
  const distUnit = userProfile.units === 'metric' ? 'km' : 'mi';
  const volumeUnit = userProfile.units === 'metric' ? 'L' : 'gal';
  const economyUnit = userProfile.units === 'metric' ? 'L/100km' : 'MPG';

  const handleOpenAddModal = () => {
    setDate(new Date().toISOString().split('T')[0]);
    setOdometer(activeVehicle ? activeVehicle.currentOdometer : '');
    setVolume('');
    
    const lastFillup = vehicleFillups.length > 0 ? vehicleFillups[0] : null;
    const defaultPrice = lastFillup?.pricePerUnit || (activeVehicle?.fuelType === 'diesel' ? 29.0 : 45.0);
    setPricePerUnit(defaultPrice);

    setTotalCost('');
    setIsFullTank(true);
    setStation(lastFillup?.station || (activeVehicle?.fuelType === 'diesel' ? 'Naftal' : ''));
    setNotes('');
    setFuelType(activeVehicle?.fuelType || 'gasoline');
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleSaveFillup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeVehicle) return;

    const errors: Record<string, string> = {};
    if (!date) errors.date = 'Date is required';
    if (odometer === '' || Number(odometer) <= 0) errors.odometer = 'Valid odometer is required';
    if (volume === '' || Number(volume) <= 0) errors.volume = 'Valid fuel volume is required';
    if (totalCost === '' || Number(totalCost) <= 0) errors.totalCost = 'Total cost is required';

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    try {
      const calculatedPPU = pricePerUnit !== '' ? Number(pricePerUnit) : Number((Number(totalCost) / Number(volume)).toFixed(3));

      await addFillUp({
        vehicleId: activeVehicle.id,
        date: new Date(date).toISOString(),
        odometer: Number(odometer),
        volume: Number(volume),
        pricePerUnit: calculatedPPU,
        totalCost: Number(totalCost),
        fuelType,
        isFullTank,
        station: station.trim() || undefined,
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
    await deleteFillUp(id);
    setDeleteConfirmId(null);
  };

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white whitespace-nowrap">
              {t.fuel.title}
            </h1>
            {activeVehicle && (
              <span className="rounded-xl border border-sky-500/40 bg-sky-500/10 text-sky-300 text-xs font-bold px-3 py-1 whitespace-nowrap">
                {activeVehicle.make} {activeVehicle.model} {activeVehicle.plate ? `(${activeVehicle.plate})` : ''}
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-slate-400">
            {t.fuel.subtitle}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 shrink-0">
          {/* Fuelio CSV Import */}
          <label
            className={`inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-700/80 bg-[#111827] px-4 py-2.5 text-xs font-mono font-bold text-slate-200 hover:border-sky-500/50 hover:text-sky-300 transition shadow-sm whitespace-nowrap ${
              !activeVehicle || importingCsv ? 'opacity-50 pointer-events-none' : 'cursor-pointer'
            }`}
            title={
              lang === 'fr'
                ? 'Importer un fichier CSV de sauvegarde Fuelio'
                : 'Import a Fuelio backup CSV file'
            }
          >
            <Upload className="h-4 w-4 text-sky-400" />
            <span>
              {importingCsv
                ? lang === 'fr'
                  ? 'Import...'
                  : 'Importing...'
                : lang === 'fr'
                  ? 'Importer CSV (Fuelio)'
                  : 'Import CSV (Fuelio)'}
            </span>
            <input
              type="file"
              accept=".csv,text/csv"
              onChange={handleImportFuelioCsvFile}
              disabled={!activeVehicle || importingCsv}
              className="hidden"
              aria-label={lang === 'fr' ? 'Importer CSV (Fuelio)' : 'Import CSV (Fuelio)'}
            />
          </label>

          {/* Fuelio CSV Export */}
          <button
            type="button"
            onClick={handleExportFuelioCsv}
            disabled={!activeVehicle}
            className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-700/80 bg-[#111827] px-4 py-2.5 text-xs font-mono font-bold text-slate-200 hover:border-amber-500/50 hover:text-amber-300 disabled:opacity-50 transition shadow-sm cursor-pointer whitespace-nowrap"
            title={
              lang === 'fr'
                ? 'Exporter les pleins et coûts au format CSV compatible Fuelio'
                : 'Export fill-ups and costs in Fuelio-compatible CSV format'
            }
          >
            <Download className="h-4 w-4 text-amber-400" />
            <span>{lang === 'fr' ? 'Exporter CSV (Fuelio)' : 'Export CSV (Fuelio)'}</span>
          </button>

          {/* Add Fillup */}
          <button
            onClick={handleOpenAddModal}
            disabled={!activeVehicle}
            className="inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-sky-500 to-blue-600 px-5 py-2.5 text-sm font-black text-white shadow-lg shadow-sky-500/25 hover:opacity-95 disabled:opacity-50 transition cursor-pointer whitespace-nowrap"
          >
            <Plus className="h-4 w-4" />
            <span>{t.fuel.addFillup}</span>
          </button>
        </div>
      </div>

      {/* CSV Import / Export Status Banner */}
      {csvStatus && (
        <div
          className={`flex items-center justify-between gap-3 rounded-2xl p-4 text-xs font-mono font-bold shadow-md ${
            csvStatus.error
              ? 'border border-red-500/40 bg-red-500/15 text-red-300'
              : 'border border-emerald-500/40 bg-emerald-500/15 text-emerald-300'
          }`}
        >
          <div className="flex items-center gap-2.5">
            {csvStatus.error ? (
              <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
            ) : (
              <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
            )}
            <span>{csvStatus.text}</span>
          </div>
          <button
            type="button"
            onClick={() => setCsvStatus(null)}
            className="rounded-lg p-1 text-slate-400 hover:text-white transition"
            aria-label="Close notification"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Avg Consumption */}
        <div className="rounded-2xl border border-slate-800/80 bg-[#111827]/90 p-5 shadow-lg">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">
              {t.dashboard.statsFuelEconomy}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-500/15 border border-sky-500/30 text-sky-400">
              <Fuel className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="font-mono text-3xl font-black text-white">
              {avgEconomy > 0 ? avgEconomy : '—'}
            </span>
            <span className="text-xs font-bold text-slate-400">{economyUnit}</span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400">
            {lang === 'fr'
              ? `Basé sur ${fullTankCount} pleins complets`
              : `Based on ${fullTankCount} full tanks`}
          </p>
        </div>

        {/* Total Spent */}
        <div className="rounded-2xl border border-slate-800/80 bg-[#111827]/90 p-5 shadow-lg">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">
              {t.fuel.totalCost}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
              <DollarSign className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-1">
            <span className="font-mono text-3xl font-black text-amber-400">
              {currencySymbol}{totalCostSum.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400">
            {lang === 'fr' ? 'Moy.' : 'Avg.'} {currencySymbol}
            {vehicleFillups.length > 0 ? (totalCostSum / vehicleFillups.length).toFixed(2) : '0.00'}{' '}
            / {lang === 'fr' ? 'plein' : 'fill-up'}
          </p>
        </div>

        {/* Total Volume */}
        <div className="rounded-2xl border border-slate-800/80 bg-[#111827]/90 p-5 shadow-lg">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">
              {lang === 'fr' ? 'Volume Total' : 'Total Volume'}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-500/15 border border-indigo-500/30 text-indigo-400">
              <Gauge className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="font-mono text-3xl font-black text-white">
              {totalVolumeSum.toFixed(1)}
            </span>
            <span className="text-xs font-bold text-slate-400">{volumeUnit}</span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400">
            {lang === 'fr' ? 'Total carburant injecté' : 'Total fuel volume'}
          </p>
        </div>

        {/* Total Fillups Count */}
        <div className="rounded-2xl border border-slate-800/80 bg-[#111827]/90 p-5 shadow-lg">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">
              {lang === 'fr' ? 'Ravitaillements' : 'Refuels'}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-400">
              <Calendar className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="font-mono text-3xl font-black text-white">
              {vehicleFillups.length}
            </span>
            <span className="text-xs font-bold text-slate-400">
              {lang === 'fr' ? 'entrées' : 'entries'}
            </span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400">
            {fullTankCount} {lang === 'fr' ? 'réservoirs pleins' : 'full tanks'}
          </p>
        </div>
      </div>

      {/* Filter, Search & Page Size Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="relative w-full sm:w-80">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder={
              lang === 'fr'
                ? 'Rechercher station, notes, odomètre...'
                : 'Search station, notes, odometer...'
            }
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setCurrentPage(1);
            }}
            className="w-full rounded-2xl border border-slate-800 bg-[#111827] pl-10 pr-4 py-2.5 text-sm text-slate-200 placeholder-slate-500 focus:border-sky-500 focus:outline-none focus:ring-1 focus:ring-sky-500 shadow-inner"
          />
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 text-xs text-slate-400 font-mono">
            <span>{lang === 'fr' ? 'Lignes / page :' : 'Rows / page:'}</span>
            <select
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value));
                setCurrentPage(1);
              }}
              aria-label="Rows per page"
              className="rounded-xl border border-slate-800 bg-[#111827] px-2.5 py-1 text-xs font-mono font-bold text-slate-200 focus:border-sky-500 focus:outline-none"
            >
              <option value={30}>30</option>
              <option value={60}>60</option>
              <option value={100}>100</option>
              <option value={500}>500</option>
            </select>
          </div>

          <span className="text-xs text-slate-400 font-mono">
            {lang === 'fr'
              ? `Affichage ${paginatedFillups.length} sur ${filteredFillups.length} pleins`
              : `Showing ${paginatedFillups.length} of ${filteredFillups.length} fill-ups`}
          </span>
        </div>
      </div>

      {/* Fill-ups Table / Card list */}
      {filteredFillups.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-slate-800 bg-[#111827]/60 p-12 text-center shadow-xl">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-sky-500/15 border border-sky-500/30 text-sky-400">
            <Fuel className="h-7 w-7" />
          </div>
          <h2 className="mt-4 text-base font-bold text-white">
            {vehicleFillups.length === 0 ? 'Aucun plein enregistré pour le moment' : 'Aucun enregistrement ne correspond'}
          </h2>
          <p className="mt-1 text-xs text-slate-400 max-w-sm mx-auto">
            {vehicleFillups.length === 0 
              ? 'Enregistrez votre premier plein pour calculer la consommation moyenne et le coût au kilomètre.'
              : 'Effacez votre recherche pour voir tous les pleins enregistrés.'}
          </p>
          {vehicleFillups.length === 0 && (
            <button
              onClick={handleOpenAddModal}
              className="mt-5 inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-sky-500 to-blue-600 px-5 py-2.5 text-sm font-black text-white shadow-md shadow-sky-500/20 hover:opacity-95 transition"
            >
              <Plus className="h-4 w-4" />
              <span>{t.fuel.addFillup}</span>
            </button>
          )}
        </div>
      ) : (
        <div className="overflow-hidden rounded-3xl border border-slate-800/80 bg-[#111827]/90 shadow-2xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900/90 border-b border-slate-800 text-[11px] font-black uppercase tracking-wider text-slate-400">
                <tr>
                  <th className="px-5 py-3.5">{t.common.date}</th>
                  <th className="px-5 py-3.5">{t.common.odometer}</th>
                  <th className="px-5 py-3.5">{t.fuel.volume}</th>
                  <th className="px-5 py-3.5">{t.fuel.pricePerUnit}</th>
                  <th className="px-5 py-3.5">{t.fuel.totalCost}</th>
                  <th className="px-5 py-3.5">{t.fuel.calculatedEconomy}</th>
                  <th className="px-5 py-3.5">{t.fuel.station}</th>
                  <th className="px-5 py-3.5 text-right">{t.common.actions}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-medium">
                {paginatedFillups.map((f) => {
                  const stats = fillupStatsMap.get(f.id);

                  return (
                    <tr key={f.id} className="hover:bg-slate-800/40 transition-colors">
                      {/* Date */}
                      <td className="px-5 py-4 whitespace-nowrap">
                        <div className="font-semibold text-slate-200">
                          {f.date.slice(0, 10)}
                        </div>
                        <div className="flex items-center gap-1.5 mt-0.5">
                          {f.isFullTank ? (
                            <span className="rounded-md bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 text-[10px] font-black px-1.5 py-0.5">
                              {lang === 'fr' ? 'Plein Complet' : 'Full Tank'}
                            </span>
                          ) : (
                            <span className="rounded-md bg-slate-800 border border-slate-700 text-slate-400 text-[10px] font-medium px-1.5 py-0.5">
                              {lang === 'fr' ? 'Partiel' : 'Partial'}
                            </span>
                          )}
                          <span className="text-[10px] font-mono text-slate-500 uppercase">
                            {f.fuelType}
                          </span>
                        </div>
                      </td>

                      {/* Odometer & Trip Distance */}
                      <td className="px-5 py-4 whitespace-nowrap">
                        <div className="font-mono font-bold text-sky-300">
                          {f.odometer.toLocaleString()} {distUnit}
                        </div>
                        {stats?.distance !== undefined && (
                          <div className="text-[11px] font-mono text-slate-500">
                            +{stats.distance.toLocaleString()} {distUnit}
                          </div>
                        )}
                      </td>

                      {/* Volume */}
                      <td className="px-5 py-4 whitespace-nowrap text-slate-200 font-mono">
                        {f.volume} {volumeUnit}
                      </td>

                      {/* Price Per Unit */}
                      <td className="px-5 py-4 whitespace-nowrap text-slate-400 font-mono">
                        {currencySymbol}{f.pricePerUnit.toFixed(2)}
                      </td>

                      {/* Total Cost */}
                      <td className="px-5 py-4 whitespace-nowrap">
                        <span className="font-mono font-black text-amber-400">
                          {currencySymbol}{f.totalCost.toFixed(2)}
                        </span>
                      </td>

                      {/* Calculated Economy */}
                      <td className="px-5 py-4 whitespace-nowrap">
                        {stats?.consumption !== undefined ? (
                          <span className="inline-flex items-center gap-1 rounded-xl border border-sky-500/30 bg-sky-500/10 px-2.5 py-1 text-xs font-mono font-black text-sky-300">
                            {stats.consumption} {economyUnit}
                          </span>
                        ) : (
                          <span className="text-xs text-slate-600 font-mono">—</span>
                        )}
                      </td>

                      {/* Station & Notes */}
                      <td className="px-5 py-4">
                        <div className="font-medium text-slate-300 flex items-center gap-1.5">
                          {f.station ? (
                            <>
                              <MapPin className="h-3.5 w-3.5 text-sky-400 shrink-0" />
                              <span className="truncate max-w-[160px] rounded-md bg-slate-800/90 border border-slate-700/80 px-2 py-0.5 text-xs font-bold text-slate-200">{f.station}</span>
                            </>
                          ) : (
                            <span className="text-slate-600">—</span>
                          )}
                        </div>
                        {f.notes && (
                          <div className="text-[11px] text-slate-400 italic truncate max-w-[180px] mt-0.5">
                            {f.notes}
                          </div>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="px-5 py-4 text-right whitespace-nowrap">
                        <button
                          onClick={() => setDeleteConfirmId(f.id)}
                          className="rounded-xl p-1.5 text-slate-500 hover:bg-red-500/10 hover:text-red-400 transition"
                          title={t.common.delete}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination Footer */}
          {totalPages > 1 && (
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-slate-800/80 bg-slate-900/60 px-5 py-3.5">
              <span className="text-xs font-mono text-slate-400">
                {lang === 'fr'
                  ? `Page ${safeCurrentPage} sur ${totalPages} (${filteredFillups.length} entrées au total)`
                  : `Page ${safeCurrentPage} of ${totalPages} (${filteredFillups.length} total entries)`}
              </span>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={safeCurrentPage <= 1}
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  className="inline-flex items-center gap-1 rounded-xl border border-slate-700 bg-slate-800/90 px-3 py-1.5 text-xs font-mono font-bold text-slate-200 hover:border-sky-500/50 hover:text-sky-300 disabled:opacity-40 transition cursor-pointer"
                >
                  <ChevronLeft className="h-3.5 w-3.5" />
                  <span>{lang === 'fr' ? 'Précédent' : 'Previous'}</span>
                </button>
                <button
                  type="button"
                  disabled={safeCurrentPage >= totalPages}
                  onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                  className="inline-flex items-center gap-1 rounded-xl border border-slate-700 bg-slate-800/90 px-3 py-1.5 text-xs font-mono font-bold text-slate-200 hover:border-sky-500/50 hover:text-sky-300 disabled:opacity-40 transition cursor-pointer"
                >
                  <span>{lang === 'fr' ? 'Suivant' : 'Next'}</span>
                  <ChevronRight className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Add Fill-up Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-md overflow-y-auto animate-in fade-in duration-150">
          <div className="relative w-full max-w-lg rounded-3xl bg-[#0F1522] border border-slate-800 p-6 shadow-2xl shadow-black/90 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
                  <Fuel className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-slate-100 font-mono tracking-tight">{t.fuel.addFillup}</h2>
                  <p className="text-xs text-slate-400 font-mono">
                    {activeVehicle?.make} {activeVehicle?.model} {activeVehicle?.plate ? `(${activeVehicle.plate})` : ''}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setIsModalOpen(false)}
                className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSaveFillup} className="mt-5 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-300 font-mono uppercase tracking-wider mb-1.5">
                    {t.common.date} *
                  </label>
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full rounded-xl border border-slate-800 bg-[#080B11] px-3.5 py-2.5 text-sm font-mono text-slate-100 placeholder:text-slate-600 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/50 transition"
                  />
                  {formErrors.date && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.date}</p>}
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-300 font-mono uppercase tracking-wider mb-1.5">
                    {t.common.odometer} ({distUnit}) *
                  </label>
                  <input
                    type="number"
                    placeholder="e.g. 64250"
                    value={odometer}
                    onChange={(e) => setOdometer(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full rounded-xl border border-slate-800 bg-[#080B11] px-3.5 py-2.5 text-sm font-mono text-slate-100 placeholder:text-slate-600 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/50 transition"
                  />
                  {formErrors.odometer && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.odometer}</p>}
                </div>
              </div>

              {/* Fuel Price, Total Paid, and Calculated Liters */}
              <div className="rounded-2xl border border-amber-500/30 bg-amber-500/5 p-4 space-y-3 relative overflow-hidden">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  {/* 1. Total Cost Paid */}
                  <div>
                    <label className="block text-xs font-black text-amber-400 font-mono uppercase tracking-wider mb-1.5">
                      {t.fuel.totalCost} ({currencySymbol}) *
                    </label>
                    <input
                      type="number"
                      step="any"
                      placeholder="e.g. 2000"
                      value={totalCost}
                      onChange={(e) => handleTotalCostChange(e.target.value)}
                      className="w-full rounded-xl border border-amber-500/50 bg-[#080B11] px-3.5 py-2.5 text-base font-mono font-black text-amber-400 shadow-inner placeholder:text-slate-600 focus:border-amber-400 focus:outline-none focus:ring-2 focus:ring-amber-500/30 transition"
                    />
                    {formErrors.totalCost && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.totalCost}</p>}
                  </div>

                  {/* 2. Fuel Price per Liter */}
                  <div>
                    <label className="block text-xs font-bold text-slate-300 font-mono uppercase tracking-wider mb-1.5">
                      {lang === 'fr' ? 'Prix / L' : 'Price / Unit'} ({currencySymbol})
                    </label>
                    <input
                      type="number"
                      step="any"
                      placeholder="e.g. 29.00"
                      value={pricePerUnit}
                      onChange={(e) => handlePriceChange(e.target.value)}
                      className="w-full rounded-xl border border-slate-800 bg-[#080B11] px-3.5 py-2.5 text-sm font-mono font-bold text-cyan-300 placeholder:text-slate-600 focus:border-cyan-500 focus:outline-none focus:ring-1 focus:ring-cyan-500 transition"
                    />
                  </div>

                  {/* 3. Fuel Volume (Calculated or Entered) */}
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <label className="block text-xs font-bold text-slate-300 font-mono uppercase tracking-wider">
                        {t.fuel.volume} ({volumeUnit}) *
                      </label>
                      {volume !== '' && totalCost !== '' && pricePerUnit !== '' && (
                        <span className="text-[10px] font-mono font-bold text-cyan-400 bg-cyan-500/15 border border-cyan-500/30 px-1.5 py-0.5 rounded-md">
                          ✓ Auto
                        </span>
                      )}
                    </div>
                    <input
                      type="number"
                      step="any"
                      placeholder="e.g. 68.97"
                      value={volume}
                      onChange={(e) => handleVolumeChange(e.target.value)}
                      className="w-full rounded-xl border border-slate-800 bg-[#080B11] px-3.5 py-2.5 text-sm font-mono font-bold text-cyan-300 placeholder:text-slate-600 focus:border-cyan-500 focus:outline-none focus:ring-1 focus:ring-cyan-500 transition"
                    />
                    {formErrors.volume && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.volume}</p>}
                  </div>
                </div>

                <p className="text-[11px] text-amber-300/80 font-mono flex items-center gap-1.5 pt-1">
                  <span>💡</span>
                  <span>
                    {lang === 'fr' 
                      ? 'Entrez simplement le prix au litre et le montant payé : le nombre de litres se calcule automatiquement !' 
                      : 'Just enter the fuel price and total amount paid: total liters are calculated automatically!'}
                  </span>
                </p>
              </div>

              {/* Full Tank Toggle */}
              <div className="flex items-center gap-3 rounded-xl bg-slate-900/60 p-3.5 border border-slate-800">
                <input
                  type="checkbox"
                  id="full-tank"
                  checked={isFullTank}
                  onChange={(e) => setIsFullTank(e.target.checked)}
                  className="h-4 w-4 rounded border-slate-700 bg-[#080B11] text-amber-500 focus:ring-amber-500/50"
                />
                <label htmlFor="full-tank" className="cursor-pointer select-none text-xs">
                  <span className="font-bold text-slate-200 font-mono block">{t.fuel.isFullTank}</span>
                  <span className="text-slate-400 text-[11px] font-mono">{t.fuel.fullTankHelp}</span>
                </label>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-300 font-mono uppercase tracking-wider mb-1.5">
                    {t.fuel.station}
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Naftal, Total, Shell"
                    value={station}
                    onChange={(e) => setStation(e.target.value)}
                    className="w-full rounded-xl border border-slate-800 bg-[#080B11] px-3.5 py-2.5 text-sm font-mono text-slate-100 placeholder:text-slate-600 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/50 transition"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-300 font-mono uppercase tracking-wider mb-1.5">
                    {t.vehicles.fuelType}
                  </label>
                  <select
                    value={fuelType}
                    onChange={(e) => setFuelType(e.target.value as FuelType)}
                    className="w-full rounded-xl border border-slate-800 bg-[#080B11] px-3.5 py-2.5 text-sm font-mono text-slate-100 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/50 transition"
                  >
                    {(Object.keys(t.fuelTypes) as FuelType[]).map((ft) => (
                      <option key={ft} value={ft} className="bg-[#0F1522] text-slate-100">
                        {t.fuelTypes[ft]}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-300 font-mono uppercase tracking-wider mb-1.5">
                  {t.common.notes}
                </label>
                <input
                  type="text"
                  placeholder="e.g. Highway trip, premium fuel, tire pressure checked..."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full rounded-xl border border-slate-800 bg-[#080B11] px-3.5 py-2.5 text-sm font-mono text-slate-100 placeholder:text-slate-600 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/50 transition"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="rounded-xl border border-slate-800 bg-slate-900 px-4 py-2 text-sm font-semibold text-slate-300 hover:bg-slate-800 hover:text-white transition"
                >
                  {t.common.cancel}
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 px-6 py-2.5 text-sm font-black text-slate-950 shadow-lg shadow-amber-500/20 hover:opacity-95 disabled:opacity-50 transition"
                >
                  {submitting ? t.common.loading : t.common.save}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      {deleteConfirmId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-md">
          <div className="relative w-full max-w-md rounded-3xl bg-[#0F1522] border border-red-500/30 p-6 shadow-2xl shadow-black/90 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3 text-red-400">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-red-500/15 border border-red-500/30 text-red-400">
                <AlertTriangle className="h-5 w-5" />
              </div>
              <h2 className="text-lg font-bold text-slate-100 font-mono">{t.common.delete}</h2>
            </div>
            <p className="mt-3 text-sm text-slate-300 leading-relaxed font-mono">
              {t.common.confirmDelete}
            </p>
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                onClick={() => setDeleteConfirmId(null)}
                className="rounded-xl border border-slate-800 bg-slate-900 px-4 py-2 text-sm font-semibold text-slate-300 hover:bg-slate-800 hover:text-white transition"
              >
                {t.common.cancel}
              </button>
              <button
                onClick={() => handleDelete(deleteConfirmId)}
                className="rounded-xl bg-red-600 hover:bg-red-500 px-5 py-2 text-sm font-bold text-white shadow-lg shadow-red-600/30 transition"
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
