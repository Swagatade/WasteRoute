/**
 * WasteRoute Municipal Operations - Real-World Operations Dataset
 * Covers Municipal Wards, Complaints, Workers, IoT Smart Bins, and Verification Records
 */

// Campus zones (also written to Firestore `zones` the first time it is empty; editable there afterwards).
export const DEFAULT_ZONES = [
  { id: 'ward-ktr-01', name: 'SRM KTR Academic Zone (UB & Library)', kind: 'academic', lat: 12.8235, lng: 80.0438 },
  { id: 'ward-ktr-02', name: 'SRM KTR Tech Zone (Tech Park & IT)', kind: 'tech', lat: 12.8248, lng: 80.0456 },
  { id: 'ward-ktr-03', name: 'SRM KTR Cafeteria Zone (Java Green & Clock Tower)', kind: 'cafeteria', lat: 12.8225, lng: 80.0438 },
  { id: 'ward-ktr-04', name: "Men's Hostels (Boys) - SRM KTR", kind: 'hostel', group: 'boys', lat: 12.8210, lng: 80.0415 },
  { id: 'ward-ktr-06', name: "Women's Hostels (Girls) - SRM KTR", kind: 'hostel', group: 'girls', lat: 12.8218, lng: 80.0402 },
  { id: 'ward-ktr-05', name: 'SRM KTR Perimeter & Transit (Main Gate 1 & Hospital)', kind: 'perimeter', lat: 12.8230, lng: 80.0480 }
];

export const MUNICIPAL_WARDS = [
  { id: 'all', name: 'All Campus Zones' },
  ...DEFAULT_ZONES.map(({ id, name }) => ({ id, name }))
];

// No bundled sample data: complaints, bins and workers come only from live Firestore.
export const INITIAL_COMPLAINTS = [];

export const INITIAL_WORKERS = [];

export const INITIAL_SMART_BINS = [];

// The hostels of SRM KTR as given by the supervisor (names match the `households` documents in Firestore).
export const HOSTEL_NAME_SUGGESTIONS = {
  boys: ['Paari', 'Kaari', 'Oori', 'Adhiyaman', 'Nelson Mandela', 'Sannasi', 'Began', 'Pierre Fauchard', 'MSRS Block'],
  girls: ['Kalpana Chawla', 'Kopperundevi', 'Meenakshi', 'Senbagam', 'Thamarai', 'Malligai', 'Mullai', 'Kurunji', 'Sannasi C', 'ESQ Blocks']
};
