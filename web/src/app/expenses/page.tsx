'use client';

import React, { useState, useMemo } from 'react';
import { currencySymbolFor, formatMoney } from '@/lib/currency';
import { useApp } from '@/lib/app-context';
import Modal from '@/components/modal';
import { 
  Receipt, 
  Plus, 
  Search, 
  Trash2, 
  Shield, 
  ParkingCircle, 
  Sparkles, 
  AlertTriangle, 
  FileText, 
  CreditCard, 
  Wrench, 
  X, 
  DollarSign, 
  ExternalLink,
  Tag
} from 'lucide-react';

const CATEGORY_ICONS: Record<string, React.ElementType> = {
  insurance: Shield,
  parking: ParkingCircle,
  tolls: Receipt,
  fines: AlertTriangle,
  carWash: Sparkles,
  taxes: FileText,
  registration: FileText,
  financing: CreditCard,
  repairs: Wrench,
  maintenance: Wrench,
  other: Tag,
};

const CATEGORY_COLORS: Record<string, { bg: string; text: string; border: string }> = {
  insurance: { bg: 'bg-cyan-500/15', text: 'text-cyan-400', border: 'border-cyan-500/30' },
  parking: { bg: 'bg-purple-500/15', text: 'text-purple-400', border: 'border-purple-500/30' },
  tolls: { bg: 'bg-amber-500/15', text: 'text-amber-400', border: 'border-amber-500/30' },
  fines: { bg: 'bg-rose-500/15', text: 'text-rose-400', border: 'border-rose-500/30' },
  carWash: { bg: 'bg-teal-500/15', text: 'text-teal-400', border: 'border-teal-500/30' },
  taxes: { bg: 'bg-yellow-500/15', text: 'text-yellow-400', border: 'border-yellow-500/30' },
  registration: { bg: 'bg-emerald-500/15', text: 'text-emerald-400', border: 'border-emerald-500/30' },
  financing: { bg: 'bg-indigo-500/15', text: 'text-indigo-400', border: 'border-indigo-500/30' },
  repairs: { bg: 'bg-red-500/15', text: 'text-red-400', border: 'border-red-500/30' },
  maintenance: { bg: 'bg-blue-500/15', text: 'text-blue-400', border: 'border-blue-500/30' },
  other: { bg: 'bg-slate-500/15', text: 'text-slate-400', border: 'border-slate-500/30' },
};

