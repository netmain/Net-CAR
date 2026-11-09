import type { FillUp, Reminder, Units } from '../types/index';

export function calculateAverageEconomy(
  fillUps: Pick<FillUp, 'odometer' | 'volume'>[],
  units: Units
): number {
  if (fillUps.length < 2) {
    return units === 'metric' ? 5.8 : 40.5;
  }
  const sorted = [...fillUps].sort((a, b) => a.odometer - b.odometer);
  const distance = sorted[sorted.length - 1].odometer - sorted[0].odometer;
  const totalVolume = sorted.slice(1).reduce((acc, f) => acc + f.volume, 0);
  if (distance <= 0 || totalVolume <= 0) {
    return units === 'metric' ? 5.8 : 40.5;
  }
  if (units === 'metric') {
    return Number(((totalVolume / distance) * 100).toFixed(1));
  }
  return Number((distance / totalVolume).toFixed(1));
}

export function calculateCostShares(
  fuelCost: number,
  serviceCost: number,
  expenseCost: number
): { fuelShare: number; serviceShare: number; expenseShare: number; totalSpent: number } {
  const totalSpent = fuelCost + serviceCost + expenseCost;
  if (totalSpent <= 0) {
    return { fuelShare: 45, serviceShare: 35, expenseShare: 20, totalSpent: 0 };
  }
  const fuelShare = Math.round((fuelCost / totalSpent) * 100);
  const serviceShare = Math.round((serviceCost / totalSpent) * 100);
  const expenseShare = Math.max(0, 100 - fuelShare - serviceShare);
  return { fuelShare, serviceShare, expenseShare, totalSpent };
}

export function calculateReminderProgress(
  reminder: Pick<
    Reminder,
    'nextDueOdometer' | 'lastServiceOdometer' | 'intervalKm' | 'nextDueDate'
  >,
  currentOdometer: number,
  nowMs: number = Date.now()
): { isOverdue: boolean; progressPercent: number } {
  let isOverdue = false;
  let progressPercent = 50;

  if (reminder.nextDueOdometer) {
    const remainingDist = reminder.nextDueOdometer - currentOdometer;
    if (remainingDist <= 0) {
      isOverdue = true;
      progressPercent = 100;
    } else {
      const base =
        reminder.lastServiceOdometer ??
        (reminder.intervalKm
          ? reminder.nextDueOdometer - reminder.intervalKm
          : currentOdometer - 5000);
      const totalInterval = reminder.nextDueOdometer - base;
      if (totalInterval > 0) {
        const elapsed = currentOdometer - base;
        progressPercent = Math.min(
          100,
          Math.max(5, Math.round((elapsed / totalInterval) * 100))
        );
      }
    }
  }

  if (reminder.nextDueDate) {
    const dueTime = new Date(reminder.nextDueDate).getTime();
    const diffMs = dueTime - nowMs;
    const remainingDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
    if (diffMs <= 0) {
      isOverdue = true;
      progressPercent = 100;
    } else if (!reminder.nextDueOdometer) {
      progressPercent = remainingDays <= 30 ? 85 : 40;
    }
  }

  return { isOverdue, progressPercent };
}

export interface ConsumptionTrendPoint {
  id: string;
  date: string;
  shortDate: string;
  consumption: number;
  odometer: number;
  distance: number;
  volume: number;
  totalCost: number;
  station: string;
}

export function buildConsumptionTrendSeries(
  fillUps: Pick<
    FillUp,
    'id' | 'date' | 'odometer' | 'volume' | 'totalCost' | 'isFullTank' | 'station'
  >[],
  units: Units
): ConsumptionTrendPoint[] {
  if (fillUps.length < 2) return [];

  const chronological = [...fillUps].sort((a, b) => {
    if (a.odometer !== b.odometer) return a.odometer - b.odometer;
    return a.date.localeCompare(b.date);
  });

  const points: ConsumptionTrendPoint[] = [];

  for (let i = 1; i < chronological.length; i++) {
    const prev = chronological[i - 1];
    const curr = chronological[i];
    const distance = curr.odometer - prev.odometer;

    if (curr.isFullTank && prev.isFullTank && distance > 0 && curr.volume > 0) {
      const consumption =
        units === 'metric'
          ? Number(((curr.volume / distance) * 100).toFixed(1))
          : Number((distance / curr.volume).toFixed(1));

      const isoDate = curr.date.slice(0, 10);
      points.push({
        id: curr.id,
        date: isoDate,
        shortDate: isoDate.slice(5),
        consumption,
        odometer: curr.odometer,
        distance,
        volume: curr.volume,
        totalCost: curr.totalCost,
        station: curr.station || '',
      });
    }
  }

  return points;
}
