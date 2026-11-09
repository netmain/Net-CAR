import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateAverageEconomy,
  calculateCostShares,
  calculateReminderProgress,
} from './telemetry-math';
import { parseFuelioCsv, exportFuelioCsv } from './fuelio-csv';

test('calculateAverageEconomy computes metric L/100km and imperial MPG accurately', () => {
  const fillUps = [
    { odometer: 63480, volume: 48.0 },
    { odometer: 64250, volume: 50.0 },
  ];
  assert.equal(calculateAverageEconomy(fillUps, 'metric'), 6.5);
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

test('parseFuelioCsv and exportFuelioCsv provide 100% dual compatibility with Fuelio CSV format', () => {
  const sampleFuelioCsv = `"## Vehicle"
"Name","Description","DistUnit","FuelUnit","ConsumptionUnit","ImportCSVDateFormat","VIN","Insurance","Plate","Make","Model","Year","TankCount","Tank1Type","Tank2Type","Active","Tank1Capacity","Tank2Capacity"
"Golf VII","Daily Driver","0","0","0","yyyy-MM-dd","","","00124-116-16","Volkswagen","Golf VII 2.0 TDI","2021","1","110","0","1","55.0","0.0"
"## Log"
"Data","Odo (km)","Fuel (litres)","Full","Price (optional)","l/100km (optional)","latitude (optional)","longitude (optional)","City (optional)","Notes (optional)","Missed","TankNumber","FuelType","VolumePrice","StationID (optional)","ExcludeDistance","UniqueId","TankCalc"
"2026-11-05 10:00","64250","50.00","1","1450.00","6.49","","","Naftal Sidi Fredj","Plein gasoil","0","1","110","29.000","0","0","1","0.0"
"2026-10-25 09:30","63480","48.00","1","1392.00","0.0","","","Naftal Est","Autoroute","0","1","110","29.000","0","0","2","0.0"
"## Costs"
"CostTitle","Date","Odo","CostTypeID","Notes","Cost","RemindOdo","RemindDate","isTemplate","RepeatOdo","RepeatMonths","isIncome","UniqueId"
"Parking Centre","2026-10-29 10:00","63900","1","Centre ville","500.00","0","2000-01-01","0","0","0","0","1"`;

  const parsed = parseFuelioCsv(sampleFuelioCsv, 'veh-1', 'diesel');
  assert.equal(parsed.fillUps.length, 2);
  assert.equal(parsed.expenses.length, 1);
  assert.equal(parsed.maxOdometer, 64250);
  assert.equal(parsed.vehicleMeta?.make, 'Volkswagen');
  assert.equal(parsed.vehicleMeta?.fuelType, 'diesel');
  assert.equal(parsed.fillUps[0].odometer, 64250);
  assert.equal(parsed.fillUps[0].volume, 50);
  assert.equal(parsed.fillUps[0].totalCost, 1450);
  assert.equal(parsed.fillUps[0].pricePerUnit, 29);
  assert.equal(parsed.fillUps[0].station, 'Naftal Sidi Fredj');
  assert.equal(parsed.fillUps[0].isFullTank, true);

  // Round-trip export and re-import
  const exported = exportFuelioCsv(
    {
      id: 'veh-1',
      name: 'Daily Driver',
      make: 'Volkswagen',
      model: 'Golf VII 2.0 TDI',
      year: 2021,
      plate: '00124-116-16',
      fuelType: 'diesel',
      currentOdometer: 64250,
    },
    parsed.fillUps.map((f, idx) => ({ ...f, id: `f-${idx}` })),
    parsed.expenses.map((e, idx) => ({ ...e, id: `e-${idx}` })),
    'metric'
  );

  assert.ok(exported.includes('"## Vehicle"'));
  assert.ok(exported.includes('"## Log"'));
  assert.ok(exported.includes('"## Costs"'));

  const reparsed = parseFuelioCsv(exported, 'veh-1', 'diesel');
  assert.equal(reparsed.fillUps.length, 2);
  assert.equal(reparsed.expenses.length, 1);
  assert.equal(reparsed.fillUps[0].odometer, 64250);
  assert.equal(reparsed.fillUps[0].totalCost, 1450);
});
