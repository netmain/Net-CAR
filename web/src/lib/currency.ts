/**
 * Currency helpers shared by the web app.
 *
 * The symbol is kept clean ("DA") and the space between the symbol and the
 * amount is added when the two are rendered together ("DA 2000.00").
 */
export function currencySymbolFor(currency?: string | null): string {
  switch (currency) {
    case 'DZD':
      return 'DA';
    case 'EUR':
      return '€';
    case 'USD':
      return '$';
    case 'CAD':
      return 'CA$';
    case 'GBP':
      return '£';
    case 'MAD':
      return 'MAD';
    case 'CHF':
      return 'CHF';
    default:
      return currency && currency.trim() ? currency.trim() : 'DA';
  }
}

export function formatMoney(symbol: string, amount: number | string, decimals = 2): string {
  const cleanSymbol = (symbol || '').trim();
  const value = typeof amount === 'number' ? amount.toFixed(decimals) : String(amount);
  return cleanSymbol ? `${cleanSymbol} ${value}` : value;
}
