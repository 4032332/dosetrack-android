# DoseTrack for Android — Project Scaffold Design

**Date:** 2026-07-10
**Author:** Rob Brown (via Claude Code)
**Status:** Draft for review
**Scope:** Set up the Android project workspace, tooling, module/package structure,
data-layer schema stubs, and an Android `CLAUDE.md` brief. **No feature logic** is
implemented in this phase — feature build-out is planned separately, phase by phase,
after this scaffold lands and compiles.

---

## 1. Purpose

Create a native Android codebase that reproduces the **behaviour** of the existing
iOS DoseTrack app (`/Users/robbrown/CodingProjects/Apps/dosetrack-ios`) — same
features, same screens, same data model, same product rules — re-implemented with
Android's native toolkit. The iOS Swift/SwiftUI/CoreData code cannot be copied
directly; it is translated framework-for-framework into Kotlin/Compose/Room.

This document covers **only the scaffold**. It is the Android equivalent of standing
up the empty Xcode project, the CoreData model, and the `CLAUDE.md` brief — i.e. iOS
"Phase 1" minus the business logic.

## 2. Non-Goals (this phase)

- No notification scheduling logic, no billing logic, no sync logic, no UI behaviour.
- No Wear OS module, no Glance widget module. These are added in their own later
  phases (mirroring how iOS added Watch/Widgets as separate phases).
- No App Store / Play Store submission assets.

## 3. Source of Truth

The Android app mirrors the **current** iOS app, which has evolved past the original
iOS `CLAUDE.md` brief. Confirmed from the live iOS source:

- Core Data entities have gained `updatedAt` (all three) and `escriptData` (Medication).
- The iOS app has additional services beyond the original brief: caregiver/family
  sharing, Supabase-backed sync + auth (Apple/Google OAuth), push tokens, meal-time
  presets. These are **noted** in the Android `CLAUDE.md` as future phases but are
  **not** scaffolded now.

## 4. Technology Mapping

| iOS (current) | Android equivalent | Notes |
|---|---|---|
| SwiftUI | Jetpack Compose | Material 3 |
| CoreData | Room | Same three entities |
| NSPersistentCloudKitContainer | Room + Supabase sync | Reuse existing Supabase project (see §7) |
| UNUserNotificationCenter + UNCalendarNotificationTrigger | AlarmManager (exact alarms) + NotificationCompat, WorkManager for refresh | Exact alarms survive reboot via `BOOT_COMPLETED` receiver |
| Critical alerts entitlement | Full-screen intent / high-importance channel | Android has no exact "critical alert" equal; use high-importance channel + full-screen intent, documented for Play review |
| StoreKit 2 | Google Play Billing Library | Same two products / prices |
| Supabase (auth: Apple/Google OAuth) | Supabase Kotlin SDK | Reuse same project; add Android OAuth client |
| WatchKit companion | Wear OS (Compose for Wear) | Later phase |
| WidgetKit | Jetpack Glance | Later phase |
| Swift Charts | Vico (or Compose charts) | Later phase (History) |
| PDFKit | android.graphics.pdf.PdfDocument | Later phase (reports) |
| UIActivityViewController | Android Sharesheet (Intent.ACTION_SEND) | CSV export |
| App Group shared store | Single-process Room DB (shared with Glance in later phase) | |

## 5. Project Configuration

- **IDE:** Android Studio (current stable).
- **Language:** Kotlin, Jetpack Compose UI, Material 3.
- **Build:** Gradle with Kotlin DSL (`.gradle.kts`) + a version catalog
  (`gradle/libs.versions.toml`).
- **DI:** Hilt.
- **Application ID:** `com.robbrown.dosetrack`.
- **minSdk:** 26 (Android 8.0). Covers ~99% of active devices; gives per-app
  notification channels. targetSdk/compileSdk: latest stable.
- **Modules:** single `:app` module for now. Wear OS and Glance widget modules are
  added as separate Gradle modules in their own phases.
- **Repo:** git initialised in `dosetrack-android`. Recommended GitHub remote name:
  `dispoint-android`-style parallel → **`dosetrack-android`** (parallels
  `dosetrack-ios`).

## 6. Package / Directory Structure

Mirrors the iOS `Views / ViewModels / Services / Models` layout, adapted to Android
conventions:

