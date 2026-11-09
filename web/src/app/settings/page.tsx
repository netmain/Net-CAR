'use client';

import React, { useState, useEffect } from 'react';
import { useApp } from '@/lib/app-context';
import { SupportedCurrency, Units, SupportedLanguage } from '@/types';
import {
  Globe,
  Cloud,
  Download,
  Upload,
  CheckCircle2,
  User,
  Save,
  AlertCircle,
  HardDrive,
  RefreshCw,
  LogIn,
  LogOut,
  FileSpreadsheet,
  Smartphone,
} from 'lucide-react';

export default function SettingsPage() {
  const {
    user,
    userProfile,
    activeVehicle,
    updatePreferences,
    lang,
    setLang,
    syncStatus,
    lastSyncedAt,
    setAuthModalOpen,
    signOut,
    syncToCloudNow,
    exportBackupJson,
    importBackupJson,
    exportFuelioCsvForVehicle,
    importFuelioCsv,
    t,
  } = useApp();

  // Profile Form State
  const [name, setName] = useState(userProfile.name || '');
  const [email, setEmail] = useState(userProfile.email || '');
  const [profileSuccess, setProfileSuccess] = useState(false);

  // Sync & Backup State
  const [cloudSyncing, setCloudSyncing] = useState(false);
  const [cloudMessage, setCloudMessage] = useState<{ text: string; error?: boolean } | null>(null);
  const [restoreMessage, setRestoreMessage] = useState<{ text: string; error?: boolean } | null>(
    null
  );

  useEffect(() => {
    setName(userProfile.name || '');
    setEmail(userProfile.email || '');
  }, [userProfile.name, userProfile.email]);

  const isCloudConnected = Boolean(user && !user.isDemo);

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    updatePreferences({ name: name.trim(), email: email.trim() });
    setProfileSuccess(true);
    setTimeout(() => setProfileSuccess(false), 3000);
  };

  const handleLanguageChange = (newLang: SupportedLanguage) => {
    setLang(newLang);
    updatePreferences({ preferredLang: newLang });
  };

  const handleUnitsChange = (newUnits: Units) => {
    updatePreferences({ units: newUnits });
  };

  const handleCurrencyChange = (newCurrency: SupportedCurrency) => {
    updatePreferences({ currency: newCurrency });
  };

  // Real Firebase Cloud Sync action
  const handleCloudSync = async () => {
    setCloudMessage(null);
    if (!isCloudConnected) {
      setAuthModalOpen(true);
      return;
    }

    setCloudSyncing(true);
    try {
      const res = await syncToCloudNow();
      if (res.success) {
        setCloudMessage({
          text:
            lang === 'fr'
              ? `Synchronisation Cloud réussie (${res.count || 0} éléments sauvegardés sur Firebase pour ${user?.email}).`
              : `Cloud sync complete (${res.count || 0} records saved to Firebase for ${user?.email}).`,
        });
        setTimeout(() => setCloudMessage(null), 6000);
      } else if (res.error) {
        setCloudMessage({ text: res.error, error: true });
      }
    } catch (err) {
      console.error(err);
      setCloudMessage({
        text:
          lang === 'fr'
            ? 'La synchronisation Cloud a rencontré un problème. Vos données restent sauvegardées localement.'
            : 'Cloud sync encountered an issue. Your data remains safely saved locally.',
        error: true,
      });
    } finally {
      setCloudSyncing(false);
    }
  };

  // Local JSON Export action
  const handleExportLocalJson = () => {
    try {
      const json = exportBackupJson();
      const blob = new Blob([json], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `NetCAR_Backup_${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      setRestoreMessage({
        text:
          lang === 'fr'
            ? 'Fichier JSON exporté sur votre appareil avec succès.'
            : 'JSON backup file downloaded to your device.',
      });
      setTimeout(() => setRestoreMessage(null), 5000);
    } catch (err) {
      console.error(err);
    }
  };

  // Fuelio CSV Export action
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

      setRestoreMessage({
        text:
          lang === 'fr'
            ? `Sauvegarde CSV Fuelio exportée pour ${activeVehicle.make} ${activeVehicle.model}.`
            : `Fuelio CSV backup exported for ${activeVehicle.make} ${activeVehicle.model}.`,
      });
      setTimeout(() => setRestoreMessage(null), 6000);
    } catch (err) {
      console.error(err);
    }
  };

  // Fuelio CSV Import action
  const handleImportFuelioCsvFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !activeVehicle) return;

    const reader = new FileReader();
    reader.onload = async (event) => {
      try {
        const content = (event.target?.result as string) || '';
        const res = await importFuelioCsv(content, activeVehicle.id);
        if (res.success) {
          setRestoreMessage({
            text:
              lang === 'fr'
                ? `Import CSV Fuelio réussi : ${res.importedFillUps} plein(s) et ${res.importedExpenses} dépense(s) ajoutés à ${activeVehicle.make} ${activeVehicle.model} !`
                : `Fuelio CSV imported: ${res.importedFillUps} fill-up(s) and ${res.importedExpenses} expense(s) added to ${activeVehicle.make} ${activeVehicle.model}!`,
          });
          setTimeout(() => setRestoreMessage(null), 7000);
        } else {
          setRestoreMessage({
            text:
              res.error ||
              (lang === 'fr' ? 'Fichier CSV Fuelio invalide.' : 'Invalid Fuelio CSV file.'),
            error: true,
          });
        }
      } catch {
        setRestoreMessage({
          text:
            lang === 'fr'
              ? 'Échec de lecture du fichier CSV.'
              : 'Failed to read CSV file.',
          error: true,
        });
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  // Restore handler from file upload
  const handleRestoreFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (event) => {
      try {
        const content = event.target?.result as string;
        const success = importBackupJson(content);
        if (success) {
          if (isCloudConnected) {
            await syncToCloudNow();
          }
          setRestoreMessage({
            text:
              lang === 'fr'
                ? isCloudConnected
                  ? 'Données restaurées depuis le fichier et synchronisées avec le Cloud !'
                  : 'Données restaurées avec succès depuis le fichier local !'
                : isCloudConnected
                  ? 'Data restored from file and synced to the Cloud!'
                  : 'Data restored successfully from local backup file!',
          });
          setTimeout(() => setRestoreMessage(null), 5000);
        } else {
          setRestoreMessage({
            text:
              lang === 'fr'
                ? 'Fichier de sauvegarde invalide ou corrompu.'
                : 'Invalid or corrupt backup file.',
            error: true,
          });
        }
      } catch {
        setRestoreMessage({
          text:
            lang === 'fr' ? 'Échec de lecture du fichier.' : 'Failed to read backup file.',
          error: true,
        });
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  const currencies: { code: SupportedCurrency; label: string; symbol: string }[] = [
    { code: 'DZD', label: 'Dinar Algérien (DZD / د.ج)', symbol: 'DA' },
    { code: 'EUR', label: 'Euro (EUR)', symbol: '€' },
    { code: 'USD', label: 'US Dollar (USD)', symbol: '$' },
    { code: 'CAD', label: 'Canadian Dollar (CAD)', symbol: 'CA$' },
    { code: 'GBP', label: 'British Pound (GBP)', symbol: '£' },
    { code: 'MAD', label: 'Dirham Marocain (MAD)', symbol: 'DH' },
    { code: 'CHF', label: 'Franc Suisse (CHF)', symbol: 'CHF' },
  ];

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      {/* Header Banner */}
      <div className="border-b border-slate-800/80 pb-5">
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-100 font-mono">
          {t.settings.title}
        </h1>
        <p className="mt-1 text-sm text-slate-400">{t.settings.subtitle}</p>
      </div>

      {/* Section 1: Localization & Units */}
      <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-6 sm:p-8 shadow-lg space-y-6">
        <h2 className="text-base font-bold text-slate-100 font-mono tracking-tight flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
            <Globe className="h-5 w-5" />
          </div>
          <span>
            {lang === 'fr'
              ? 'Préférences Régionales & Affichage'
              : 'Regional & Display Preferences'}
          </span>
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Language Selector */}
          <div>
            <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-2">
              {t.settings.language}
            </label>
            <div className="flex rounded-xl border border-slate-800 bg-slate-900/90 p-1">
              <button
                type="button"
                onClick={() => handleLanguageChange('en')}
                className={`flex-1 rounded-lg py-2 text-xs font-mono font-bold transition ${
                  lang === 'en'
                    ? 'bg-amber-500 text-slate-950 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                English (EN)
              </button>
              <button
                type="button"
                onClick={() => handleLanguageChange('fr')}
                className={`flex-1 rounded-lg py-2 text-xs font-mono font-bold transition ${
                  lang === 'fr'
                    ? 'bg-amber-500 text-slate-950 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Français (FR)
              </button>
            </div>
            <p className="mt-1.5 text-[11px] text-slate-500 font-mono">
              {lang === 'fr' ? 'Traduction interface & dates' : 'Interface translation & dates'}
            </p>
          </div>

          {/* Units Selector */}
          <div>
            <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-2">
              {t.settings.units}
            </label>
            <div className="flex rounded-xl border border-slate-800 bg-slate-900/90 p-1">
              <button
                type="button"
                onClick={() => handleUnitsChange('metric')}
                className={`flex-1 rounded-lg py-2 text-xs font-mono font-bold transition ${
                  userProfile.units === 'metric'
                    ? 'bg-amber-500 text-slate-950 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Metric (km, L)
              </button>
              <button
                type="button"
                onClick={() => handleUnitsChange('imperial')}
                className={`flex-1 rounded-lg py-2 text-xs font-mono font-bold transition ${
                  userProfile.units === 'imperial'
                    ? 'bg-amber-500 text-slate-950 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Imperial (mi, gal)
              </button>
            </div>
            <p className="mt-1.5 text-[11px] text-slate-500 font-mono">
              {lang === 'fr' ? 'Distance compteur & carburant' : 'Odometer distance & fuel volume'}
            </p>
          </div>

          {/* Currency Selector */}
          <div>
            <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-2">
              {t.settings.currency}
            </label>
            <select
              value={userProfile.currency}
              onChange={(e) => handleCurrencyChange(e.target.value as SupportedCurrency)}
              className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-xs font-mono font-bold text-slate-100 shadow-sm focus:border-amber-500 focus:outline-none"
            >
              {currencies.map((curr) => (
                <option key={curr.code} value={curr.code} className="bg-slate-900 text-slate-100">
                  {curr.label} ({curr.symbol})
                </option>
              ))}
            </select>
            <p className="mt-1.5 text-[11px] text-slate-500 font-mono">
              {lang === 'fr'
                ? 'Symbole pour carburant & frais'
                : 'Fuel, service and expense symbol'}
            </p>
          </div>
        </div>
      </div>

      {/* Section 2: Real Firebase Cloud Synchronization & Local Backup */}
      <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-6 sm:p-8 shadow-lg space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div className="space-y-1">
            <h2 className="text-base font-bold text-slate-100 font-mono tracking-tight flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
                <Cloud className="h-5 w-5" />
              </div>
              <span>
                {lang === 'fr'
                  ? 'Synchronisation Cloud & Sauvegarde'
                  : 'Cloud Synchronization & Backup'}
              </span>
            </h2>
            <p className="text-xs text-slate-400 max-w-xl leading-relaxed font-sans">
              {isCloudConnected
                ? lang === 'fr'
                  ? `Votre garage est synchronisé en temps réel avec le compte Cloud ${user?.email}.`
                  : `Your garage is synchronized in real time with Cloud account ${user?.email}.`
                : lang === 'fr'
                  ? "Vous êtes actuellement en Mode Local (hors-ligne). Connectez-vous avec Google (Gmail) ou Email/Mot de passe pour synchroniser vos véhicules dans le Cloud entre tous vos appareils."
                  : 'You are currently in Local Mode (offline). Sign in with Google (Gmail) or Email/Password to synchronize your vehicles in the Cloud across all your devices.'}
            </p>
          </div>

          {/* Honest, Real Sync Status Badge */}
          {isCloudConnected ? (
            <div className="flex items-center gap-2 shrink-0">
              <div className="flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3.5 py-1.5 text-xs font-mono font-bold text-emerald-400">
                <div className="h-2 w-2 rounded-full bg-emerald-400" />
                <span>
                  {syncStatus === 'syncing'
                    ? lang === 'fr'
                      ? 'Sync en cours...'
                      : 'Syncing...'
                    : `${lang === 'fr' ? 'Connecté :' : 'Connected:'} ${user?.email}`}
                </span>
              </div>
              <button
                type="button"
                onClick={signOut}
                className="flex items-center gap-1 rounded-xl border border-slate-700 bg-slate-800/80 px-3 py-1.5 text-xs font-mono font-semibold text-slate-300 hover:border-red-500/40 hover:text-red-400 transition"
                title={t.nav.logout}
              >
                <LogOut className="h-3.5 w-3.5" />
                <span>{t.nav.logout}</span>
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2 shrink-0">
              <div className="flex items-center gap-2 rounded-full border border-amber-500/40 bg-amber-500/10 px-3.5 py-1.5 text-xs font-mono font-bold text-amber-300">
                <div className="h-2 w-2 rounded-full bg-amber-400" />
                <span>
                  {lang === 'fr' ? 'Non connecté (Mode Local)' : 'Not Connected (Local Mode)'}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setAuthModalOpen(true)}
                className="flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 px-3.5 py-1.5 text-xs font-mono font-bold text-slate-950 shadow-md hover:opacity-95 transition cursor-pointer"
              >
                <LogIn className="h-3.5 w-3.5" />
                <span>{lang === 'fr' ? 'Se connecter' : 'Sign In'}</span>
              </button>
            </div>
          )}
        </div>

        {/* Notifications */}
        {cloudMessage && (
          <div
            className={`flex items-center gap-2 rounded-xl p-3.5 text-xs font-mono font-semibold ${
              cloudMessage.error
                ? 'bg-red-500/15 border border-red-500/30 text-red-300'
                : 'bg-emerald-500/15 border border-emerald-500/30 text-emerald-300'
            }`}
          >
            {cloudMessage.error ? (
              <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
            ) : (
              <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
            )}
            <span>{cloudMessage.text}</span>
          </div>
        )}

        {restoreMessage && (
          <div
            className={`flex items-center gap-2 rounded-xl p-3.5 text-xs font-mono font-semibold ${
              restoreMessage.error
                ? 'bg-red-500/15 border border-red-500/30 text-red-300'
                : 'bg-emerald-500/15 border border-emerald-500/30 text-emerald-300'
            }`}
          >
            {restoreMessage.error ? (
              <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
            ) : (
              <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
            )}
            <span>{restoreMessage.text}</span>
          </div>
        )}

        {/* Actions: 1. Real Cloud Sync | 2. Local JSON Export/Import */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          {/* Card 1: Real Firebase Cloud Sync */}
          <div className="rounded-xl border border-slate-800 p-5 bg-slate-900/60 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 font-mono font-bold text-slate-200 text-sm">
                  <Cloud className="h-4 w-4 text-cyan-400" />
                  <span>
                    {lang === 'fr'
                      ? 'Synchronisation Cloud (Multi-Appareils)'
                      : 'Cloud Sync (Multi-Device)'}
                  </span>
                </div>
              </div>
              <p className="mt-1.5 text-xs text-slate-400 font-sans">
                {isCloudConnected
                  ? lang === 'fr'
                    ? `Sauvegarde et synchronise instantanément vos véhicules, pleins, dépenses et rappels sur votre compte (${user?.email}).`
                    : `Instantly backs up and syncs your vehicles, fill-ups, expenses, and reminders to your Cloud account (${user?.email}).`
                  : lang === 'fr'
                    ? 'Connectez votre compte Google (Gmail) ou Email pour sauvegarder vos données dans le Cloud et les retrouver sur tous vos appareils.'
                    : 'Connect your Google (Gmail) or Email account to back up your data in the Cloud and access it across all devices.'}
              </p>
              {isCloudConnected && lastSyncedAt && (
                <p className="mt-2 text-[11px] font-mono text-emerald-400">
                  {lang === 'fr' ? 'Dernière synchro :' : 'Last synced:'}{' '}
                  {new Date(lastSyncedAt).toLocaleTimeString()}
                </p>
              )}
            </div>
            <button
              type="button"
              onClick={handleCloudSync}
              disabled={cloudSyncing}
              className="mt-4 flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 px-4 py-2.5 text-xs font-mono font-bold text-slate-950 shadow-md hover:opacity-95 disabled:opacity-50 transition cursor-pointer"
            >
              {cloudSyncing ? (
                <RefreshCw className="h-4 w-4 animate-spin" />
              ) : isCloudConnected ? (
                <RefreshCw className="h-4 w-4" />
              ) : (
                <LogIn className="h-4 w-4" />
              )}
              <span>
                {cloudSyncing
                  ? lang === 'fr'
                    ? 'Synchronisation Cloud en cours...'
                    : 'Syncing with Cloud...'
                  : isCloudConnected
                    ? lang === 'fr'
                      ? 'Synchroniser avec le Cloud maintenant'
                      : 'Sync with Cloud Now'
                    : lang === 'fr'
                      ? 'Se connecter (Google / Email) pour synchroniser'
                      : 'Sign In (Google / Email) to Sync'}
              </span>
            </button>
          </div>

          {/* Card 2: Local File Backup (Export / Import JSON) */}
          <div className="rounded-xl border border-slate-800 p-5 bg-slate-900/60 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-2 font-mono font-bold text-slate-200 text-sm">
                <HardDrive className="h-4 w-4 text-indigo-400" />
                <span>
                  {lang === 'fr'
                    ? 'Sauvegarde Fichier Local (.JSON)'
                    : 'Local File Backup (.JSON)'}
                </span>
              </div>
              <p className="mt-1.5 text-xs text-slate-400 font-sans">
                {lang === 'fr'
                  ? 'Exportez une copie de sauvegarde sur votre PC au format .JSON ou restaurez un fichier existant.'
                  : 'Download a local .JSON backup copy to your PC or restore from a saved backup file.'}
              </p>
            </div>
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              <button
                type="button"
                onClick={handleExportLocalJson}
                className="flex items-center justify-center gap-1.5 rounded-xl border border-slate-700 bg-slate-800/90 px-3 py-2.5 text-xs font-mono font-bold text-slate-200 hover:bg-slate-700 transition cursor-pointer"
              >
                <Download className="h-3.5 w-3.5 text-amber-400" />
                <span>{lang === 'fr' ? 'Exporter .JSON' : 'Export .JSON'}</span>
              </button>

              <label className="flex items-center justify-center gap-1.5 rounded-xl border border-slate-700 bg-slate-800/90 px-3 py-2.5 text-xs font-mono font-bold text-slate-200 hover:bg-slate-700 cursor-pointer transition">
                <Upload className="h-3.5 w-3.5 text-cyan-400" />
                <span>{lang === 'fr' ? 'Importer .JSON' : 'Import .JSON'}</span>
                <input
                  type="file"
                  accept=".json"
                  onChange={handleRestoreFile}
                  className="hidden"
                />
              </label>
            </div>
          </div>

          {/* Card 3: Fuelio CSV Dual-Compatibility (Import / Export CSV) */}
          <div className="rounded-xl border border-slate-800 p-5 bg-slate-900/60 flex flex-col justify-between md:col-span-2">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div>
                <div className="flex items-center gap-2 font-mono font-bold text-slate-200 text-sm">
                  <FileSpreadsheet className="h-4 w-4 text-emerald-400" />
                  <span>
                    {lang === 'fr'
                      ? 'Compatibilité CSV Fuelio (Import / Export Bidirectionnel)'
                      : 'Fuelio CSV Dual Compatibility (Import / Export)'}
                  </span>
                </div>
                <p className="mt-1.5 text-xs text-slate-400 font-sans max-w-2xl">
                  {lang === 'fr'
                    ? `Importez directement une sauvegarde CSV Fuelio (pleins "## Log" et dépenses "## Costs") sur le véhicule actif (${activeVehicle?.make || ''} ${activeVehicle?.model || ''}), ou exportez au même format CSV compatible Fuelio.`
                    : `Import a Fuelio CSV backup ("## Log" fill-ups and "## Costs" expenses) into the active vehicle (${activeVehicle?.make || ''} ${activeVehicle?.model || ''}), or export in the exact same Fuelio-compatible CSV format.`}
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2.5 shrink-0">
                <button
                  type="button"
                  onClick={handleExportFuelioCsv}
                  disabled={!activeVehicle}
                  className="flex items-center justify-center gap-1.5 rounded-xl border border-slate-700 bg-slate-800/90 px-3.5 py-2.5 text-xs font-mono font-bold text-slate-200 hover:border-amber-500/50 hover:text-amber-300 disabled:opacity-50 transition cursor-pointer"
                >
                  <Download className="h-3.5 w-3.5 text-amber-400" />
                  <span>{lang === 'fr' ? 'Exporter .CSV (Fuelio)' : 'Export .CSV (Fuelio)'}</span>
                </button>

                <label className="flex items-center justify-center gap-1.5 rounded-xl border border-slate-700 bg-slate-800/90 px-3.5 py-2.5 text-xs font-mono font-bold text-slate-200 hover:border-cyan-500/50 hover:text-cyan-300 cursor-pointer transition">
                  <Upload className="h-3.5 w-3.5 text-cyan-400" />
                  <span>{lang === 'fr' ? 'Importer .CSV (Fuelio)' : 'Import .CSV (Fuelio)'}</span>
                  <input
                    type="file"
                    accept=".csv,text/csv"
                    onChange={handleImportFuelioCsvFile}
                    className="hidden"
                  />
                </label>
              </div>
            </div>
          </div>

          {/* Card 4: Download Compiled Android APK */}
          <div className="rounded-xl border border-sky-500/30 p-5 bg-gradient-to-r from-sky-500/10 via-slate-900/80 to-emerald-500/10 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 md:col-span-2">
            <div>
              <div className="flex items-center gap-2 font-mono font-bold text-slate-100 text-sm">
                <Smartphone className="h-4 w-4 text-sky-400" />
                <span>
                  {lang === 'fr'
                    ? 'Application Mobile Android (Net-CAR v2.0.0 .APK)'
                    : 'Android Mobile Application (Net-CAR v2.0.0 .APK)'}
                </span>
              </div>
              <p className="mt-1.5 text-xs text-slate-300 font-sans max-w-2xl">
                {lang === 'fr'
                  ? 'Téléchargez le fichier .APK compilé pour Android avec le nouveau menu latéral (Side Drawer), le thème sombre moderne et la synchronisation Cloud Firebase.'
                  : 'Download the compiled Android .APK featuring the new Material 3 Side Drawer main menu, modern dark telemetry theme, and Firebase Cloud synchronization.'}
              </p>
            </div>
            <a
              href="/Net-CAR.apk"
              download="Net-CAR.apk"
              className="flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-sky-500 to-emerald-500 px-4 py-2.5 text-xs font-mono font-bold text-slate-950 shadow-md hover:opacity-95 transition shrink-0"
            >
              <Download className="h-4 w-4" />
              <span>{lang === 'fr' ? 'Télécharger Net-CAR.apk' : 'Download Net-CAR.apk'}</span>
            </a>
          </div>
        </div>
      </div>

      {/* Section 3: User Profile */}
      <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-6 sm:p-8 shadow-lg space-y-6">
        <h2 className="text-base font-bold text-slate-100 font-mono tracking-tight flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-purple-500/15 border border-purple-500/30 text-purple-400">
            <User className="h-5 w-5" />
          </div>
          <span>{t.settings.profile}</span>
        </h2>

        {profileSuccess && (
          <div className="flex items-center gap-2 rounded-xl bg-emerald-500/15 border border-emerald-500/30 p-3 text-xs font-mono font-semibold text-emerald-300">
            <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
            <span>
              {lang === 'fr'
                ? 'Profil mis à jour avec succès !'
                : 'Profile information updated successfully!'}
            </span>
          </div>
        )}

        <form onSubmit={handleSaveProfile} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                {lang === 'fr' ? 'Nom Complet' : 'Full Name'}
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
              />
            </div>

            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                {lang === 'fr' ? 'Adresse E-mail' : 'Email Address'}
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3.5 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
              />
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2.5 text-xs font-mono font-bold text-slate-950 shadow-md hover:from-amber-400 hover:to-amber-500 transition cursor-pointer"
            >
              <Save className="h-4 w-4" />
              <span>{t.common.save}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}