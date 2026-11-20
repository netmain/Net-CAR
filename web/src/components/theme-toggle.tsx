'use client';

import { Moon, Sun } from 'lucide-react';
import { useApp } from '@/lib/app-context';

/**
 * Light/dark rocker. The icons are switched by CSS from `data-theme`, so the
 * correct glyph is painted before hydration; this handler only flips state.
 */
export function ThemeToggle() {
  const { lang } = useApp();

  const toggleTheme = () => {
    const root = document.documentElement;
    const next = root.dataset.theme === 'light' ? 'dark' : 'light';
    if (next === 'light') root.dataset.theme = 'light';
    else delete root.dataset.theme;
    try {
      localStorage.setItem('netcar-theme', next);
    } catch {
      /* private mode: keep the in-page choice only */
    }
  };

  const label =
    lang === 'fr' ? 'Basculer entre thème clair et sombre' : 'Toggle light and dark theme';

  return (
    <button
      type="button"
      onClick={toggleTheme}
      title={label}
      aria-label={label}
      className="flex h-8 w-8 items-center justify-center rounded-xl border border-slate-800 bg-slate-800/90 text-slate-400 hover:text-amber-400 hover:border-amber-500/40 transition cursor-pointer"
    >
      <Sun className="theme-icon-dark h-4 w-4" />
      <Moon className="theme-icon-light h-4 w-4" />
    </button>
  );
}
