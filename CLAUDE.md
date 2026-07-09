# DoseTrack Android — Claude Code Project Brief

> **Claude Code:** Read this entire document before writing code. Use every capability
> available — web search for current Android/Jetpack API docs, bash for Gradle builds and
> validation, file tools for code generation. Never wait to be asked to use a tool.

---

## 1. Project Overview

**App name:** DoseTrack
**Tagline:** Never miss a dose.
**Platform:** Android (minSdk 26 / Android 8.0), Wear OS companion (later phase)
**Language:** Kotlin / Jetpack Compose
**Architecture:** MVVM
**Application ID:** `com.robbrown.dosetrack`
**Play category:** Health & Fitness

### What DoseTrack is

The Android port of the iOS DoseTrack app — a medication and supplement tracker that wins
on three things: **reliable reminders** that actually fire (including on Wear OS),
**local-first data** (no forced account, always exportable), and an **honest free tier**
(5 medications free forever). Same features, same product, native Android toolkit.

### Source of truth

The **current iOS app** at `/Users/robbrown/CodingProjects/Apps/dosetrack-ios` is the
behavioural source of truth — NOT the (older) iOS `CLAUDE.md`. The live iOS app has grown
past that brief: it includes caregiver/family sharing, Supabase-backed auth + sync
(Apple/Google OAuth), push tokens, and meal-time presets. Those are future Android phases.

### First-time setup

`secrets.properties` (gitignored) is required for the auth/sync phase but NOT to build the
app. Copy `secrets.example.properties` → `secrets.properties` and fill in the Supabase URL
and anon key (reuse the **existing** DoseTrack Supabase project — the same one iOS uses).

---

## 2. Tech Stack (iOS → Android mapping)

| iOS | Android | Notes |
|---|---|---|
| SwiftUI | Jetpack Compose + Material 3 | |
| CoreData | Room | Three entities, schema v1 exported to `app/schemas` |
| NSPersistentCloudKitContainer | Room + Supabase sync | Pro cloud sync via Supabase, later phase |
| UNUserNotificationCenter + UNCalendarNotificationTrigger | AlarmManager (exact alarms) + NotificationCompat; WorkManager for queue refresh | **Exact alarms, rescheduled on `BOOT_COMPLETED`** |
| Critical Alerts entitlement | High-importance channel + full-screen intent | No exact Android equivalent; document for Play review |
| StoreKit 2 | Google Play Billing Library | Same two products |
| Supabase (Apple/Google OAuth) | Supabase Kotlin SDK | Reuse project; add Android OAuth client |
| WatchKit | Wear OS (Compose for Wear) | Later phase |
| WidgetKit (interactive) | Jetpack Glance | Later phase |
| Swift Charts | Vico / Compose charts | Later phase |
| PDFKit | `android.graphics.pdf.PdfDocument` | Later phase |
| UIActivityViewController | Android Sharesheet (`ACTION_SEND`) | CSV export |
| App Group shared store | Single-process Room DB (shared with Glance later) | |

