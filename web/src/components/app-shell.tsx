'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useApp } from '@/lib/app-context';
import { Navbar } from './navbar';
import { Sidebar } from './sidebar';
import { AuthModal } from './auth-modal';
import { AddVehicleModal } from './add-vehicle-modal';
import { 
  LayoutDashboard, 
  Fuel, 
  Receipt, 
  Wrench, 
  Bell 
} from 'lucide-react';

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { t, reminders, activeVehicle } = useApp();
  const dueCount = reminders.filter(
    (r) => r.isActive && (!activeVehicle || r.vehicleId === activeVehicle.id)
  ).length;

  const mobileNav = [
    { href: '/', label: t.nav.dashboard, icon: LayoutDashboard },
    { href: '/fuel', label: t.nav.fuel, icon: Fuel },
    { href: '/expenses', label: t.nav.expenses, icon: Receipt },
    { href: '/maintenance', label: t.nav.maintenance, icon: Wrench },
    { href: '/reminders', label: t.nav.reminders, icon: Bell, badge: dueCount > 0 ? dueCount : undefined },
  ];

  return (
    <div className="min-h-screen bg-[#080b11] text-slate-100 flex flex-col font-sans pb-16 md:pb-0 carbon-grid">
      <Navbar />
      <AuthModal />
      <AddVehicleModal />
      <div className="flex flex-1">
        <div className="hidden md:block">
          <Sidebar />
        </div>
        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          {children}
        </main>
      </div>

      {/* Mobile Sticky Bottom Telemetry Bar */}
      <nav className="fixed bottom-0 inset-x-0 z-30 bg-[#0B0F19]/95 backdrop-blur-xl border-t border-slate-800/80 py-1.5 px-2 flex justify-around items-center md:hidden shadow-2xl">
        {mobileNav.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href || (item.href !== '/' && pathname.startsWith(item.href));

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex flex-col items-center justify-center py-1 px-2.5 rounded-xl text-[10px] font-bold transition-all relative ${
                isActive ? 'text-amber-400' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <div className="relative">
                <Icon className={`h-5 w-5 ${isActive ? 'text-amber-400 drop-shadow-[0_0_8px_rgba(245,158,11,0.5)]' : 'text-slate-400'}`} />
                {item.badge !== undefined && (
                  <span className="absolute -top-1 -right-2 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-slate-950 text-[9px] font-black">
                    {item.badge}
                  </span>
                )}
              </div>
              <span className="mt-1">{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