export default function ExpensesPage() {
  const { 
    activeVehicle, 
    expenses, 
    addExpense, 
    deleteExpense, 
    addCustomExpenseCategory,
    userProfile, 
    lang,
    t 
  } = useApp();

  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);
  const [showCategoryDialog, setShowCategoryDialog] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [categoryError, setCategoryError] = useState('');

  // Form State
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [category, setCategory] = useState<string>('parking');
  const [amount, setAmount] = useState<number | ''>('');
  const [description, setDescription] = useState('');
  const [odometer, setOdometer] = useState<number | ''>(activeVehicle?.currentOdometer || '');
  const [receiptUrl, setReceiptUrl] = useState('');
  const [notes, setNotes] = useState('');
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const currencySymbol = currencySymbolFor(userProfile.currency);
  const distUnit = userProfile.units === 'metric' ? 'km' : 'mi';
  const activeVehicleId = activeVehicle?.id;

  // Filter expenses by active vehicle (memoized)
  const vehicleExpenses = useMemo(() => {
    return expenses
      .filter((e) => !activeVehicleId || e.vehicleId === activeVehicleId)
      .sort((a, b) => b.date.localeCompare(a.date));
  }, [expenses, activeVehicleId]);

  // Filter by category pill & search query (memoized)
  const filteredExpenses = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return vehicleExpenses.filter((e) => {
      const matchesCategory = selectedCategory === 'all' || e.category === selectedCategory;
      if (!matchesCategory) return false;
      if (!q) return true;
      return (
        e.description.toLowerCase().includes(q) ||
        (e.notes && e.notes.toLowerCase().includes(q)) ||
        (t.categories[e.category] && t.categories[e.category].toLowerCase().includes(q))
      );
    });
  }, [vehicleExpenses, selectedCategory, searchQuery, t.categories]);

  // Memoize stats
  const { totalAmount, topCategory, topCategoryAmount } = useMemo(() => {
    let total = 0;
    const categoryTotals: Record<string, number> = {};
    for (const exp of vehicleExpenses) {
      total += exp.amount;
      categoryTotals[exp.category] = (categoryTotals[exp.category] || 0) + exp.amount;
    }
    let bestCat: string | null = null;
    let bestAmount = 0;
    for (const [cat, sum] of Object.entries(categoryTotals)) {
      if ((sum || 0) > bestAmount) {
        bestAmount = sum || 0;
        bestCat = cat;
      }
    }
    return { totalAmount: total, topCategory: bestCat, topCategoryAmount: bestAmount };
  }, [vehicleExpenses]);

  const handleOpenAdd = () => {
    setDate(new Date().toISOString().split('T')[0]);
    setCategory('parking');
    setAmount('');
    setDescription('');
    setOdometer(activeVehicle?.currentOdometer || '');
    setReceiptUrl('');
    setNotes('');
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleSaveExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeVehicle) return;

    const errors: Record<string, string> = {};
    if (!date) errors.date = 'Date is required';
    if (!description.trim()) errors.description = 'Description is required';
    if (amount === '' || Number(amount) <= 0) errors.amount = 'Valid amount is required';

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    try {
      await addExpense({
        vehicleId: activeVehicle.id,
        date: new Date(date).toISOString(),
        category,
        amount: Number(amount),
        description: description.trim(),
        odometer: odometer !== '' ? Number(odometer) : undefined,
        receiptUrl: receiptUrl.trim() || undefined,
        notes: notes.trim() || undefined,
      });

      setIsModalOpen(false);
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    await deleteExpense(id);
    setDeleteConfirmId(null);
  };

  const allCategories = Array.from(
    new Set([
      ...Object.keys(t.categories),
      ...(userProfile.customExpenseCategories || []),
      ...vehicleExpenses.map((e) => e.category).filter(Boolean),
    ])
  );

  const handleCategorySelect = (selected: string) => {
    if (selected === '__add_custom__') {
      setNewCategoryName('');
      setCategoryError('');
      setShowCategoryDialog(true);
      return;
    }
    setCategory(selected);
  };

  const handleAddCategory = () => {
    const clean = newCategoryName.trim().replace(/\s+/g, ' ');
    if (!clean) {
      setCategoryError(lang === 'fr' ? 'Saisissez un nom de catégorie' : 'Enter a category name');
      return;
    }
    addCustomExpenseCategory(clean);
    setCategory(clean);
    setShowCategoryDialog(false);
    setNewCategoryName('');
    setCategoryError('');
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-300">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-100 font-mono">
              {t.expenses.title}
            </h1>
            {activeVehicle && (
              <span className="rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-mono font-bold px-2.5 py-1">
                {activeVehicle.make} {activeVehicle.model} {activeVehicle.plate ? `(${activeVehicle.plate})` : ''}
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-slate-400">
            {t.expenses.subtitle}
          </p>
        </div>

        <button
          onClick={handleOpenAdd}
          disabled={!activeVehicle}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2.5 text-sm font-bold text-slate-950 shadow-lg shadow-amber-500/20 hover:from-amber-400 hover:to-amber-500 disabled:opacity-50 transition"
        >
          <Plus className="h-4 w-4" />
          <span>{t.expenses.addExpense}</span>
        </button>
      </div>

      {/* KPI Cockpit Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {/* Total Expenses */}
        <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-5 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400">
              {t.expenses.totalExpenses}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-400">
              <DollarSign className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-1">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-slate-100">
              {totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
            <span className="text-sm font-bold text-amber-400 font-mono ml-1">{currencySymbol}</span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400 font-mono">
            {vehicleExpenses.length} {lang === 'fr' ? 'dépenses enregistrées' : 'total entries recorded'}
          </p>
        </div>

        {/* Top Expense Category */}
        <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-5 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400">
              {lang === 'fr' ? 'Dépense principale' : 'Top Expense'}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
              <Shield className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-cyan-400 truncate">
              {topCategory ? (t.categories[topCategory] || topCategory) : '—'}
            </span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400 font-mono">
            {topCategory 
              ? `${topCategoryAmount.toFixed(2)} ${currencySymbol} (${Math.round((topCategoryAmount / (totalAmount || 1)) * 100)}%)` 
              : (lang === 'fr' ? 'Aucune dépense' : 'No records yet')}
          </p>
        </div>

        {/* Average Per Entry */}
        <div className="rounded-2xl border border-slate-800 bg-[#0F1522] p-5 shadow-xl shadow-black/40">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400">
              {lang === 'fr' ? 'Coût moyen' : 'Average Cost'}
            </span>
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-purple-500/15 border border-purple-500/30 text-purple-400">
              <Receipt className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-1">
            <span className="text-2xl sm:text-3xl font-extrabold font-mono text-slate-100">
              {vehicleExpenses.length > 0 ? (totalAmount / vehicleExpenses.length).toFixed(2) : '0.00'}
            </span>
            <span className="text-sm font-bold text-amber-400 font-mono ml-1">{currencySymbol}</span>
          </div>
          <p className="mt-1.5 text-xs text-slate-400 font-mono">
            {lang === 'fr' ? 'Moyenne par entrée' : 'Average expense amount'}
          </p>
        </div>
      </div>

      {/* Filter Category Pills */}
      <div className="flex flex-wrap items-center gap-2 overflow-x-auto pb-1">
        <button
          onClick={() => setSelectedCategory('all')}
          className={`rounded-xl px-4 py-2 text-xs font-mono font-bold transition shadow-sm ${
            selectedCategory === 'all'
              ? 'bg-amber-500 text-slate-950 shadow-amber-500/20'
              : 'border border-slate-800 bg-[#0F1522] text-slate-300 hover:bg-slate-800/60 hover:text-white'
          }`}
        >
          {t.common.all} ({vehicleExpenses.length})
        </button>
        {allCategories.map((cat) => {
          const count = vehicleExpenses.filter(e => e.category === cat).length;
          if (count === 0 && selectedCategory !== cat) return null;
          return (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`rounded-xl px-4 py-2 text-xs font-mono font-bold transition shadow-sm ${
                selectedCategory === cat
                  ? 'bg-amber-500 text-slate-950 shadow-amber-500/20'
                  : 'border border-slate-800 bg-[#0F1522] text-slate-300 hover:bg-slate-800/60 hover:text-white'
              }`}
            >
              {t.categories[cat] || cat} {count > 0 ? `(${count})` : ''}
            </button>
          );
        })}
      </div>

      {/* Search Input */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="relative w-full sm:w-80">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
          <input
            type="text"
            placeholder={lang === 'fr' ? 'Rechercher description, notes...' : 'Search description, notes...'}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-xl border border-slate-800 bg-[#0F1522] pl-10 pr-4 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500 shadow-sm font-sans"
          />
        </div>

        <span className="text-xs text-slate-400 font-mono">
          {lang === 'fr' 
            ? `${filteredExpenses.length} sur ${vehicleExpenses.length} enregistrements`
            : `Showing ${filteredExpenses.length} of ${vehicleExpenses.length} records`}
        </span>
      </div>

      {/* Expenses Table */}
      {filteredExpenses.length === 0 ? (
        <div className="rounded-2xl border-2 border-dashed border-slate-800 bg-[#0F1522]/50 p-12 text-center shadow-xl">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
            <Receipt className="h-7 w-7" />
          </div>
          <h2 className="mt-4 text-base font-bold font-mono text-slate-200">
            {vehicleExpenses.length === 0 ? (lang === 'fr' ? 'Aucune dépense enregistrée' : 'No expenses recorded yet') : (lang === 'fr' ? 'Aucun résultat correspondant' : 'No expenses match your filters')}
          </h2>
          <p className="mt-1 text-xs text-slate-400 max-w-sm mx-auto">
            {vehicleExpenses.length === 0 
              ? (lang === 'fr' ? 'Enregistrez vos frais d’assurance, péages, stationnement, vignettes et lavages pour suivre vos dépenses de bord.' : 'Log insurance payments, parking fees, highway tolls, taxes, and car washes to monitor running costs.')
              : (lang === 'fr' ? 'Essayez de modifier votre recherche ou catégorie.' : 'Try clearing your category or search filter.')}
          </p>
          {vehicleExpenses.length === 0 && (
            <button
              onClick={handleOpenAdd}
              className="mt-5 inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2.5 text-sm font-bold text-slate-950 shadow-md hover:from-amber-400 hover:to-amber-500 transition"
            >
              <Plus className="h-4 w-4" />
              <span>{t.expenses.addExpense}</span>
            </button>
          )}
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-800 bg-[#0F1522] shadow-xl shadow-black/50">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900/80 border-b border-slate-800 text-[11px] font-mono font-bold uppercase tracking-wider text-slate-400">
                <tr>
                  <th className="px-5 py-3.5">{t.expenses.category}</th>
                  <th className="px-5 py-3.5">{t.expenses.description}</th>
                  <th className="px-5 py-3.5">{t.common.date}</th>
                  <th className="px-5 py-3.5">{t.common.odometer}</th>
                  <th className="px-5 py-3.5">{t.expenses.amount}</th>
                  <th className="px-5 py-3.5 text-right">{t.common.actions}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/70">
                {filteredExpenses.map((exp) => {
                  const Icon = CATEGORY_ICONS[exp.category] || Tag;
                  const colors = CATEGORY_COLORS[exp.category] || { bg: 'bg-slate-800', text: 'text-slate-300', border: 'border-slate-700' };

                  return (
                    <tr key={exp.id} className="hover:bg-slate-800/40 transition">
                      {/* Category Pill with Icon */}
                      <td className="px-5 py-4 whitespace-nowrap">
                        <span className={`inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs font-mono font-bold ${colors.bg} ${colors.text} ${colors.border}`}>
                          <Icon className="h-3.5 w-3.5" />
                          <span>{t.categories[exp.category] || exp.category}</span>
                        </span>
                      </td>

                      {/* Description & Notes */}
                      <td className="px-5 py-4">
                        <div className="font-semibold text-slate-100">
                          {exp.description}
                        </div>
                        {exp.notes && (
                          <div className="text-xs text-slate-400 italic mt-0.5 font-sans">
                            {exp.notes}
                          </div>
                        )}
                        {exp.receiptUrl && (
                          <a
                            href={exp.receiptUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1 text-[11px] font-mono font-semibold text-cyan-400 hover:text-cyan-300 hover:underline mt-1"
                          >
                            <span>{lang === 'fr' ? 'Voir le reçu' : 'View receipt'}</span>
                            <ExternalLink className="h-3 w-3" />
                          </a>
                        )}
                      </td>

                      {/* Date */}
                      <td className="px-5 py-4 whitespace-nowrap text-slate-300 font-mono text-xs">
                        {new Date(exp.date).toLocaleDateString()}
                      </td>

                      {/* Odometer */}
                      <td className="px-5 py-4 whitespace-nowrap text-slate-300 font-mono text-xs">
                        {exp.odometer ? `${exp.odometer.toLocaleString()} ${distUnit}` : '—'}
                      </td>

                      {/* Amount */}
                      <td className="px-5 py-4 whitespace-nowrap">
                        <span className="font-extrabold font-mono text-slate-100 text-base">
                          {exp.amount.toFixed(2)} <span className="text-xs font-bold text-amber-400">{currencySymbol}</span>
                        </span>
                      </td>

                      {/* Actions */}
                      <td className="px-5 py-4 text-right whitespace-nowrap">
                        <button
                          onClick={() => setDeleteConfirmId(exp.id)}
                          className="rounded-lg p-1.5 text-slate-500 hover:bg-red-500/20 hover:text-red-400 transition"
                          title={t.common.delete}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add Expense Modal */}
      <Modal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        titleId="expense-add-title"
        closedby="closerequest"
      >
          <div className="relative mx-auto w-full max-w-lg rounded-2xl bg-[#0F1522] border border-slate-800 p-6 shadow-2xl shadow-black/80 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
                  <Receipt className="h-5 w-5" />
                </div>
                <div>
                  <h2 id="expense-add-title" className="text-lg font-bold text-slate-100 font-mono tracking-tight">{t.expenses.addExpense}</h2>
                  <p className="text-xs text-slate-400">
                    {activeVehicle?.make} {activeVehicle?.model} {activeVehicle?.plate ? `(${activeVehicle.plate})` : ''}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setIsModalOpen(false)}
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSaveExpense} className="mt-5 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.expenses.category} *
                  </label>
                  <select
                    value={category}
                    onChange={(e) => handleCategorySelect(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-semibold text-slate-100 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  >
                    {allCategories.map((cat) => (
                      <option key={cat} value={cat} className="bg-slate-900 text-slate-100">
                        {t.categories[cat] || cat}
                      </option>
                    ))}
                    <option value="__add_custom__" className="bg-slate-900 text-amber-400">
                      {lang === 'fr' ? '＋ Ajouter une catégorie…' : '＋ Add a category…'}
                    </option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.expenses.amount} ({currencySymbol}) *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    autoFocus
                    placeholder="25.00"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-mono font-bold text-amber-400 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                  {formErrors.amount && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.amount}</p>}
                </div>
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {t.expenses.description} *
                </label>
                <input
                  type="text"
                  placeholder="e.g. Assurance semestrielle, Parking aéroport"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
                {formErrors.description && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.description}</p>}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.common.date} *
                  </label>
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-mono text-slate-100 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                  {formErrors.date && <p className="mt-1 text-xs text-red-400 font-mono">{formErrors.date}</p>}
                </div>

                <div>
                  <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                    {t.common.odometer} ({distUnit})
                  </label>
                  <input
                    type="number"
                    placeholder="Optional mileage"
                    value={odometer}
                    onChange={(e) => setOdometer(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-mono text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {t.expenses.receiptUrl}
                </label>
                <input
                  type="url"
                  placeholder="https://example.com/receipt.jpg"
                  value={receiptUrl}
                  onChange={(e) => setReceiptUrl(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>

              <div>
                <label className="block text-xs font-mono font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  {t.common.notes}
                </label>
                <input
                  type="text"
                  placeholder="Additional context or payment reference"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-300 hover:bg-slate-800 transition"
                >
                  {t.common.cancel}
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2 text-sm font-bold text-slate-950 shadow-lg shadow-amber-500/20 hover:from-amber-400 hover:to-amber-500 disabled:opacity-50 transition"
                >
                  {submitting ? t.common.loading : t.common.save}
                </button>
              </div>
            </form>
          </div>
      </Modal>


      {/* Custom Category Dialog */}
      <Modal
        open={showCategoryDialog}
        onClose={() => setShowCategoryDialog(false)}
        titleId="expense-category-title"
        closedby="closerequest"
      >
          <div className="relative mx-auto w-full max-w-md rounded-2xl bg-[#0F1522] border border-slate-800 p-6 shadow-2xl shadow-black/80">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
                <Tag className="h-5 w-5" />
              </div>
              <h2 id="expense-category-title" className="text-lg font-bold text-slate-100 font-mono">
                {lang === 'fr' ? 'Nouvelle catégorie' : 'New category'}
              </h2>
            </div>
            <p className="mt-3 text-sm text-slate-300">
              {lang === 'fr'
                ? 'Ajoutez une catégorie de dépense personnalisée.'
                : 'Add a custom expense category.'}
            </p>
            <input
              type="text"
              autoFocus
              value={newCategoryName}
              onChange={(e) => {
                setNewCategoryName(e.target.value);
                setCategoryError('');
              }}
              placeholder={lang === 'fr' ? 'ex. Location de voiture' : 'e.g. Car rental'}
              className="mt-4 w-full rounded-xl border border-slate-700 bg-slate-900/90 px-3 py-2.5 text-sm font-medium text-slate-100 placeholder-slate-500 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
            />
            {categoryError && <p className="mt-1 text-xs text-red-400 font-mono">{categoryError}</p>}
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                onClick={() => setShowCategoryDialog(false)}
                className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-300 hover:bg-slate-800 transition"
              >
                {t.common.cancel}
              </button>
              <button
                onClick={handleAddCategory}
                className="rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 px-5 py-2 text-sm font-bold text-slate-950 shadow-lg shadow-amber-500/20 hover:from-amber-400 hover:to-amber-500 transition"
              >
                {lang === 'fr' ? 'Ajouter' : 'Add'}
              </button>
            </div>
          </div>
      </Modal>


      {/* Delete Confirmation Dialog */}
      <Modal
        open={!!deleteConfirmId}
        onClose={() => setDeleteConfirmId(null)}
        titleId="expense-delete-title"
        closedby="closerequest"
      >
          <div className="relative mx-auto w-full max-w-md rounded-2xl bg-[#0F1522] border border-slate-800 p-6 shadow-2xl shadow-black/80 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3 text-red-400">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-red-500/15 border border-red-500/30 text-red-400">
                <AlertTriangle className="h-5 w-5" />
              </div>
              <h2 id="expense-delete-title" className="text-lg font-bold text-slate-100 font-mono">{t.common.delete}</h2>
            </div>
            <p className="mt-3 text-sm text-slate-300 leading-relaxed">
              {t.common.confirmDelete}
            </p>
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                autoFocus
                onClick={() => setDeleteConfirmId(null)}
                className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-300 hover:bg-slate-800 transition"
              >
                {t.common.cancel}
              </button>
              <button
                onClick={() => { if (deleteConfirmId) handleDelete(deleteConfirmId); }}
                className="rounded-xl bg-gradient-to-r from-red-600 to-red-700 px-5 py-2 text-sm font-bold text-white shadow-lg shadow-red-600/25 hover:from-red-500 hover:to-red-600 transition"
              >
                {t.common.confirm}
              </button>
            </div>
          </div>
      </Modal>

    </div>
  );
}
