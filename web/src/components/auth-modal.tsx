'use client';

import React, { useState } from 'react';
import { useApp } from '@/lib/app-context';
import { 
  X, 
  Mail, 
  Lock, 
  User, 
  Car, 
  CheckCircle2, 
  AlertCircle, 
  ArrowRight,
  Sparkles,
  ShieldCheck,
  Eye,
  EyeOff
} from 'lucide-react';

export function AuthModal() {
  const { 
    authModalOpen, 
    setAuthModalOpen, 
    signInWithEmail, 
    signUpWithEmail, 
    signInWithGoogle, 
    signInDemo, 
    lang, 
    t 
  } = useApp();

  const [mode, setMode] = useState<'signin' | 'signup'>('signin');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  if (!authModalOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!email.trim() || !email.includes('@')) {
      setErrorMessage(lang === 'fr' ? 'Veuillez saisir une adresse e-mail valide.' : 'Please enter a valid email address.');
      return;
    }

    if (!password || password.length < 6) {
      setErrorMessage(t.auth.errorWeakPassword);
      return;
    }

    if (mode === 'signup') {
      if (password !== confirmPassword) {
        setErrorMessage(t.auth.errorPasswordMismatch);
        return;
      }
    }

    setLoading(true);
    try {
      if (mode === 'signin') {
        const res = await signInWithEmail(email.trim(), password);
        if (res.success) {
          setSuccessMessage(lang === 'fr' ? 'Connexion réussie ! Bienvenue sur Net-CAR.' : 'Successfully signed in! Welcome to Net-CAR.');
          setTimeout(() => {
            setAuthModalOpen(false);
            setLoading(false);
          }, 600);
        } else {
          setErrorMessage(res.error || t.auth.errorInvalidAuth);
          setLoading(false);
        }
      } else {
        const res = await signUpWithEmail(email.trim(), password, fullName.trim());
        if (res.success) {
          setSuccessMessage(lang === 'fr' ? 'Compte créé avec succès ! Bienvenue à bord.' : 'Account created successfully! Welcome aboard.');
          setTimeout(() => {
            setAuthModalOpen(false);
            setLoading(false);
          }, 600);
        } else {
          setErrorMessage(res.error || (lang === 'fr' ? 'Erreur lors de la création du compte.' : 'Failed to create account.'));
          setLoading(false);
        }
      }
    } catch (err: any) {
      setErrorMessage(err.message || t.auth.errorInvalidAuth);
      setLoading(false);
    }
  };

  const handleGoogleClick = async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const res = await signInWithGoogle();
      if (res.success) {
        setSuccessMessage(lang === 'fr' ? 'Connexion Google réussie !' : 'Google sign-in successful!');
        setTimeout(() => {
          setAuthModalOpen(false);
          setLoading(false);
        }, 600);
      } else {
        setErrorMessage(res.error || 'Google Sign-In failed.');
        setLoading(false);
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Google Sign-In failed.');
      setLoading(false);
    }
  };

  const handleDemoClick = () => {
    signInDemo();
    setSuccessMessage(lang === 'fr' ? 'Connecté avec le compte Démo Net-CAR !' : 'Signed in as Net-CAR Demo driver!');
    setTimeout(() => {
      setAuthModalOpen(false);
    }, 500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-md overflow-y-auto animate-in fade-in duration-200">
      <div className="relative w-full max-w-md rounded-2xl bg-[#0F1522] border border-slate-800 p-6 sm:p-8 shadow-2xl shadow-black/80 animate-in zoom-in-95 duration-150">
        {/* Close Button */}
        <button
          onClick={() => setAuthModalOpen(false)}
          className="absolute right-4 top-4 rounded-xl p-1.5 text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition"
        >
          <X className="h-5 w-5" />
        </button>

        {/* Brand Crest */}
        <div className="flex items-center gap-3 mb-5">
          <div className="relative flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-amber-500 via-orange-600 to-red-600 text-white shadow-lg shadow-orange-500/25">
            <Car className="h-6 w-6" />
            <div className="absolute inset-0 rounded-xl border border-white/20 pointer-events-none" />
          </div>
          <div>
            <h2 className="text-xl font-extrabold tracking-tight text-slate-100 font-mono">
              Net-CAR <span className="text-amber-400 text-xs uppercase tracking-widest font-bold">Auth</span>
            </h2>
            <p className="text-xs text-slate-400">
              {mode === 'signin' ? t.auth.welcomeSubtitle : t.auth.createAccountSubtitle}
            </p>
          </div>
        </div>

        {/* Mode Tabs */}
        <div className="flex rounded-xl border border-slate-800 bg-slate-900/90 p-1 mb-5">
          <button
            type="button"
            onClick={() => { setMode('signin'); setErrorMessage(null); }}
            className={`flex-1 rounded-lg py-2 text-xs font-mono font-bold transition ${
              mode === 'signin'
                ? 'bg-amber-500 text-slate-950 shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            {t.auth.signInBtn}
          </button>
          <button
            type="button"
            onClick={() => { setMode('signup'); setErrorMessage(null); }}
            className={`flex-1 rounded-lg py-2 text-xs font-mono font-bold transition ${
              mode === 'signup'
                ? 'bg-amber-500 text-slate-950 shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            {t.auth.signUpBtn}
          </button>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="mb-4 flex items-center gap-2.5 rounded-xl bg-red-500/15 border border-red-500/30 p-3 text-xs font-mono text-red-300">
            <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Success Alert */}
        {successMessage && (
          <div className="mb-4 flex items-center gap-2.5 rounded-xl bg-emerald-500/15 border border-emerald-500/30 p-3 text-xs font-mono text-emerald-300">
            <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Google Sign In Button */}
        <button
          type="button"
          onClick={handleGoogleClick}
          disabled={loading}
          className="w-full flex items-center justify-center gap-3 rounded-xl border border-slate-700 bg-slate-900/80 px-4 py-2.5 text-xs font-mono font-bold text-slate-200 shadow-sm hover:bg-slate-800 hover:border-slate-600 hover:text-white transition"
        >
          {/* Google Logo SVG */}
          <svg className="h-4 w-4" viewBox="0 0 24 24">
            <path
              fill="#4285F4"
              d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
            />
            <path
              fill="#34A853"
              d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            />
            <path
              fill="#FBBC05"
              d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
            />
            <path
              fill="#EA4335"
              d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
            />
          </svg>
          <span>{t.auth.googleSignIn}</span>
        </button>

        {/* Divider */}
        <div className="relative my-4">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-800" />
          </div>
          <div className="relative flex justify-center text-[11px] font-mono uppercase tracking-wider text-slate-500">
            <span className="bg-[#0F1522] px-3">{t.auth.orDivider}</span>
          </div>
        </div>

        {/* Email & Password Form */}
        <form onSubmit={handleSubmit} className="space-y-3.5">
          {mode === 'signup' && (
            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
                {t.auth.fullName}
              </label>
              <div className="relative">
                <User className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
                <input
                  type="text"
                  placeholder="e.g. Karim Mansouri"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 pl-10 pr-4 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
              {t.auth.email} *
            </label>
            <div className="relative">
              <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
              <input
                type="email"
                required
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full rounded-xl border border-slate-700 bg-slate-900/90 pl-10 pr-4 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
              {t.auth.password} *
            </label>
            <div className="relative">
              <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
              <input
                type={showPassword ? 'text' : 'password'}
                required
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full rounded-xl border border-slate-700 bg-slate-900/90 pl-10 pr-10 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>

          {mode === 'signup' && (
            <div>
              <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1">
                {t.auth.confirmPassword} *
              </label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  placeholder="••••••••"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 pl-10 pr-4 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-2 flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2.5 text-sm font-mono font-bold text-slate-950 shadow-lg shadow-amber-500/25 hover:from-amber-400 hover:to-amber-500 disabled:opacity-50 transition"
          >
            <span>{loading ? t.common.loading : mode === 'signin' ? t.auth.signInBtn : t.auth.signUpBtn}</span>
            <ArrowRight className="h-4 w-4" />
          </button>
        </form>

        {/* Demo Fast Login Banner */}
        <div className="mt-5 pt-4 border-t border-slate-800 text-center">
          <p className="text-xs text-slate-400 mb-2 font-mono">
            {lang === 'fr' ? 'Besoin de tester immédiatement sans compte ?' : 'Want to test instantly without creating an account?'}
          </p>
          <button
            type="button"
            onClick={handleDemoClick}
            className="inline-flex items-center gap-1.5 rounded-lg border border-cyan-500/30 bg-cyan-500/10 px-3.5 py-1.5 text-xs font-mono font-bold text-cyan-300 hover:bg-cyan-500/20 transition"
          >
            <Sparkles className="h-3.5 w-3.5 text-cyan-400" />
            <span>{lang === 'fr' ? 'Connexion Démo 1-Clic' : '1-Click Instant Demo Access'}</span>
          </button>
        </div>

        {/* Cloud sync footnote */}
        <p className="mt-4 text-[10px] text-center text-slate-500 font-mono">
          {t.auth.driveBackupNote}
        </p>
      </div>
    </div>
  );
}