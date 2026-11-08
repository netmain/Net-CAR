'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useApp } from '@/lib/app-context';
import {
  LayoutDashboard,
  Car,
  Fuel,
  Receipt,
  Wrench,
  Bell,
  BarChart3,
  Settings,
  Radio,
  Cloud,
} from 'lucide-react';

export function Sidebar() {
  const pathname = usePathname();
  const { t, lang, reminders, activeVehicle, user, syncStatus, setAuthModalOpen } = useApp();

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

  const isCloudConnected = Boolean(user && !user.isDemo);

  return (
    <aside className="w-64 border-r border-slate-800/80 bg-[#0B0F19]/95 min-h-[calc(100vh-4rem)] flex flex-col justify-between py-6 px-3.5 backdrop-blur-xl">
      <div className="space-y-1">
        <div className="px-3 pb-3 flex items-center justify-between text-[10px] font-black uppercase tracking-widest text-slate-500">
          <span>Cockpit</span>
          <span className="flex items-center gap-1 text-[9px] text-emerald-400 font-bold lowercase">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-ping" />
            live
          </span>
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive =
            pathname === item.href ||
            (item.href !== '/' && pathname.startsWith(item.href));

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`group flex items-center justify-between rounded-xl px-3.5 py-2.5 text-sm font-semibold transition-all duration-150 ${
                isActive
                  ? 'bg-gradient-to-r from-amber-500/15 via-orange-500/5 to-transparent text-amber-400 border-l-2 border-amber-500 shadow-sm'
                  : 'text-slate-400 hover:bg-slate-800/40 hover:text-slate-200'
              }`}
            >
              <div className="flex items-center gap-3">
                <Icon
                  className={`h-4 w-4 transition-colors ${
                    isActive
                      ? 'text-amber-400'
                      : 'text-slate-500 group-hover:text-slate-300'
                  }`}
                />
                <span>{item.label}</span>
              </div>
              {item.badge !== undefined && (
                <span
                  className={`flex h-5 w-5 items-center justify-center rounded-full text-[11px] font-black ${
                    isActive
                      ? 'bg-amber-400 text-slate-950'
                      : 'bg-red-500/20 text-red-400 border border-red-500/30'
                  }`}
                >
                  {item.badge}
                </span>
              )}
            </Link>
          );
        })}
      </div>

      {/* Cloud & Telemetry Sync Widget */}
      <div className="rounded-2xl border border-slate-800/80 bg-[#121826]/80 p-3.5 mx-1 shadow-inner relative overflow-hidden group">
        <div className="absolute top-0 right-0 h-16 w-16 bg-amber-500/5 rounded-full blur-xl pointer-events-none" />
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 font-bold text-xs text-slate-200">
            <Radio className="h-3.5 w-3.5 text-amber-400" />
            <span>{isCloudConnected ? 'Firebase Cloud Sync' : 'Telemetry Link'}</span>
          </div>
          <span
            className={`h-2 w-2 rounded-full shadow-sm animate-pulse ${
              isCloudConnected
                ? 'bg-emerald-400 shadow-emerald-400/80'
                : 'bg-amber-400 shadow-amber-400/80'
            }`}
          />
        </div>
        <p className="mt-1.5 text-[11px] text-slate-400 leading-relaxed font-medium">
          {isCloudConnected
            ? lang === 'fr'
              ? `Synchronisé en temps réel (${syncStatus === 'syncing' ? 'en cours...' : 'actif'}) pour ${activeVehicle ? `${activeVehicle.make} ${activeVehicle.model}` : 'votre flotte'}.`
              : `Real-time sync active for ${activeVehicle ? `${activeVehicle.make} ${activeVehicle.model}` : 'your fleet'}.`
            : lang === 'fr'
              ? `Mode local pour ${activeVehicle ? `${activeVehicle.make} ${activeVehicle.model}` : 'vos véhicules'}. Connectez-vous pour synchroniser vos appareils.`
              : `Local mode for ${activeVehicle ? `${activeVehicle.make} ${activeVehicle.model}` : 'your vehicles'}. Sign in to sync across devices.`}
        </p>
        {!isCloudConnected && (
          <button
            onClick={() => setAuthModalOpen(true)}
            className="mt-2.5 w-full flex items-center justify-center gap-1.5 rounded-xl bg-amber-500/15 border border-amber-500/30 px-3 py-1.5 text-[11px] font-mono font-bold text-amber-300 hover:bg-amber-500/25 transition"
          >
            <Cloud className="h-3.5 w-3.5 text-amber-400" />
            <span>{lang === 'fr' ? 'Connecter Gmail / Email' : 'Connect Gmail / Email'}</span>
          </button>
        )}
      </div>
    </aside>
  );
}
