'use client';

import React, { useState } from 'react';
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
  Flame,
  ShieldCheck,
  ChevronDown
} from 'lucide-react';

export function Navbar() {
  const pathname = usePathname();
  const { lang, setLang, t, vehicles, activeVehicle, setActiveVehicleId, user, signOut, setAuthModalOpen, reminders } = useApp();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const dueRemindersCount = reminders.filter(r => r.isActive).length;

  const navItems = [
    { href: '/', label: t.nav.dashboard, icon: LayoutDashboard },
    { href: '/vehicles', label: t.nav.vehicles, icon: Car },
    { href: '/fuel', label: t.nav.fuel, icon: Fuel },
    { href: '/expenses', label: t.nav.expenses, icon: Receipt },
    { href: '/maintenance', label: t.nav.maintenance, icon: Wrench },
    { href: '/reminders', label: t.nav.reminders, icon: Bell, badge: dueRemindersCount > 0 ? dueRemindersCount : undefined },
    { href: '/reports', label: t.nav.reports, icon: BarChart3 },
    { href: '/settings', label: t.nav.settings, icon: Settings },
  ];

  return (
    <>
      <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-slate-800/80 bg-[#0B0F19]/90 px-4 backdrop-blur-xl sm:px-6 shadow-2xl">
        {/* Brand & Active Vehicle */}
        <div className="flex items-center gap-3 sm:gap-4">
          {/* Mobile hamburger button */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden flex h-9 w-9 items-center justify-center rounded-xl border border-slate-800 bg-slate-900/80 text-slate-300 hover:text-amber-400 hover:border-amber-500/50 transition"
            aria-label="Toggle menu"
          >
            {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>

          {/* High-Performance Crest Logo */}
          <Link href="/" className="flex items-center gap-2.5 group">
            <div className="relative flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-amber-500 via-orange-600 to-red-600 text-white shadow-lg shadow-orange-500/20 group-hover:shadow-orange-500/40 transition-all duration-300">
              <Car className="h-5 w-5 transition-transform duration-300 group-hover:scale-110" />
              <div className="absolute inset-0 rounded-xl border border-white/25 pointer-events-none" />
            </div>
            <div className="hidden sm:flex flex-col">
              <span className="font-black text-lg tracking-tight bg-gradient-to-r from-white via-slate-100 to-amber-400 bg-clip-text text-transparent">
                Net-CAR
              </span>
              <span className="text-[9px] font-extrabold uppercase tracking-widest text-amber-500/80 -mt-1">
                Telemetry OS
              </span>
            </div>
          </Link>

          {/* Vehicle Selector Dropdown */}
          {vehicles.length > 0 && (
            <div className="relative flex items-center ml-1 sm:ml-2">
              <label htmlFor="vehicle-select" className="sr-only">{t.common.selectVehicle}</label>
              <div className="flex items-center gap-2 rounded-xl border border-slate-800 bg-[#121826]/90 px-3 py-1.5 text-xs sm:text-sm font-semibold text-slate-200 shadow-inner hover:border-slate-700 transition max-w-[200px] sm:max-w-none">
                <div className="h-2 w-2 rounded-full bg-emerald-400 shadow-sm shadow-emerald-400/80 animate-pulse shrink-0" />
                <select
                  id="vehicle-select"
                  value={activeVehicle?.id || ''}
                  onChange={(e) => setActiveVehicleId(e.target.value)}
                  className="bg-transparent font-bold text-slate-200 outline-none cursor-pointer truncate"
                >
                  {vehicles.map((v) => (
                    <option key={v.id} value={v.id} className="bg-[#121826] text-slate-200">
                      {v.make} {v.model} {v.plate ? `(${v.plate})` : ''}
                    </option>
                  ))}
                </select>
              </div>
              <Link
                href="/vehicles/new"
                className="ml-2 hidden lg:flex items-center justify-center h-8 w-8 rounded-xl border border-slate-800 bg-slate-900/60 text-slate-400 hover:text-amber-400 hover:border-amber-500/40 transition shadow-sm"
                title={t.vehicles.addVehicle}
              >
                <Plus className="h-4 w-4" />
              </Link>
            </div>
          )}
        </div>

        {/* Right Controls: Language Switcher, Currency, Profile */}
        <div className="flex items-center gap-2 sm:gap-3">
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
              const isActive = pathname === item.href || (item.href !== '/' && pathname.startsWith(item.href));

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