**Dependency management:** Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`).
**DI:** Hilt. **Annotation processing:** KSP (never kapt).

---

## 3. Data Layer (Room)

Three entities translated 1:1 from the current iOS Core Data model. UUIDs → `String`
primary keys; dates → `Long` (epoch millis); transformable `[Int]`/`[String]` → JSON via
`ListConverters`. Child entities carry a `medicationId` FK with `onDelete = CASCADE` and an
index.

- **Medication** — id, name, dosage, unit, colorHex?, photoData?, escriptData?, notes?,
  isActive, currentCount, refillThreshold, totalDosesPerDay, sortOrder, createdAt?, updatedAt?
- **Schedule** — id, medicationId(FK), hour, minute, daysOfWeek(`List<Int>`, 1=Sun..7=Sat,
  empty=every day), frequency, intervalDays, isEnabled, notificationIds(`List<String>`), updatedAt?
- **DoseLog** — id, medicationId(FK), scheduledAt?, loggedAt?, status("taken"/"skipped"/"missed"),
  notes?, updatedAt?

Access via Hilt-provided singleton `DoseTrackDatabase` (`dosetrack.db`). Bump `version` and
add a `Migration` for every schema change; never ship a destructive migration in production.

---

## 4. Subscription & Paywall (Play Billing)

| Product ID | Price | |
|---|---|---|
| `com.robbrown.dosetrack.pro.monthly` | $3.99/mo | Pro Monthly |
| `com.robbrown.dosetrack.pro.annual` | $29.99/yr | Pro Annual (badge: "Best Value — Save 37%") |

Product IDs live in `util/Constants.kt`. Cache entitlement in DataStore/SharedPreferences
for offline access; listen to Play Billing purchase updates for real-time status.

**Free-tier limits:** max **5 medications**; no family sharing; no cloud sync; no PDF doctor
reports. Everything else fully functional.

**Paywall triggers:** adding medication #6; Family Sharing; iCloud/cloud Sync; Doctor Report.
Never show the paywall on first launch.

---

## 5. Notifications (the primary value proposition)

Build this right before anything else in the notifications phase.

- Request `POST_NOTIFICATIONS` (Android 13+) at onboarding, plus
  `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` for exact medication reminders.
- **Use `AlarmManager.setExactAndAllowWhileIdle` (or `setAlarmClock`) — NEVER
  `setInexactRepeating` or WorkManager-only timing for the actual reminder.** This is the
  Android analogue of the iOS "never use `UNTimeIntervalNotificationTrigger`" rule.
- Register a `BOOT_COMPLETED` (and `TIME_SET` / `TIMEZONE_CHANGED`) receiver that
  reschedules all pending alarms — exact alarms do not survive reboot on their own.
- Notification channel `MEDICATION_DUE` at high importance; actions **Taken ✓ / Skip /
  Snooze 30 min** handled in a `BroadcastReceiver` that writes the `DoseLog` without opening
  the app.
- Refresh the alarm/notification queue on each app open (next ~30 days) and via a periodic
  WorkManager job.
- Wear OS: notifications mirror automatically, but register the same actions on the watch.

---

## 6. Screens (mirror iOS)

Bottom-nav `TabView` equivalent (`DoseTrackNavHost`): **Today / Medications / History /
Settings**.

- **Today** — greeting + date + adherence score; medications due today grouped by time;
  tap → Taken/Skip/Snooze; empty state "All doses taken 🎉".
- **Medications** — active list with next-due + refill warning; FAB to add (check free-tier
  limit first); swipe Edit/Delete (confirm); tap → detail. Add/Edit form: name (autocomplete),
  dose + unit, color (8 presets), schedule builder, pill count + refill threshold, optional
  photo, notes.
- **History** — week/month/custom range; adherence bar chart; per-medication breakdown;
  calendar month grid; export CSV (free) / PDF (Pro).
- **Settings** — medications shortcut; notifications (test + critical toggle); subscription;
  family sharing (Pro); cloud sync (Pro); export CSV (always free); reminder defaults; about
  (version, privacy, disclaimer); delete all data (strong confirm).

**Onboarding** (first launch only, `hasCompletedOnboarding` in DataStore): Welcome →
Notifications permission → Add first medication (skippable).

---

## 7. Critical Rules — Never Violate

1. Never store medication data on a server without explicit opt-in. Free tier is 100% local.
2. Never paywall core reminder functionality. Unlimited medications is Pro; reminders always
   work free.
3. Never use inexact/repeating alarms for recurring medication reminders. Exact alarms only,
   rescheduled on boot.
4. Never show a paywall on first app open.
5. Never permanently delete a medication without a soft-delete (`isActive = false`) step first.
6. Always include the disclaimer on medical-adjacent UI: *"DoseTrack is a reminder tool, not
   medical advice. Always follow your healthcare provider's instructions."*
7. Data export (CSV) is always free. Never gate it behind Pro.
8. The refill countdown works on the free tier. It is a safety feature, not premium.

---

## 8. Build Phases (in order — each must build + run before the next)

1. **Scaffold** ✅ — Gradle/Compose project, Room schema, Hilt, four-tab shell, this brief.
2. **Data layer** — DAOs (CRUD), repositories, unit tests.
3. **Core UI** — Today / Medications list / Add-Edit form / Detail, wired to Room.
4. **Notifications engine** — AlarmManager + boot receiver + actions + WorkManager refresh.
5. **History & adherence** — charts, calendar, adherence calc, CSV export, PDF (Pro).
6. **Billing** — Play Billing, paywall, free-tier gating.
7. **Auth & sync** — Supabase auth (Google/Apple), Pro cloud sync. Needs Android OAuth
   client (package `com.robbrown.dosetrack` + signing SHA-1) added to Supabase.
8. **Caregiver / family sharing**, **push notifications**, **meal-time presets** — parity
   with current iOS.
9. **Wear OS companion** — separate Gradle module (Compose for Wear), today view,
   notification actions, complication.
10. **Glance widgets** — separate module: small / medium (interactive mark-as-taken) / lock.
11. **Polish & Play submission** — haptics, dynamic type, dark mode audit, accessibility,
    `PrivacyInfo`-equivalent Data Safety form, review notes for exact-alarm/full-screen-intent
    use, screenshots.

---

## 9. Build & Run (local)

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug            # build
./gradlew :app:connectedDebugAndroidTest # instrumented tests (needs emulator/device)
./gradlew :app:installDebug             # install to running emulator
```

Emulator AVD: `Pixel_10_Pro_XL` (API 36, arm64). `secrets.properties` is optional until the
auth phase (the build falls back to `secrets.example.properties`).

---

*Built by Rob Brown using Claude Code. Companion to the iOS app at ../dosetrack-ios.*
