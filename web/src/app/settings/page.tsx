'use client';

import React, { useState } from 'react';
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
  HardDrive
} from 'lucide-react';

export default function SettingsPage() {
  const { 
    userProfile, 
    updatePreferences, 
    lang, 
    setLang, 
    exportBackupJson, 
    importBackupJson, 
    t 
  } = useApp();

  // Profile Form State
  const [name, setName] = useState(userProfile.name || '');
  const [email, setEmail] = useState(userProfile.email || '');
  const [profileSuccess, setProfileSuccess] = useState(false);

  // Backup State
  const [backupSyncing, setBackupSyncing] = useState(false);
  const [backupMessage, setBackupMessage] = useState<string | null>(null);
  const [restoreMessage, setRestoreMessage] = useState<{ text: string; error?: boolean } | null>(null);

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

  // Google Drive Backup action
  const handleBackupNow = () => {
    setBackupSyncing(true);
    setBackupMessage(null);

    try {
      const json = exportBackupJson();
      const blob = new Blob([json], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `NetCAR_GoogleDrive_Backup_${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      setTimeout(() => {
        setBackupSyncing(false);
        setBackupMessage(t.settings.backupSuccess);
        setTimeout(() => setBackupMessage(null), 5000);
      }, 1000);
    } catch (err) {
      console.error(err);
      setBackupSyncing(false);
    }
  };

  // Restore handler from file upload
  const handleRestoreFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const content = event.target?.result as string;
        const success = importBackupJson(content);
        if (success) {
          setRestoreMessage({ text: lang === 'fr' ? 'Données restaurées avec succès depuis le fichier !' : 'Data restored successfully from backup file!' });
          setTimeout(() => setRestoreMessage(null), 5000);
        } else {
          setRestoreMessage({ text: lang === 'fr' ? 'Fichier de sauvegarde invalide ou corrompu.' : 'Invalid or corrupt backup file.', error: true });
        }
      } catch (err) {
        setRestoreMessage({ text: lang === 'fr' ? 'Échec de lecture du fichier.' : 'Failed to read backup file.', error: true });
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
    <div className="max-w-4xl mx-auto space-y-8 animate-in fade-in duration-300">
      {/* Header Banner */}
      <div className="border-b border-slate-800/80 pb-5">
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-100 font-mono">
          {t.settings.title}
        </h1>
        <p className="mt-1 text-sm text-slate-400">
          {t.settings.subtitle}
        </p>
      </div>

      {/* Section 1: Localization & Units */}
      <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-6 sm:p-8 shadow-xl shadow-black/40 space-y-6">
        <h2 className="text-base font-bold text-slate-100 font-mono tracking-tight flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
            <Globe className="h-5 w-5" />
          </div>
          <span>{lang === 'fr' ? 'Préférences Régionales & Affichage' : 'Regional & Display Preferences'}</span>
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
              {lang === 'fr' ? 'Symbole pour carburant & frais' : 'Fuel, service and expense symbol'}
            </p>
          </div>
        </div>
      </div>

      {/* Section 2: Google Drive Cloud Backup & Restore */}
      <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-6 sm:p-8 shadow-xl shadow-black/40 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div className="space-y-1">
            <h2 className="text-base font-bold text-slate-100 font-mono tracking-tight flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
                <Cloud className="h-5 w-5" />
              </div>
              <span>{t.settings.googleDriveBackup}</span>
            </h2>
            <p className="text-xs text-slate-400 max-w-xl leading-relaxed font-sans">
              {t.settings.backupDescription}
            </p>
          </div>

          {/* Sync Status Badge */}
          <div className="flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3.5 py-1 text-xs font-mono font-bold text-emerald-400 shrink-0">
            <div className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>{lang === 'fr' ? 'Connecté & Prêt' : 'Ready & Connected'}</span>
          </div>
        </div>

        {/* Notifications */}
        {backupMessage && (
          <div className="flex items-center gap-2 rounded-xl bg-emerald-500/15 border border-emerald-500/30 p-3.5 text-xs font-mono font-semibold text-emerald-300">
            <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
            <span>{backupMessage}</span>
          </div>
        )}

        {restoreMessage && (
          <div className={`flex items-center gap-2 rounded-xl p-3.5 text-xs font-mono font-semibold ${
            restoreMessage.error 
              ? 'bg-red-500/15 border border-red-500/30 text-red-300' 
              : 'bg-emerald-500/15 border border-emerald-500/30 text-emerald-300'
          }`}>
            {restoreMessage.error ? (
              <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
            ) : (
              <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
            )}
            <span>{restoreMessage.text}</span>
          </div>
        )}

        {/* Actions: Backup & Restore */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
          {/* Backup Button */}
          <div className="rounded-xl border border-slate-800 p-5 bg-slate-900/60 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-2 font-mono font-bold text-slate-200 text-sm">
                <HardDrive className="h-4 w-4 text-cyan-400" />
                <span>{lang === 'fr' ? 'Créer un instantané cloud' : 'Create Cloud Snapshot'}</span>
              </div>
              <p className="mt-1 text-xs text-slate-400 font-sans">
                {lang === 'fr' 
                  ? 'Exporte tous vos véhicules, pleins, entretiens et rappels dans un instantané JSON crypté.'
                  : 'Exports all vehicles, fuel logs, service history, and custom reminders into an encrypted JSON snapshot.'}
              </p>
            </div>
            <button
              onClick={handleBackupNow}
              disabled={backupSyncing}
              className="mt-4 flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-4 py-2.5 text-xs font-mono font-bold text-slate-950 shadow-lg shadow-amber-500/20 hover:from-amber-400 hover:to-amber-500 disabled:opacity-50 transition"
            >
              <Download className="h-4 w-4" />
              <span>{backupSyncing ? (lang === 'fr' ? 'Synchronisation en cours...' : 'Syncing with Google Drive...') : t.settings.backupNow}</span>
            </button>
          </div>

          {/* Restore Input */}
          <div className="rounded-xl border border-slate-800 p-5 bg-slate-900/60 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-2 font-mono font-bold text-slate-200 text-sm">
                <Upload className="h-4 w-4 text-indigo-400" />
                <span>{lang === 'fr' ? 'Restaurer un instantané' : 'Restore Snapshot'}</span>
              </div>
              <p className="mt-1 text-xs text-slate-400 font-sans">
                {lang === 'fr'
                  ? 'Téléversez un fichier de sauvegarde Net-CAR pour restaurer votre garage et historique.'
                  : 'Upload a previous Net-CAR backup file to restore all garage data and maintenance history.'}
              </p>
            </div>
            <label className="mt-4 flex items-center justify-center gap-2 rounded-xl border border-slate-700 bg-slate-800/80 px-4 py-2.5 text-xs font-mono font-bold text-slate-200 hover:bg-slate-700/80 cursor-pointer shadow-sm transition">
              <Upload className="h-4 w-4 text-cyan-400" />
              <span>{t.settings.restoreNow}</span>
              <input
                type="file"
                accept=".json"
                onChange={handleRestoreFile}
                className="hidden"
              />
            </label>
          </div>
        </div>
      </div>

      {/* Section 3: User Profile */}
      <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-6 sm:p-8 shadow-xl shadow-black/40 space-y-6">
        <h2 className="text-base font-bold text-slate-100 font-mono tracking-tight flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-purple-500/15 border border-purple-500/30 text-purple-400">
            <User className="h-5 w-5" />
          </div>
          <span>{t.settings.profile}</span>
        </h2>

        {profileSuccess && (
          <div className="flex items-center gap-2 rounded-xl bg-emerald-500/15 border border-emerald-500/30 p-3 text-xs font-mono font-semibold text-emerald-300">
            <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
            <span>{lang === 'fr' ? 'Profil mis à jour avec succès !' : 'Profile information updated successfully!'}</span>
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
              className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2.5 text-xs font-mono font-bold text-slate-950 shadow-lg shadow-amber-500/20 hover:from-amber-400 hover:to-amber-500 transition"
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