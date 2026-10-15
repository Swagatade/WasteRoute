/**
 * WestRoute Municipal Operations - Real-World Operations Dataset
 * Covers Municipal Wards, Complaints, Workers, IoT Smart Bins, and Verification Records
 */

export const MUNICIPAL_WARDS = [
  { id: 'all', name: 'All Campus Zones' },
  { id: 'ward-ktr-01', name: 'SRM KTR Academic Zone (UB & Library)' },
  { id: 'ward-ktr-02', name: 'SRM KTR Tech Zone (Tech Park & IT)' },
  { id: 'ward-ktr-03', name: 'SRM KTR Cafeteria Zone (Java Green & Clock Tower)' },
  { id: 'ward-ktr-04', name: 'SRM KTR Residential Zone (Paari & Kaari Hostels)' },
  { id: 'ward-ktr-05', name: 'SRM KTR Perimeter & Transit (Main Gate 1 & Hospital)' }
];

// No bundled sample data: complaints, bins and workers come only from live Firestore.
export const INITIAL_COMPLAINTS = [];

export const INITIAL_WORKERS = [];

export const INITIAL_SMART_BINS = [];
