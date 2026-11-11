import { FuelType } from '@/types';

export type VehiclePhotoCategory = 'sedan' | 'hatchback' | 'suv' | 'electric';

export const STUDIO_VEHICLE_PHOTOS: Record<VehiclePhotoCategory, string> = {
  sedan: '/src/assets/images/vehicle_sedan_studio_1794317549992.jpg',
  hatchback: '/src/assets/images/vehicle_hatchback_studio_1794317555153.jpg',
  suv: '/src/assets/images/vehicle_suv_studio_1794317552758.jpg',
  electric: '/src/assets/images/vehicle_electric_studio_1794317552777.jpg',
};

export const PRESET_VEHICLE_PHOTOS = [
  {
    label: 'Sedan (Blue)',
    category: 'sedan' as VehiclePhotoCategory,
    url: STUDIO_VEHICLE_PHOTOS.sedan,
  },
  {
    label: 'Hatchback (Grey)',
    category: 'hatchback' as VehiclePhotoCategory,
    url: STUDIO_VEHICLE_PHOTOS.hatchback,
  },
  {
    label: 'SUV (White)',
    category: 'suv' as VehiclePhotoCategory,
    url: STUDIO_VEHICLE_PHOTOS.suv,
  },
  {
    label: 'Electric / Modern',
    category: 'electric' as VehiclePhotoCategory,
    url: STUDIO_VEHICLE_PHOTOS.electric,
  },
];

const SUV_KEYWORDS = [
  'suv',
  'cross',
  '4x4',
  'awd',
  'tiguan',
  'touareg',
  't-roc',
  't-cross',
  'troc',
  'tcross',
  'q2',
  'q3',
  'q5',
  'q7',
  'q8',
  'x1',
  'x2',
  'x3',
  'x4',
  'x5',
  'x6',
  'x7',
  'gla',
  'glb',
  'glc',
  'gle',
  'gls',
  'g-class',
  '2008',
  '3008',
  '5008',
  'captur',
  'kadjar',
  'austral',
  'arkana',
  'koleos',
  'duster',
  'jogger',
  'rav4',
  'c-hr',
  'chr',
  'highlander',
  'land cruiser',
  'hilux',
  'cr-v',
  'crv',
  'hr-v',
  'hrv',
  'tucson',
  'kona',
  'santa fe',
  'sportage',
  'sorento',
  'niro',
  'qashqai',
  'juke',
  'x-trail',
  'kuga',
  'puma',
  'explorer',
  'bronco',
  'ranger',
  'f-150',
  'xc40',
  'xc60',
  'xc90',
  'cayenne',
  'macan',
  'urus',
  'defender',
  'range rover',
  'evoque',
  'discovery',
  'jeep',
  'wrangler',
  'cherokee',
  'compass',
  'renegade',
  'formentor',
  'ateca',
  'arona',
  'kamiq',
  'karoq',
  'kodiaq',
  'model y',
  'model x',
  'cybertruck',
];

const ELECTRIC_KEYWORDS = [
  'tesla',
  'model 3',
  'model s',
  'taycan',
  'e-tron',
  'etron',
  'eqa',
  'eqb',
  'eqc',
  'eqe',
  'eqs',
  'i3',
  'i4',
  'i5',
  'i7',
  'ix',
  'id.3',
  'id.4',
  'id.5',
  'id.7',
  'id3',
  'id4',
  'id5',
  'id7',
  'zoe',
  'e-tech',
  'etech',
  'e-208',
  'e-2008',
  'e-308',
  'ioniq',
  'ev3',
  'ev6',
  'ev9',
  'polestar',
  'leaf',
  'ariya',
  'byd',
  'seal',
  'atto',
  'dolphin',
  'mg4',
  'spring',
  'mach-e',
  'lucid',
  'rivian',
  'electric',
  'electrique',
  'électrique',
  'ev',
];

const HATCHBACK_KEYWORDS = [
  'golf',
  'polo',
  'up!',
  'scirocco',
  'clio',
  'megane',
  'mégane',
  'twingo',
  '208',
  '308',
  '108',
  '206',
  '207',
  '307',
  'yaris',
  'auris',
  'aygo',
  'civic',
  'jazz',
  'focus',
  'fiesta',
  'ibiza',
  'leon',
  'león',
  'fabia',
  'scala',
  'a1',
  'a3',
  's3',
  'rs3',
  '1 series',
  'serie 1',
  'série 1',
  '116i',
  '118i',
  '118d',
  '120i',
  '120d',
  'm135i',
  'a-class',
  'classe a',
  'a180',
  'a200',
  'a250',
  'a35',
  'a45',
  'mini',
  'cooper',
  'c3',
  'c4',
  'ds3',
  'ds4',
  'sandero',
  'swift',
  'ignis',
  'baleno',
  'i10',
  'i20',
  'i30',
  'ceed',
  'picanto',
  'rio',
  '500',
  'panda',
  'punto',
  'tipo',
  'bravo',
  'giulietta',
  'mito',
  'corsa',
  'astra',
  'mazda2',
  'mazda3',
  'mazda 2',
  'mazda 3',
  'impreza',
  'hatchback',
  'compact',
  'gti',
  'gtd',
  'gte',
];

export function classifyVehicleCategory(
  make: string,
  model: string,
  fuelType?: FuelType | string
): VehiclePhotoCategory {
  const normalizedMake = (make || '').trim().toLowerCase();
  const normalizedModel = (model || '').trim().toLowerCase();
  const combined = `${normalizedMake} ${normalizedModel}`.trim();

  if (fuelType === 'electric') {
    if (SUV_KEYWORDS.some((kw) => combined.includes(kw))) {
      return 'suv';
    }
    return 'electric';
  }

  if (SUV_KEYWORDS.some((kw) => combined.includes(kw))) {
    return 'suv';
  }

  if (ELECTRIC_KEYWORDS.some((kw) => combined.includes(kw))) {
    return 'electric';
  }

  if (HATCHBACK_KEYWORDS.some((kw) => combined.includes(kw))) {
    return 'hatchback';
  }

  return 'sedan';
}

export function buildGeneratedVehiclePhotoUrl(params: {
  make: string;
  model: string;
  year?: number | string;
  fuelType?: FuelType | string;
}): {
  photoUrl: string;
  category: VehiclePhotoCategory;
  label: string;
} {
  const cleanMake = (params.make || '').trim();
  const cleanModel = (params.model || '').trim();
  const category = classifyVehicleCategory(cleanMake, cleanModel, params.fuelType);
  const baseAsset = STUDIO_VEHICLE_PHOTOS[category];

  const query = new URLSearchParams();
  if (cleanMake) query.set('make', cleanMake);
  if (cleanModel) query.set('model', cleanModel);
  if (params.year) query.set('year', String(params.year));

  const queryString = query.toString();
  const photoUrl = queryString ? `${baseAsset}?${queryString}` : baseAsset;
  const label = [cleanMake, cleanModel].filter(Boolean).join(' ') || 'Vehicle';

  return {
    photoUrl,
    category,
    label,
  };
}

/**
 * Returns true when a photo URL can be rendered by the browser.
 * Android stores photos picked from the device as local `file://` URIs;
 * those are synced through Firestore but only exist on that phone, so the
 * web app shows its normal car placeholder for them.
 */
export function isLoadableVehiclePhotoUrl(url?: string | null): boolean {
  const value = (url || '').trim();
  if (!value) return false;
  if (/^(https?:|data:|blob:)/i.test(value)) return true;
  return value.startsWith('/') && !value.startsWith('//');
}
