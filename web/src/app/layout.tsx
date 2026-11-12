import type { Metadata } from 'next';
import './globals.css';
import { AppProvider } from '@/lib/app-context';
import { AppShell } from '@/components/app-shell';

export const metadata: Metadata = {
  title: 'Net-CAR — Performance Vehicle Management & Telemetry',
  description: 'Smart vehicle management, fuel economy tracking, expense monitoring, and scheduled maintenance reminders.',
  // The cockpit UI is dark-only: declare it before first paint so the
  // browser never shows a white flash or light native widgets.
  other: {
    'color-scheme': 'dark',
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="h-full antialiased dark">
      <body className="min-h-full bg-[#080b11] text-slate-100 font-sans selection:bg-amber-500 selection:text-black">
        <AppProvider>
          <AppShell>{children}</AppShell>
        </AppProvider>
      </body>
    </html>
  );
}