```
dosetrack-android/
├── CLAUDE.md                        ← Android brief (translated from iOS)
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
├── secrets.example.properties       ← template; real secrets.properties gitignored
├── .gitignore
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        └── java/com/robbrown/dosetrack/
            ├── DoseTrackApplication.kt      ← @HiltAndroidApp
            ├── MainActivity.kt              ← Compose host + nav
            ├── data/
            │   ├── entity/                  ← Room @Entity: Medication, Schedule, DoseLog
            │   ├── dao/                      ← @Dao interfaces (empty stubs)
            │   ├── DoseTrackDatabase.kt      ← @Database
            │   └── converters/              ← TypeConverters (daysOfWeek, notificationIds)
            ├── di/                           ← Hilt modules (DatabaseModule, etc.)
            ├── notifications/                ← NotificationScheduler stub
            ├── billing/                      ← SubscriptionManager stub
            ├── ui/
            │   ├── theme/                    ← Compose Material 3 theme
            │   ├── today/
            │   ├── medications/
            │   ├── history/
            │   ├── settings/
            │   ├── onboarding/
            │   └── paywall/
            └── util/
```

Each `ui/*` package gets a placeholder Composable screen wired into a bottom-nav
`NavHost` (Today / Medications / History / Settings), matching the iOS `TabView`. No
screen has real behaviour — placeholders only.

## 7. Data Layer (schema stub only)

Room entities translated 1:1 from the current CoreData model. Types map:
CoreData `UUID`→`String`/`UUID`, `Integer 32`→`Int`, `Integer 16`→`Int`,
`Boolean`→`Boolean`, `Date`→`Long` (epoch millis) or `Instant`, `Binary`→`ByteArray`,
`Transformable [Int]/[String]`→JSON `String` via `TypeConverter`.

**Medication:** id, name, dosage, unit, colorHex, photoData, escriptData, notes,
isActive, currentCount, refillThreshold, totalDosesPerDay, sortOrder, createdAt,
updatedAt. Relationships → schedules (1:many, cascade), doseLogs (1:many, cascade),
modelled via `medicationId` FK on child entities.

**Schedule:** id, hour, minute, daysOfWeek (`List<Int>` via converter), frequency,
intervalDays, isEnabled, notificationIds (`List<String>` via converter), updatedAt,
medicationId FK.

**DoseLog:** id, scheduledAt, loggedAt, status, notes, updatedAt, medicationId FK.

DAOs are declared but empty (method signatures added in the data-layer feature phase).
The database must build and open; a single instrumented test confirms the schema
compiles and the DB opens. No CRUD logic yet.

## 8. Supabase / Secrets

- **Reuse the existing DoseTrack Supabase project** (the one the iOS app uses) so
  accounts and sync are cross-platform — that is the entire point of a shared backend.
  Action item for Rob: add an **Android OAuth client** (Google) and register the
  Android app's redirect for Apple/Google sign-in in Supabase Auth settings.
- Secrets handled the same way as iOS's gitignored `Secrets.swift`: a
  `secrets.properties` file (gitignored) read at build time via
  `secrets.example.properties` template committed to the repo. Holds Supabase URL +
  anon key. No secrets are used until the auth/sync phase, but the plumbing is
  scaffolded now.

## 9. Android `CLAUDE.md`

A full translation of the iOS brief adapted to Android: the tech-stack mapping above,
the same three-entity schema, the same subscription tiers and prices, the same
notification-reliability rules (restated for AlarmManager — e.g. "always use exact
alarms rescheduled on `BOOT_COMPLETED`; never rely on inexact/`setInexactRepeating`
for medication reminders", the Android analogue of the "never use
`UNTimeIntervalNotificationTrigger`" rule), the same critical product rules (5-med
free limit, local-first, export always free, refill countdown always free,
soft-delete first, disclaimer text), and the same phased build order re-sequenced for
Android (Wear OS + Glance as their own late phases).

## 10. Deliverables of This Phase

1. Git repo initialised (done) with `.gitignore`.
2. Gradle project that **builds successfully** (`./gradlew :app:assembleDebug`).
3. Package structure with placeholder Compose screens + bottom nav that runs on an
   emulator.
4. Room database with the three entities that opens (verified by one instrumented
   test).
5. `secrets.example.properties` + gitignored `secrets.properties` plumbing.
6. Android `CLAUDE.md` committed.

## 11. Verification

- `./gradlew :app:assembleDebug` succeeds.
- App launches on an emulator showing the four-tab shell.
- The single Room instrumented test passes (DB opens, schema valid).

## 12. Explicitly Deferred to Later Phases

Data-layer CRUD + repositories; notification engine (AlarmManager/WorkManager/boot
receiver); Play Billing; Today/Medications/History/Settings behaviour; add/edit
medication form; onboarding; paywall; CSV export; PDF reports; Supabase auth + sync;
caregiver/family sharing; push notifications; Wear OS app; Glance widgets; Play Store
submission assets. Each gets its own brainstorm → spec → plan cycle.
