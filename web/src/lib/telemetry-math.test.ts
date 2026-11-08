import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateAverageEconomy,
  calculateCostShares,
  calculateReminderProgress,
} from './telemetry-math';

test('calculateAverageEconomy computes metric L/100km and imperial MPG accurately', () => {
  const fillUps = [
    { odometer: 63480, volume: 48.0 },
    { odometer: 64250, volume: 50.0 },
  ];
  // Distance = 770 km, subsequent volume = 50.0 L -> (50 / 770) * 100 = 6.5 L/100km
  assert.equal(calculateAverageEconomy(fillUps, 'metric'), 6.5);
  // Imperial: 770 / 50 = 15.4 MPG
  assert.equal(calculateAverageEconomy(fillUps, 'imperial'), 15.4);
});

test('calculateCostShares computes percentages summing to 100', () => {
  const shares = calculateCostShares(2000, 5000, 3000);
  assert.equal(shares.totalSpent, 10000);
  assert.equal(shares.fuelShare, 20);
  assert.equal(shares.serviceShare, 50);
  assert.equal(shares.expenseShare, 30);
});

test('calculateReminderProgress computes accurate odometer progress and overdue state', () => {
  const progress = calculateReminderProgress(
    {
      lastServiceOdometer: 60000,
      nextDueOdometer: 75000,
      intervalKm: 15000,
    },
    64250
  );
  // (64250 - 60000) / 15000 = 4250 / 15000 = 28.33% -> 28%
  assert.equal(progress.isOverdue, false);
  assert.equal(progress.progressPercent, 28);

  const overdue = calculateReminderProgress(
    {
      lastServiceOdometer: 60000,
      nextDueOdometer: 75000,
      intervalKm: 15000,
    },
    76000
  );
  assert.equal(overdue.isOverdue, true);
  assert.equal(overdue.progressPercent, 100);
});
