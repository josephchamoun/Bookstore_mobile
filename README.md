# Bookstore — Offline-First Android App

An Android bookstore app built with Kotlin, focused on offline-first architecture and efficient data synchronization. The main goal of this project was learning real-world mobile architecture patterns — offline sync, delta fetching, and background workers — rather than just building features.

---

## Features

- Browse and filter books by category
- Add books to cart and place orders
- Add books to favorites
- Read purchased books online as a PDF
- Add reviews
- Full offline support — the app works without internet using locally synced data

---

## Architecture — The Main Focus

### Offline-First with SQLite
- All data is stored locally in **SQLite**, so the app works fully without an internet connection
- When connectivity is restored, the app syncs with the MySQL backend automatically

### Delta Sync — Only Fetch What Changed
Instead of re-downloading the entire book catalog on every sync, the app only fetches records that changed since the last sync:
```
Each record has an updated_at column on the backend
App sends its last sync timestamp → backend returns only changed records
Local SQLite is updated with only the new/modified data
```
This saves bandwidth and makes syncs fast even on slow connections.

### Background Sync Worker
A background worker runs at scheduled intervals — even when the app is closed.

---

## Firebase Migration Branch

The `firebase-migration` branch replaces SQLite with **Firebase Realtime Database**:
- All local data storage migrated from SQLite to Firebase
- Auth remains the same (unchanged)
- Real-time data updates replace the manual sync worker
- Gained hands-on experience comparing local-first (SQLite) vs cloud-first (Firebase) architecture and the tradeoffs of each approach

---

## Tech Stack

### Main Branch (Offline-First)
| Layer | Technology |
|---|---|
| Language | Kotlin |
| Local Database | SQLite (offline storage) |
| Backend Database | MySQL |
| Background Sync | Android WorkManager |
| Sync Strategy | Delta sync via `updated_at` timestamps |
| PDF Viewing | Android PDF viewer |
| Auth | Token-based (Laravel Sanctum) |

### Firebase Branch
| Layer | Technology |
|---|---|
| Local/Cloud Database | Firebase Realtime Database |
| Everything else | Same as main branch |

---

## Related Repository

- [Bookstore Admin Web Panel (PHP)](https://github.com/josephchamoun/bookstore_web) — admin panel for managing books, orders, and users
