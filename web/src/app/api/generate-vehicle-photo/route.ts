import { NextRequest, NextResponse } from 'next/server';
import { buildGeneratedVehiclePhotoUrl } from '@/lib/vehicle-photo';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const make = typeof body.make === 'string' ? body.make.trim() : '';
    const model = typeof body.model === 'string' ? body.model.trim() : '';
    const year = body.year;
    const fuelType = typeof body.fuelType === 'string' ? body.fuelType : undefined;

    if (!make && !model) {
      return NextResponse.json(
        { error: 'Make or model is required to generate a vehicle photo.' },
        { status: 400 }
      );
    }

    const result = buildGeneratedVehiclePhotoUrl({
      make,
      model,
      year,
      fuelType,
    });

    return NextResponse.json(result);
  } catch (err) {
    console.error('Error generating vehicle photo:', err);
    return NextResponse.json(
      { error: 'Failed to generate vehicle placeholder photo.' },
      { status: 500 }
    );
  }
}
