// One-time copy of the SRM KTR smart bins from the old Firebase project into the current one.
// Usage (from "Supervisor database"):  node scripts/copyBinsFromOldProject.mjs <email>   [--dry-run]
// Reads the new project's config from .env, asks for the password, and writes with that user's auth
// (Firestore rules only allow signed-in writes). Safe to re-run: documents are merged by id.
import { readFileSync } from 'node:fs';
import { createInterface } from 'node:readline';
import { initializeApp } from 'firebase/app';
import { getAuth, signInWithEmailAndPassword } from 'firebase/auth';
import { getFirestore, doc, setDoc, Timestamp, GeoPoint } from 'firebase/firestore';

const OLD_PROJECT = 'westroute-b0550';
const email = process.argv[2];
const dryRun = process.argv.includes('--dry-run');

const env = Object.fromEntries(
  readFileSync(new URL('../.env', import.meta.url), 'utf8')
    .split(/\r?\n/)
    .filter((l) => l.includes('=') && !l.startsWith('#'))
    .map((l) => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()])
);

// Firestore REST typed value -> plain JS value.
function fromRest(v) {
  if ('stringValue' in v) return v.stringValue;
  if ('integerValue' in v) return Number(v.integerValue);
  if ('doubleValue' in v) return v.doubleValue;
  if ('booleanValue' in v) return v.booleanValue;
  if ('timestampValue' in v) return Timestamp.fromDate(new Date(v.timestampValue));
  if ('geoPointValue' in v) return new GeoPoint(v.geoPointValue.latitude, v.geoPointValue.longitude);
  if ('mapValue' in v) return Object.fromEntries(Object.entries(v.mapValue.fields ?? {}).map(([k, x]) => [k, fromRest(x)]));
  if ('arrayValue' in v) return (v.arrayValue.values ?? []).map(fromRest);
  return null;
}

async function readOldBins() {
  const url = `https://firestore.googleapis.com/v1/projects/${OLD_PROJECT}/databases/(default)/documents/smart_bins?pageSize=300`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Could not read old smart_bins (${res.status}). Is the old project still there?`);
  const { documents = [] } = await res.json();
  return documents.map((d) => ({
    id: d.name.split('/').pop(),
    data: Object.fromEntries(Object.entries(d.fields ?? {}).map(([k, v]) => [k, fromRest(v)])),
  }));
}

function askPassword() {
  return new Promise((resolve) => {
    const rl = createInterface({ input: process.stdin, output: process.stdout });
    process.stdout.write('Password: ');
    rl._writeToOutput = () => {}; // do not echo the password
    rl.question('', (answer) => { rl.close(); process.stdout.write('\n'); resolve(answer); });
  });
}

const bins = await readOldBins();
console.log(`Found ${bins.length} bins in ${OLD_PROJECT}.`);
if (dryRun) {
  for (const b of bins) console.log(` - ${b.id}: ${b.data.name} (${b.data.coordinates?.lat}, ${b.data.coordinates?.lng})`);
  process.exit(0);
}
if (!email) {
  console.error('Usage: node scripts/copyBinsFromOldProject.mjs <email> [--dry-run]');
  process.exit(1);
}

const app = initializeApp({
  apiKey: env.VITE_FIREBASE_API_KEY,
  authDomain: env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: env.VITE_FIREBASE_PROJECT_ID,
  appId: env.VITE_FIREBASE_APP_ID,
});
await signInWithEmailAndPassword(getAuth(app), email, await askPassword());
const db = getFirestore(app);
for (const b of bins) {
  await setDoc(doc(db, 'smart_bins', b.id), b.data, { merge: true });
  console.log(`Copied ${b.id}: ${b.data.name}`);
}
console.log(`Done. ${bins.length} bins written to ${env.VITE_FIREBASE_PROJECT_ID}.`);
process.exit(0);
