import type { FillUp, Expense, Vehicle, FuelType, Units, ExpenseCategory } from '../types/index';

/**
 * Parse a single CSV line respecting double quotes and escaped double quotes ("").
 */
export function parseCsvLine(line: string, delimiter = ','): string[] {
  const result: string[] = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (ch === delimiter && !inQuotes) {
      result.push(current.trim());
      current = '';
    } else {
      current += ch;
    }
  }
  result.push(current.trim());
  return result;
}

function escapeCsvCell(val: string | number | boolean | undefined | null): string {
  if (val === undefined || val === null) return '""';
  const str = String(val).replace(/"/g, '""');
  return `"${str}"`;
}

export function mapFuelioCodeToFuelType(
  raw: string | undefined,
  fallback: FuelType = 'gasoline'
): FuelType {
  if (!raw) return fallback;
  const val = raw.trim().toLowerCase();
  if (val === 'diesel' || val === 'gazole' || val === 'gasoil') return 'diesel';
  if (val === 'gasoline' || val === 'petrol' || val === 'essence' || val === 'sp95' || val === 'sp98')
    return 'gasoline';
  if (val === 'lpg' || val === 'gpl') return 'lpg';
  if (val === 'electric' || val === 'ev' || val === 'électrique') return 'electric';
  if (val === 'hybrid' || val === 'hybride') return 'hybrid';

  const code = Number(val);
  if (!Number.isNaN(code) && code > 0) {
    // Fuelio numeric FuelType IDs:
    // 100-109: Petrol / Gasoline
    // 110-119, 200-299: Diesel
    // 120-129, 400-499: LPG / CNG
    // 300-399: Hybrid
    // 500-599: Electric
    if ((code >= 110 && code < 120) || (code >= 200 && code < 300)) return 'diesel';
    if ((code >= 120 && code < 130) || (code >= 400 && code < 500)) return 'lpg';
    if (code >= 300 && code < 400) return 'hybrid';
    if (code >= 500 && code < 600) return 'electric';
    if (code >= 100 && code < 110) return 'gasoline';
  }
  return fallback;
}

export function mapFuelTypeToFuelioCode(fuelType: FuelType): string {
  switch (fuelType) {
    case 'diesel':
      return '110';
    case 'lpg':
      return '120';
    case 'hybrid':
      return '301';
    case 'electric':
      return '501';
    case 'gasoline':
    default:
      return '100';
  }
}

function normalizeDateToIso(rawDate: string): string | null {
  const cleaned = rawDate.replace(/^["']|["']$/g, '').trim();
  if (!cleaned) return null;

  // Support "YYYY-MM-DD HH:mm", "YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY", "YYYY.MM.DD"
  const isoLike = cleaned.match(/^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})(?:\s+(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?/);
  if (isoLike) {
    const [, y, m, d, hh = '10', mm = '00', ss = '00'] = isoLike;
    const dt = new Date(
      Date.UTC(Number(y), Number(m) - 1, Number(d), Number(hh), Number(mm), Number(ss))
    );
    if (!Number.isNaN(dt.getTime())) return dt.toISOString();
  }

  const slashMatch = cleaned.match(/^(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})(?:\s+(\d{1,2}):(\d{1,2}))?/);
  if (slashMatch) {
    const [, first, second, y, hh = '10', mm = '00'] = slashMatch;
    const n1 = Number(first);
    const n2 = Number(second);
    // If first > 12, it's DD/MM/YYYY; otherwise default to DD/MM/YYYY (European/Fuelio standard)
    const day = n1 > 12 ? n1 : n1;
    const month = n1 > 12 ? n2 : n2;
    const dt = new Date(Date.UTC(Number(y), month - 1, day, Number(hh), Number(mm), 0));
    if (!Number.isNaN(dt.getTime())) return dt.toISOString();
  }

  const fallback = new Date(cleaned);
  if (!Number.isNaN(fallback.getTime())) {
    return fallback.toISOString();
  }
  return null;
}

function formatDateForFuelio(isoDate: string): string {
  const d = new Date(isoDate);
  if (Number.isNaN(d.getTime())) {
    return isoDate.slice(0, 16).replace('T', ' ');
  }
  const yyyy = d.getUTCFullYear();
  const mm = String(d.getUTCMonth() + 1).padStart(2, '0');
  const dd = String(d.getUTCDate()).padStart(2, '0');
  const hh = String(d.getUTCHours()).padStart(2, '0');
  const min = String(d.getUTCMinutes()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd} ${hh}:${min}`;
}

function parseNumber(val: string | undefined): number {
  if (!val) return 0;
  // Support both "1450.50" and European "1450,50"
  const normalized = val.replace(/\s+/g, '').replace(',', '.');
  const num = Number(normalized);
  return Number.isFinite(num) ? num : 0;
}

export interface FuelioImportResult {
  fillUps: Omit<FillUp, 'id'>[];
  expenses: Omit<Expense, 'id'>[];
  vehicleMeta?: Partial<Vehicle>;
  maxOdometer: number;
}

/**
 * Parses a Fuelio CSV backup (multi-section with "## Vehicle", "## Log", "## Costs"
 * OR flat CSV log table) into Net-CAR FillUp and Expense records.
 */
export function parseFuelioCsv(
  csvContent: string,
  vehicleId: string,
  defaultFuelType: FuelType = 'gasoline'
): FuelioImportResult {
  const cleanText = csvContent.replace(/^\uFEFF/, '');
  const rawLines = cleanText.split(/\r?\n/);

  const fillUps: Omit<FillUp, 'id'>[] = [];
  const expenses: Omit<Expense, 'id'>[] = [];
  let vehicleMeta: Partial<Vehicle> | undefined;
  let maxOdometer = 0;

  type Section = 'none' | 'vehicle' | 'log' | 'costs' | 'other';
  let currentSection: Section = 'none';
  let headers: string[] = [];
  let delimiter = ',';

  for (let i = 0; i < rawLines.length; i++) {
    const rawLine = rawLines[i].trim();
    if (!rawLine) continue;

    // Detect delimiter from first non-empty line if needed
    const unquoted = rawLine.replace(/^"+|"+$/g, '').trim();

    if (unquoted.startsWith('##')) {
      const secName = unquoted.replace(/^#+\s*/, '').toLowerCase();
      if (secName.startsWith('vehicle')) {
        currentSection = 'vehicle';
      } else if (secName.startsWith('log')) {
        currentSection = 'log';
      } else if (secName.startsWith('cost') && !secName.startsWith('costcategories')) {
        currentSection = 'costs';
      } else {
        currentSection = 'other';
      }
      headers = [];
      continue;
    }

    if (currentSection === 'other') {
      continue;
    }

    // Determine delimiter on header line
    if (headers.length === 0) {
      const commaCount = (rawLine.match(/,/g) || []).length;
      const semiCount = (rawLine.match(/;/g) || []).length;
      delimiter = semiCount > commaCount ? ';' : ',';

      const parsedHeaders = parseCsvLine(rawLine, delimiter).map((h) => h.toLowerCase().trim());

      // If no "##" section was seen yet, check if this is a flat log CSV header
      if (currentSection === 'none') {
        const hasDate = parsedHeaders.some((h) => h === 'data' || h.includes('date'));
        const hasOdo = parsedHeaders.some((h) => h.includes('odo') || h.includes('kilom'));
        const hasFuel = parsedHeaders.some(
          (h) => h.includes('fuel') || h.includes('volume') || h.includes('litre')
        );
        if (hasDate && (hasOdo || hasFuel)) {
          currentSection = 'log';
          headers = parsedHeaders;
          continue;
        }
      } else {
        headers = parsedHeaders;
        continue;
      }
    }

    const row = parseCsvLine(rawLine, delimiter);
    if (row.length === 0 || (row.length === 1 && !row[0])) continue;

    const getCol = (...candidates: string[]): string | undefined => {
      for (const cand of candidates) {
        const exactIdx = headers.findIndex((h) => h === cand);
        if (exactIdx !== -1 && exactIdx < row.length) {
          return row[exactIdx];
        }
      }
      for (const cand of candidates) {
        const idx = headers.findIndex((h) => h.startsWith(cand) || h.includes(cand));
        if (idx !== -1 && idx < row.length) {
          return row[idx];
        }
      }
      return undefined;
    };

    if (currentSection === 'vehicle') {
      if (!vehicleMeta) {
        const name = getCol('name');
        const make = getCol('make');
        const model = getCol('model');
        const year = parseNumber(getCol('year'));
        const plate = getCol('plate');
        const vin = getCol('vin');
        const tankType = getCol('tank1type');
        vehicleMeta = {
          name: name || undefined,
          make: make || undefined,
          model: model || undefined,
          year: year >= 1900 ? year : undefined,
          plate: plate || undefined,
          vin: vin || undefined,
          fuelType: tankType ? mapFuelioCodeToFuelType(tankType, defaultFuelType) : undefined,
        };
      }
      continue;
    }

    if (currentSection === 'log') {
      const rawDate = getCol('data', 'date') ?? row[0];
      const isoDate = normalizeDateToIso(rawDate || '');
      if (!isoDate) continue;

      const odometer = parseNumber(getCol('odo', 'odometer', 'kilom') ?? row[1]);
      const volume = parseNumber(getCol('fuel (', 'fuel', 'volume', 'litre') ?? row[2]);
      if (odometer <= 0 && volume <= 0) continue;

      const rawFull = (getCol('full') ?? row[3] ?? '1').toLowerCase();
      const isFullTank = rawFull === '1' || rawFull === 'true' || rawFull === 'yes' || rawFull === 'oui';

      // In Fuelio: "Price (optional)" (col 4) is Total Cost, and "VolumePrice" (col 13) is Unit Price
      const priceColIdx = headers.findIndex(
        (h) => h === 'price (optional)' || h === 'price' || h === 'totalcost' || h === 'total cost' || h === 'cost'
      );
      const volPriceColIdx = headers.findIndex(
        (h) => h === 'volumeprice' || h === 'volume price' || h === 'priceperunit' || h === 'unitprice' || h === 'prix/l'
      );

      const rawTotalOrPrice = priceColIdx !== -1 ? parseNumber(row[priceColIdx]) : parseNumber(row[4]);
      const rawVolPrice = volPriceColIdx !== -1 ? parseNumber(row[volPriceColIdx]) : parseNumber(row[13]);

      let totalCost = 0;
      let pricePerUnit = 0;

      if (rawTotalOrPrice > 0 && rawVolPrice > 0) {
        totalCost = rawTotalOrPrice;
        pricePerUnit = rawVolPrice;
      } else if (rawTotalOrPrice > 0 && volume > 0) {
        totalCost = rawTotalOrPrice;
        pricePerUnit = Number((totalCost / volume).toFixed(3));
      } else if (rawVolPrice > 0 && volume > 0) {
        pricePerUnit = rawVolPrice;
        totalCost = Number((volume * pricePerUnit).toFixed(2));
      }

      const station = getCol('city', 'station');
      const notes = getCol('notes', 'note');
      const rawFuelType = getCol('fueltype', 'fuel type');
      const fuelType = mapFuelioCodeToFuelType(rawFuelType, vehicleMeta?.fuelType || defaultFuelType);

      if (odometer > maxOdometer) {
        maxOdometer = odometer;
      }

      fillUps.push({
        vehicleId,
        date: isoDate,
        odometer,
        volume: Number(volume.toFixed(2)),
        pricePerUnit: Number(pricePerUnit.toFixed(3)),
        totalCost: Number(totalCost.toFixed(2)),
        fuelType,
        isFullTank,
        station: station ? station.slice(0, 200) : undefined,
        notes: notes ? notes.slice(0, 5000) : undefined,
      });
    } else if (currentSection === 'costs') {
      const title = getCol('costtitle', 'title', 'description') ?? row[0];
      const rawDate = getCol('date', 'data') ?? row[1];
      const isoDate = normalizeDateToIso(rawDate || '');
      if (!isoDate) continue;

      const odometer = parseNumber(getCol('odo', 'odometer') ?? row[2]);
      const amount = parseNumber(getCol('cost', 'amount') ?? row[5]);
      const notes = getCol('notes', 'note') ?? row[4];

      if (amount <= 0) continue;
      if (odometer > maxOdometer) {
        maxOdometer = odometer;
      }

      const category: ExpenseCategory = 'other';
      expenses.push({
        vehicleId,
        date: isoDate,
        category,
        amount: Number(amount.toFixed(2)),
        description: (title || 'Fuelio Cost').slice(0, 500),
        odometer: odometer > 0 ? odometer : undefined,
        notes: notes ? notes.slice(0, 5000) : undefined,
      });
    }
  }

  return {
    fillUps,
    expenses,
    vehicleMeta,
    maxOdometer,
  };
}

/**
 * Exports a vehicle and its fill-ups (and expenses) in exact Fuelio CSV format
 * for 100% dual compatibility between Fuelio and Net-CAR.
 */
export function exportFuelioCsv(
  vehicle: Vehicle,
  fillUps: FillUp[],
  expenses: Expense[] = [],
  units: Units = 'metric'
): string {
  const lines: string[] = [];

  // 1. "## Vehicle" section
  lines.push('"## Vehicle"');
  lines.push(
    [
      'Name',
      'Description',
      'DistUnit',
      'FuelUnit',
      'ConsumptionUnit',
      'ImportCSVDateFormat',
      'VIN',
      'Insurance',
      'Plate',
      'Make',
      'Model',
      'Year',
      'TankCount',
      'Tank1Type',
      'Tank2Type',
      'Active',
      'Tank1Capacity',
      'Tank2Capacity',
    ]
      .map(escapeCsvCell)
      .join(',')
  );

  const distUnitCode = units === 'metric' ? '0' : '1';
  const fuelUnitCode = units === 'metric' ? '0' : '1';
  const consUnitCode = units === 'metric' ? '0' : '1';
  const tank1TypeCode = mapFuelTypeToFuelioCode(vehicle.fuelType);

  lines.push(
    [
      vehicle.name || `${vehicle.make} ${vehicle.model}`,
      vehicle.notes || '',
      distUnitCode,
      fuelUnitCode,
      consUnitCode,
      'yyyy-MM-dd',
      vehicle.vin || '',
      '',
      vehicle.plate || '',
      vehicle.make,
      vehicle.model,
      vehicle.year,
      '1',
      tank1TypeCode,
      '0',
      '1',
      '55.0',
      '0.0',
    ]
      .map(escapeCsvCell)
      .join(',')
  );

  // 2. "## Log" section
  lines.push('"## Log"');
  lines.push(
    [
      'Data',
      units === 'metric' ? 'Odo (km)' : 'Odo (mi)',
      units === 'metric' ? 'Fuel (litres)' : 'Fuel (us gallons)',
      'Full',
      'Price (optional)',
      units === 'metric' ? 'l/100km (optional)' : 'mpg (optional)',
      'latitude (optional)',
      'longitude (optional)',
      'City (optional)',
      'Notes (optional)',
      'Missed',
      'TankNumber',
      'FuelType',
      'VolumePrice',
      'StationID (optional)',
      'ExcludeDistance',
      'UniqueId',
      'TankCalc',
    ]
      .map(escapeCsvCell)
      .join(',')
  );

  // Pre-compute consumption per fill-up in chronological order
  const chronological = [...fillUps].sort((a, b) => a.odometer - b.odometer);
  const consumptionById = new Map<string, string>();
  for (let i = 1; i < chronological.length; i++) {
    const prev = chronological[i - 1];
    const curr = chronological[i];
    const dist = curr.odometer - prev.odometer;
    if (curr.isFullTank && prev.isFullTank && dist > 0 && curr.volume > 0) {
      const econ =
        units === 'metric'
          ? ((curr.volume / dist) * 100).toFixed(2)
          : (dist / curr.volume).toFixed(2);
      consumptionById.set(curr.id, econ);
    }
  }

  // Fuelio exports logs in reverse chronological order (newest first)
  const sortedFillUps = [...fillUps].sort(
    (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()
  );

  sortedFillUps.forEach((f, index) => {
    const econ = consumptionById.get(f.id) || '0.0';
    lines.push(
      [
        formatDateForFuelio(f.date),
        f.odometer,
        f.volume.toFixed(2),
        f.isFullTank ? '1' : '0',
        f.totalCost.toFixed(2),
        econ,
        '',
        '',
        f.station || '',
        f.notes || '',
        '0',
        '1',
        mapFuelTypeToFuelioCode(f.fuelType || vehicle.fuelType),
        f.pricePerUnit.toFixed(3),
        '0',
        '0',
        index + 1,
        '0.0',
      ]
        .map(escapeCsvCell)
        .join(',')
    );
  });

  // 3. Optional "## Costs" section if expenses exist
  if (expenses.length > 0) {
    lines.push('"## Costs"');
    lines.push(
      [
        'CostTitle',
        'Date',
        'Odo',
        'CostTypeID',
        'Notes',
        'Cost',
        'RemindOdo',
        'RemindDate',
        'isTemplate',
        'RepeatOdo',
        'RepeatMonths',
        'isIncome',
        'UniqueId',
      ]
        .map(escapeCsvCell)
        .join(',')
    );

    expenses.forEach((e, idx) => {
      lines.push(
        [
          e.description,
          formatDateForFuelio(e.date),
          e.odometer || 0,
          '1',
          e.notes || '',
          e.amount.toFixed(2),
          '0',
          '2000-01-01',
          '0',
          '0',
          '0',
          '0',
          idx + 1,
        ]
          .map(escapeCsvCell)
          .join(',')
      );
    });
  }

  return lines.join('\r\n');
}
