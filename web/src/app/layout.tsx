import type { Metadata } from 'next';
import './globals.css';
import { AppProvider } from '@/lib/app-context';
import { AppShell } from '@/components/app-shell';

export const metadata: Metadata = {
  title: 'Net-CAR — Performance Vehicle Management & Telemetry',
  description: 'Smart vehicle management, fuel economy tracking, expense monitoring, and scheduled maintenance reminders.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="h-full antialiased" suppressHydrationWarning>
      <head>
        {/* Runs before first paint so a light-theme user never sees the dark
            cockpit flash. Dark is the default; only an explicit light choice
            sets the attribute. */}
        <script
          dangerouslySetInnerHTML={{
            __html: `(function(){try{if(localStorage.getItem('netcar-theme')==='light'){document.documentElement.setAttribute('data-theme','light')}}catch(e){}})()`,
          }}
        />
      </head>
      <body className="min-h-full bg-canvas text-slate-100 font-sans selection:bg-amber-500 selection:text-black">
        <AppProvider>
          <AppShell>{children}</AppShell>
        </AppProvider>
      </body>
    </html>
  );
}
