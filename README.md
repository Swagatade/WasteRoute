# WasteRoute

**Smart municipal waste management for campuses and cities.** WasteRoute connects residents, field workers and supervisors on a single real-time platform: citizens report cleanliness issues, supervisors review and dispatch them, and workers collect, document and close the job with photo proof, all synced live through Firebase.

The reference deployment is configured for **SRM IST Kattankulathur (SRM KTR)**, with campus smart-bin data and map defaults for that site.

---

## Table of Contents

- [Overview](#overview)
- [Repository Layout](#repository-layout)
- [Features](#features)
  - [Android App: Citizens](#android-app--citizens)
  - [Android App: Workers](#android-app--workers)
  - [Supervisor Web Dashboard](#supervisor-web-dashboard)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Roles and Access Control](#roles-and-access-control)
- [Firestore Data Model](#firestore-data-model)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [1. Firebase Setup](#1-firebase-setup)
  - [2. Environment Configuration](#2-environment-configuration)
  - [3. Run the Supervisor Dashboard](#3-run-the-supervisor-dashboard)
  - [4. Build the Android App](#4-build-the-android-app)
  - [5. Deploy](#5-deploy)
- [Environment Variable Reference](#environment-variable-reference)
- [Security Notes](#security-notes)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

| Actor | Platform | What they do |
|-------|----------|--------------|
| **Citizen** | Android app | Reports grievances with a photo and GPS location, tracks complaint progress, finds nearby bins, confirms household collection by scanning a worker's QR code. |
| **Worker** | Android app | Receives dispatched tasks and a daily timetable, navigates to the site, shares live truck GPS, uploads an "after" proof photo to close the task. |
| **Supervisor** | Web dashboard | Monitors operations, reviews complaints, registers workers, assigns dispatches, plans collection routes, manages areas and hostel blocks and views reports. |

Everything is backed by one Firebase project (Authentication + Cloud Firestore), so a complaint raised on a phone appears on the supervisor's screen within moments, and a worker's status update flows straight back to the citizen.

## Repository Layout

```
WasteRoute/
├── WasteRoute/                 # Native Android app (Kotlin + Jetpack Compose)
│   ├── app/                   #   Application module
│   │   └── src/main/java/com/wasteroute/app/
│   │       ├── auth/          #   Firebase auth + session management
│   │       ├── components/    #   Reusable Compose UI components
│   │       ├── firebase/      #   Firestore repositories (dispatch, collection, households, data)
│   │       ├── location/      #   Fused-location helper
│   │       ├── model/         #   Domain models and roles
│   │       ├── navigation/    #   Compose navigation graph and routes
│   │       ├── notify/        #   Worker alert foreground service + task alarms
│   │       ├── screens/       #   Citizen and worker screens
│   │       ├── theme/         #   Material 3 + glassmorphism theme
│   │       └── util/          #   Image and photo-capture helpers
│   ├── dataconnect/           #   Firebase Data Connect scaffold (template sample)
│   ├── preview/               #   Static HTML preview of the app UI
│   └── .env.example           #   Android environment template
├── Supervisor database/       # Supervisor web dashboard (React + Vite)
│   ├── src/
│   │   ├── views/             #   Overview, Complaints, Workers, Routes, Households, Map…
│   │   ├── components/        #   Navbar, Sidebar, StatusBadge, WorkerPhone
│   │   ├── context/           #   AuthContext (supervisor-only sign-in)
│   │   ├── firebase/          #   Firebase config, Firestore service, worker provisioning
│   │   └── data/              #   Campus map data and fallback sample data
│   ├── scripts/               #   One-off maintenance scripts (e.g. copy bins between projects)
│   └── .env.example           #   Web environment template
├── shared/
│   └── campus_map_data.json   # Smart-bin / campus map dataset shared by both apps
├── dataconnect/               # Root Data Connect scaffold referenced by firebase.json
├── firestore.rules            # Firestore security rules (shared by both apps)
├── firebase.json              # Firebase Hosting / Firestore / Data Connect config
├── .firebaserc                # Firebase project alias
├── .env.example               # Root workspace environment template
├── WasteRoute.code-workspace   # VS Code multi-root workspace
└── LICENSE                    # Apache License 2.0
```

## Features

### Android App: Citizens

- **Email/password and Google sign-in** (Credential Manager), with password reset.
- **City Cleanliness dashboard** with a cleanliness score, impact metrics and neighbourhood charts.
- **Report a grievance** with a camera photo and auto-detected GPS location.
- **Complaint journey tracking**: a timeline from submission through assignment, trip start and resolution, with the worker's proof photo.
- **Nearby smart-bin map** (OpenStreetMap) with fill levels.
- **Link your home or hostel block** with its home code, then **scan a worker's QR code** to confirm collection. This writes an audit log entry and marks the stop completed.
- Light/dark theme toggle and a performance mode for lower-end devices.
- Offline banner when connectivity is lost.

### Android App: Workers

- **Worker login** restricted to `@worker.in` accounts provisioned by a supervisor.
- **Task queue and dispatch**: accept a task, start the trip, and follow it on a route map.
- **Live GPS**: publishes the truck/worker position in real time so citizens and supervisors can track progress.
- **Proof of work**: capture an "after" photo and close the task, which appends to the complaint timeline. Bin stops are cleared with a camera photo; household stops are confirmed by the resident's QR scan.
- **Daily timetable** generated from recurring routes assigned by supervisors, with bin and household stops.
- **Collection history** of completed stops.
- **Personal QR code** that residents scan to confirm collection.
- **Alerts**: a foreground service and alarms notify workers about new or upcoming tasks.

### Supervisor Web Dashboard

| Tab | Purpose |
|-----|---------|
| **Operations Overview** | Live KPIs across complaints, workers and bins, with ward filtering and an emergency-dispatch action. |
| **Complaint Review** | Review citizen complaints (`Pending`, `Under Review`, `Assigned`, `In Progress`, `Resolved`), assign workers and update status. |
| **Worker & Fleet Dispatch** | See worker availability, vehicles and live locations. |
| **Route Planner** | Build ordered routes of bin and household stops and assign them to workers as daily recurring routines. Today's and the next two days' slots are generated automatically. |
| **Areas & Hostels** | Manage collection zones and register hostel blocks (one at a time, in bulk by name, or the SRM hostels in one click), and see which homes are collected. |
| **Worker Registration** | Provision new worker accounts (`@worker.in`) without logging the supervisor out. |
| **Campus Map & Bins** | Leaflet + OpenStreetMap view of smart bins and worker positions. |

Access is limited to accounts on the configured supervisor domain (default `@supervisor.in`).

## Architecture

```
┌──────────────────────┐        ┌───────────────────────────┐        ┌────────────────────────┐
│  Android app         │        │  Firebase project         │        │  Supervisor dashboard  │
│  (Citizen + Worker)  │◄──────►│  • Authentication         │◄──────►│  (React + Vite)        │
│  Kotlin / Compose    │        │  • Cloud Firestore        │        │  Firebase Hosting      │
└──────────────────────┘        │  • Security Rules         │        └────────────────────────┘
                                └───────────────────────────┘
```

Both clients talk to Firestore directly and subscribe to real-time snapshot listeners; there is no custom backend server. Authorization is enforced by [`firestore.rules`](firestore.rules).

**Typical complaint lifecycle**

1. A citizen submits a complaint (photo + location) → `complaints` document created.
2. A supervisor reviews it and assigns a worker → status becomes `Assigned`.
3. The worker accepts the task and starts the trip → status `In Progress`, live truck location streamed.
4. The worker uploads proof and closes the task → status `Resolved`, timeline updated.
5. The citizen sees every step on their complaint timeline.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Android app | Kotlin, Jetpack Compose, Material 3, Navigation Compose, Gradle (Kotlin DSL) |
| Android services | Firebase Auth, Firestore, Analytics, Google Credential Manager / Sign-In, Play Services Location |
| QR | ZXing core + `zxing-android-embedded` |
| Maps | OpenStreetMap tiles |
| Web dashboard | React 19, Vite 6, `lucide-react`, Firebase JS SDK 11 |
| Backend | Firebase Authentication, Cloud Firestore, Firebase Hosting |
| Build targets | Android `minSdk 26`, `targetSdk`/`compileSdk 35`, Java 17 |

## Roles and Access Control

Roles are determined by the email domain of the signed-in account:

| Role | Account | Where they sign in |
|------|---------|--------------------|
| Citizen | Any email / Google account | Android app |
| Worker | `*@worker.in` | Android app (worker login) |
| Supervisor | `*@supervisor.in` (configurable via `VITE_SUPERVISOR_ALLOWED_DOMAIN`) | Web dashboard |

Highlights of the Firestore rules:

- `smart_bins`, `workers`, `complaints`: public read, signed-in write; complaints can never be deleted.
- `schedules`: signed-in read; only non-workers may delete; workers may only move a slot to `In Progress`, create the daily slot of their own routine, or complete a bin stop with a proof photo; a resident may only mark a slot `Completed` as themselves.
- `schedule_templates`: signed-in read; non-workers write.
- `households`: private. Readable by supervisors, workers and the home's own resident. Supervisors create/delete; workers may mark a home collected; a resident may link an unclaimed home with its code and confirm their own collection.
- `zones`: signed-in read; supervisors write.
- `collection_logs`: create-only audit trail tied to the signed-in resident.

## Firestore Data Model

| Collection | Description |
|------------|-------------|
| `complaints` | Citizen reports: reporter UID, location, photos, status, assigned worker, event timeline. |
| `smart_bins` | Bin code, name, zone, fill level, coordinates, type. |
| `workers` | Registered workers: ID, name, contact, vehicle, ward, live location. |
| `schedules` | Concrete collection slots (bin or household stops), id `"<templateId>_<yyyy-MM-dd>"`. |
| `schedule_templates` | Daily recurring routes per worker, made of ordered stops. |
| `households` | Homes / hostel blocks on the collection round: location, zone, owner link, collected status and proof. |
| `zones` | Collection areas (Academic, Hostel, Cafeteria…) that workers and hostels are grouped under. |
| `collection_logs` | Immutable log of resident collection confirmations. |

## Getting Started

### Prerequisites

- **Node.js** 18+ and npm (web dashboard)
- **Android Studio** (Koala or newer recommended) with JDK 17 and Android SDK 35
- A **Firebase project** with Authentication and Cloud Firestore enabled
- **Firebase CLI**: `npm install -g firebase-tools`

### 1. Firebase Setup

1. Create a project in the [Firebase console](https://console.firebase.google.com/).
2. Enable **Authentication** with the *Email/Password* provider, plus *Google* for citizen sign-in.
3. Create a **Cloud Firestore** database.
4. Register an **Android app** (package `com.wasteroute.app`) and download `google-services.json` into `WasteRoute/app/` (the file is git-ignored).
5. Register a **Web app** and copy its config values.
6. Create at least one supervisor account whose email ends in `@supervisor.in`.
7. Point the CLI at your project: edit `.firebaserc`, or run `firebase use --add`.
8. Publish the rules:
   ```bash
   firebase deploy --only firestore:rules
   ```

### 2. Environment Configuration

Copy each template and fill in your own values. Real `.env` files are git-ignored; never commit them.

```bash
cp .env.example .env
cp "Supervisor database/.env.example" "Supervisor database/.env"
cp WasteRoute/.env.example WasteRoute/.env
```

See the [Environment Variable Reference](#environment-variable-reference) below.

### 3. Run the Supervisor Dashboard

```bash
cd "Supervisor database"
npm install
npm run dev        # development server with hot reload
npm run build      # production build into Supervisor database/dist
npm run preview    # preview the production build locally
```

Sign in with a `@supervisor.in` account. Use the **Worker Registration** tab to create worker accounts for the Android app, then build routes in **Route Planner**.

### 4. Build the Android App

Open the `WasteRoute/` folder in Android Studio, let Gradle sync, then run on a device or emulator (API 26+).

Or from the command line:

```bash
cd WasteRoute
./gradlew assembleDebug      # Windows: gradlew.bat assembleDebug
./gradlew assembleRelease
```

The debug APK is written to `WasteRoute/app/build/outputs/apk/debug/`.

> **Note:** the release build type is currently signed with the **debug** key and has minification disabled. Configure a proper release keystore before publishing to the Play Store.

Required runtime permissions: location (fine/coarse), camera, notifications, internet, and a foreground service for worker alerts.

### 5. Deploy

Firebase Hosting serves the built dashboard (`Supervisor database/dist`) with an SPA rewrite to `index.html`:

```bash
cd "Supervisor database" && npm run build && cd ..
firebase deploy --only hosting,firestore:rules
```

## Environment Variable Reference

**Supervisor dashboard** (`Supervisor database/.env`)

| Variable | Description |
|----------|-------------|
| `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_STORAGE_BUCKET`, `VITE_FIREBASE_MESSAGING_SENDER_ID`, `VITE_FIREBASE_APP_ID`, `VITE_FIREBASE_MEASUREMENT_ID` | Firebase Web SDK config from the console. |
| `VITE_SUPERVISOR_ALLOWED_DOMAIN` | Email domain permitted to sign in (default `@supervisor.in`). |
| `VITE_PORTAL_NAME` | Display name of the municipal authority. |
| `VITE_MUNICIPAL_PORTAL_URL` | Link to the public portal. |
| `VITE_OSM_TILE_URL`, `VITE_OSM_ATTRIBUTION` | Map tile source and attribution. |
| `VITE_CAMPUS_LAT`, `VITE_CAMPUS_LNG`, `VITE_CAMPUS_ZOOM`, `VITE_CAMPUS_NAME` | Default map centre and campus label. |

**Android app** (`WasteRoute/.env`, read by `app/build.gradle.kts` into `BuildConfig`)

| Variable | Description |
|----------|-------------|
| `GOOGLE_OAUTH_CLIENT_ID` | Web OAuth client ID used for Google sign-in. |
| `API_BASE_URL` | Optional backend base URL. |
| `MUNICIPAL_PORTAL_URL` | Link to the municipal portal. |
| `OSM_TILE_URL` | Map tile URL template. |
| `CAMPUS_LAT`, `CAMPUS_LNG`, `CAMPUS_NAME` | Default map location (defaults to SRM KTR). |
| `FIREBASE_*` | Project metadata for reference; Firebase itself is configured by `google-services.json`. |

## Security Notes

- `.env`, `google-services.json`, service-account keys and keystores are in `.gitignore`. Verify nothing sensitive is staged before committing.
- Firebase web/Android API keys identify your project but are not secrets; **the security rules are your real protection**. Review `firestore.rules` before going to production.
- `smart_bins`, `workers` and `complaints` still allow public reads and any signed-in write, and worker/supervisor roles are inferred from email domain. `households` and `zones` are already locked down by role. Tighten these (for example with custom claims) for a production deployment.
- The Android manifest enables `usesCleartextTraffic`; disable it if you do not need HTTP endpoints.
- Complaint proof photos are currently stored as data URIs inside Firestore documents. For scale, move them to Cloud Storage.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Dashboard shows sample data / "Firebase is not configured" | `Supervisor database/.env` is missing or incomplete. Restart `npm run dev` after editing it. |
| "Access Denied" on dashboard login | The account is not on the supervisor domain (`VITE_SUPERVISOR_ALLOWED_DOMAIN`). |
| Worker cannot log in on the app | The email must end in `@worker.in` and be registered through the Worker Registration tab. |
| Android build fails on `google-services` | Place `google-services.json` in `WasteRoute/app/`. |
| Permission-denied errors in Firestore | Redeploy rules: `firebase deploy --only firestore:rules`. The `households` and `zones` collections need the latest rules. |
| Hostels or routes missing on the dashboard | Open **Areas & Hostels** and use the one-click SRM hostel registration, then rebuild routes in **Route Planner**. |
| Google sign-in fails | Check `GOOGLE_OAUTH_CLIENT_ID` and add your debug SHA-1 fingerprint to the Firebase Android app. |

## Contributing

1. Fork the repository and create a feature branch.
2. Keep Kotlin and React code consistent with the surrounding style.
3. Test on both the Android app and the dashboard when changing shared data (Firestore schema, rules, `shared/campus_map_data.json`).
4. Open a pull request describing the change and how you verified it.

## License

Licensed under the [Apache License 2.0](LICENSE).
