'use client';

import { useEffect, useState } from 'react';
import { Theme } from '@astryxdesign/core/theme';
import { themes, defaultThemeSlug } from '@/astryx-themes';

type Mode = 'light' | 'dark';

/**
 * Bridges the app's `data-theme` rocker to Astryx. The toggle writes the
 * attribute directly, so observe it instead of duplicating theme state.
 *
 * Theme is mounted after hydration: its root sync writes `data-theme` on
 * <html>, and mounting it with a guessed mode would overwrite the boot
 * script's light choice before we could read it.
 */
export function AstryxThemeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setMode] = useState<Mode | null>(null);

  useEffect(() => {
    const root = document.documentElement;
    const read = () => setMode(root.dataset.theme === 'light' ? 'light' : 'dark');
    read();
    const observer = new MutationObserver(read);
    observer.observe(root, { attributes: true, attributeFilter: ['data-theme'] });
    return () => observer.disconnect();
  }, []);

  if (mode === null) return <>{children}</>;

  return (
    <Theme theme={themes[defaultThemeSlug]} mode={mode}>
      {children}
    </Theme>
  );
}
