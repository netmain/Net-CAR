'use client';

import React, { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useApp } from '@/lib/app-context';
import {
  Car,
  Plus,
  Menu,
  X,
  LayoutDashboard,
  Fuel,
  Receipt,
  Wrench,
  Bell,
  BarChart3,
  Settings,
  LogOut,
  Cloud,
  RefreshCw,
  Download,
} from 'lucide-react';

export function Navbar() {
  const pathname = usePathname();
  const {
    lang,
    setLang,
    t,
    vehicles,
    activeVehicle,
    setActiveVehicleId,
    user,
    syncStatus,
    signOut,
    setAuthModalOpen,
    setAddVehicleModalOpen,
    reminders,
  } = useApp();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const selectRef = useRef<HTMLSelectElement>(null);

  // If the user changed the native <select> before hydration finished, sync its DOM value immediately
  useEffect(() => {
    const el = selectRef.current;
    if (el && el.value && activeVehicle && el.value !== activeVehicle.id) {
      setActiveVehicleId(el.value);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const dueRemindersCount = reminders.filter(
    (r) => r.isActive && (!activeVehicle || r.vehicleId === activeVehicle.id)
  ).length;

  const navItems = [
    { href: '/', label: t.nav.dashboard, icon: LayoutDashboard },
    { href: '/vehicles', label: t.nav.vehicles, icon: Car },
    { href: '/fuel', label: t.nav.fuel, icon: Fuel },
    { href: '/expenses', label: t.nav.expenses, icon: Receipt },
    { href: '/maintenance', label: t.nav.maintenance, icon: Wrench },
    {
      href: '/reminders',
      label: t.nav.reminders,
      icon: Bell,
      badge: dueRemindersCount > 0 ? dueRemindersCount : undefined,
    },
    { href: '/reports', label: t.nav.reports, icon: BarChart3 },
    { href: '/settings', label: t.nav.settings, icon: Settings },
  ];

  return (
    <>
      <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-slate-800/80 bg-[#090D16]/90 backdrop-blur-xl px-4 sm:px-6 shadow-lg shadow-black/20">
        {/* Brand & Active Vehicle */}
        <div className="flex items-center gap-3 sm:gap-4">
          {/* Mobile hamburger button */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden flex h-9 w-9 items-center justify-center rounded-xl border border-slate-800 bg-slate-900 text-slate-300 hover:text-sky-400 hover:border-sky-500/50 transition"
            aria-label="Toggle menu"
          >
            {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>

          {/* High-Performance Crest Logo */}
          <Link href="/" className="flex items-center gap-2.5 group">
            <div className="relative flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-sky-500 via-blue-600 to-indigo-600 text-white shadow-md shadow-sky-500/20">
              <Car className="h-5 w-5" />
              <div className="absolute inset-0 rounded-xl border border-white/25 pointer-events-none" />
            </div>
            <div className="hidden sm:flex flex-col">
              <span className="font-black text-lg tracking-tight bg-gradient-to-r from-white via-sky-100 to-sky-400 bg-clip-text text-transparent">
                Net-CAR
              </span>
              <span className="text-[9px] font-extrabold uppercase tracking-widest text-sky-400/90 -mt-1">
                Telemetry OS
              </span>
            </div>
          </Link>

          {/* Vehicle Selector Dropdown */}
          {vehicles.length > 0 && (
            <div className="relative flex items-center ml-1 sm:ml-2">
              <label htmlFor="vehicle-select" className="sr-only">
                {t.common.selectVehicle}
              </label>
              <div className="flex items-center gap-2 rounded-xl border border-slate-700/80 bg-[#111827] px-3 py-1.5 text-xs sm:text-sm font-semibold text-slate-200 hover:border-sky-500/60 transition max-w-[210px] sm:max-w-none shadow-inner">
                <div className="h-2 w-2 rounded-full bg-emerald-400 shrink-0 shadow-sm shadow-emerald-400/50" />
                <select
                  ref={selectRef}
                  id="vehicle-select"
                  aria-label={t.common.selectVehicle}
                  value={activeVehicle?.id || ''}
                  onChange={(e) => setActiveVehicleId(e.target.value)}
                  onInput={(e) => setActiveVehicleId((e.target as HTMLSelectElement).value)}
                  className="bg-transparent font-bold text-slate-100 outline-none cursor-pointer truncate pr-2"
                >
                  {vehicles.map((v) => (
                    <option
                      key={v.id}
                      value={v.id}
                      className="bg-[#111827] text-slate-100 py-1"
                    >
                      {v.make} {v.model} {v.plate ? `(${v.plate})` : ''}
                    </option>
                  ))}
                </select>
              </div>
              <button
                type="button"
                onClick={() => setAddVehicleModalOpen(true)}
                className="ml-2 flex items-center justify-center h-8 w-8 rounded-xl border border-slate-800 bg-slate-900/80 text-slate-400 hover:text-sky-400 hover:border-sky-500/40 transition shadow-sm cursor-pointer"
                title={t.vehicles.addVehicle}
                aria-label={t.vehicles.addVehicle}
              >
                <Plus className="h-4 w-4" />
              </button>
            </div>
          )}
        </div>

        {/* Right Controls: APK Download, Sync Badge, Language Switcher, Profile / Auth */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Direct Android APK Download Button */}
          <a
            href="/api/download-apk"
            download="Net-CAR.apk"
            className="flex items-center gap-1.5 rounded-xl border border-emerald-500/40 bg-emerald-500/15 px-2.5 py-1 text-[11px] font-mono font-bold text-emerald-300 hover:bg-emerald-500/25 transition cursor-pointer"
            title={
              lang === 'fr'
                ? 'Télécharger la dernière version de Net-CAR.apk (v2.2.0)'
                : 'Download the latest Net-CAR.apk (v2.2.0)'
            }
          >
            <Download className="h-3.5 w-3.5 text-emerald-400 shrink-0" />
            <span className="hidden md:inline">
              {lang === 'fr' ? 'Télécharger Net-CAR.apk' : 'Download Net-CAR.apk'}
            </span>
            <span className="md:hidden">APK</span>
          </a>

          {/* Cloud Sync Status Indicator */}
          {user && !user.isDemo ? (
            <Link
              href="/settings"
              className={`hidden sm:flex items-center gap-1.5 rounded-xl border px-2.5 py-1 text-[11px] font-mono font-bold transition ${
                syncStatus === 'syncing'
                  ? 'border-sky-500/40 bg-sky-500/15 text-sky-300'
                  : syncStatus === 'error'
                    ? 'border-rose-500/40 bg-rose-500/15 text-rose-300'
                    : 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/15'
              }`}
              title={
                syncStatus === 'syncing'
                  ? lang === 'fr'
                    ? 'Synchronisation Cloud en cours...'
                    : 'Cloud synchronization in progress...'
                  : lang === 'fr'
                    ? 'Synchronisation Firebase Cloud active entre vos appareils'
                    : 'Real-time Firebase Cloud sync active across devices'
              }
            >
              {syncStatus === 'syncing' ? (
                <RefreshCw className="h-3.5 w-3.5 animate-spin text-sky-400" />
              ) : (
                <Cloud
                  className={`h-3.5 w-3.5 ${
                    syncStatus === 'error' ? 'text-rose-400' : 'text-emerald-400'
                  }`}
                />
              )}
              <span>
                {syncStatus === 'syncing'
                  ? lang === 'fr'
                    ? 'Synchronisation...'
                    : 'Syncing...'
                  : syncStatus === 'error'
                    ? lang === 'fr'
                      ? 'Erreur Sync'
                      : 'Sync Error'
                    : 'Cloud Sync'}
              </span>
            </Link>
          ) : (
            <button
              onClick={() => setAuthModalOpen(true)}
              className="hidden sm:flex items-center gap-1.5 rounded-xl border border-amber-500/30 bg-amber-500/10 px-2.5 py-1 text-[11px] font-mono font-bold text-amber-300 hover:bg-amber-500/20 transition cursor-pointer"
              title={
                lang === 'fr'
                  ? 'Connectez-vous pour synchroniser vos véhicules entre vos appareils'
                  : 'Sign in to synchronize your vehicles across devices'
              }
            >
              <Cloud className="h-3.5 w-3.5 text-amber-400" />
              <span>{lang === 'fr' ? 'Activer Sync Cloud' : 'Enable Cloud Sync'}</span>
            </button>
          )}

          {/* Language Switcher - Rocker Pill */}
          <div className="flex items-center rounded-xl border border-slate-800 bg-[#121826]/90 p-1 text-xs font-bold shadow-inner">
            <button
              onClick={() => setLang('en')}
              className={`px-2.5 py-1 rounded-lg transition-all ${
                lang === 'en'
                  ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-slate-950 font-black shadow-md shadow-amber-500/20'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              EN
            </button>
            <button
              onClick={() => setLang('fr')}
              className={`px-2.5 py-1 rounded-lg transition-all ${
                lang === 'fr'
                  ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-slate-950 font-black shadow-md shadow-amber-500/20'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              FR
            </button>
          </div>

          {/* User Account / Sign In */}
          {user ? (
            <div className="flex items-center gap-2 pl-1 sm:pl-2">
              <Link
                href="/settings"
                className="flex items-center gap-2 rounded-xl border border-slate-800 bg-[#121826]/90 p-1 pr-2 sm:pr-3 hover:border-slate-700 transition"
              >
                <div className="flex h-7 w-7 sm:h-8 sm:w-8 items-center justify-center rounded-lg bg-gradient-to-br from-amber-400/20 to-orange-500/20 border border-amber-500/30 text-amber-400 font-extrabold text-xs">
                  {user.email ? user.email.slice(0, 2).toUpperCase() : 'KM'}
                </div>
                <span className="hidden md:inline text-xs font-semibold text-slate-300 truncate max-w-[120px]">
                  {user.email}
                </span>
              </Link>
              <button
                onClick={signOut}
                title={t.nav.logout}
                aria-label={t.nav.logout}
                className="flex h-8 w-8 items-center justify-center rounded-xl border border-slate-800/80 text-slate-400 hover:bg-red-500/10 hover:border-red-500/40 hover:text-red-400 transition"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => setAuthModalOpen(true)}
              className="rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 px-3.5 py-1.5 text-xs font-mono font-bold text-slate-950 shadow-md shadow-orange-500/20 hover:opacity-95 transition"
            >
              {t.auth.signInBtn}
            </button>
          )}
        </div>
      </header>

      {/* Mobile Dropdown Navigation Drawer */}
      {mobileMenuOpen && (
        <div className="fixed inset-x-0 top-16 z-20 border-b border-slate-800 bg-[#0B0F19]/95 backdrop-blur-2xl p-4 shadow-2xl md:hidden animate-in fade-in slide-in-from-top-2 duration-150">
          <nav className="space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive =
                pathname === item.href ||
                (item.href !== '/' && pathname.startsWith(item.href));

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`flex items-center justify-between rounded-xl px-3.5 py-2.5 text-sm font-semibold transition ${
                    isActive
                      ? 'bg-amber-500/15 border-l-2 border-amber-500 text-amber-400 font-bold'
                      : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <Icon className="h-4 w-4" />
                    <span>{item.label}</span>
                  </div>
                  {item.badge !== undefined && (
                    <span className="flex h-5 w-5 items-center justify-center rounded-full bg-red-500/20 border border-red-500/40 text-red-400 text-[11px] font-bold">
                      {item.badge}
                    </span>
                  )}
                </Link>
              );
            })}
          </nav>
        </div>
      )}
    </>
  );
}
