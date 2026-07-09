# DoseTrack Android Scaffold Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a buildable native Android project for DoseTrack (Kotlin/Compose/Room) that launches a four-tab shell on an emulator and opens a Room database whose schema mirrors the current iOS Core Data model — no feature logic.

**Architecture:** Single `:app` Gradle module, MVVM-ready package layout mirroring the iOS `Views/ViewModels/Services/Models` split. Jetpack Compose (Material 3) UI with a bottom-nav `NavHost`. Room for persistence with three entities (Medication, Schedule, DoseLog) and JSON `TypeConverter`s for the two transformable list fields. Hilt for DI. Secrets plumbed via a gitignored `secrets.properties` read at build time, mirroring iOS `Secrets.swift`.

**Tech Stack:** Kotlin, Gradle (Kotlin DSL) + version catalog, Jetpack Compose + Material 3, Navigation-Compose, Room, Hilt, JUnit4 + AndroidX instrumented test (Room schema verification).

**Reference spec:** `docs/superpowers/specs/2026-07-10-android-scaffold-design.md`

---

## File Structure

Created by this plan:

```
dosetrack-android/
├── settings.gradle.kts                 # module + repositories
├── build.gradle.kts                    # root: plugin versions via catalog
├── gradle.properties                   # AndroidX, JVM args
├── gradle/libs.versions.toml           # dependency version catalog
├── gradle/wrapper/…                    # Gradle wrapper (8.x)
├── gradlew / gradlew.bat
├── secrets.example.properties          # committed template
├── secrets.properties                  # gitignored (real values)
└── app/
    ├── build.gradle.kts                # android + compose + room + hilt config
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── java/com/robbrown/dosetrack/
        │       ├── DoseTrackApplication.kt      # @HiltAndroidApp
        │       ├── MainActivity.kt              # @AndroidEntryPoint, setContent
        │       ├── data/
        │       │   ├── entity/Medication.kt
        │       │   ├── entity/Schedule.kt
        │       │   ├── entity/DoseLog.kt
        │       │   ├── converters/ListConverters.kt
        │       │   ├── dao/MedicationDao.kt     # empty stub
        │       │   ├── dao/ScheduleDao.kt       # empty stub
        │       │   ├── dao/DoseLogDao.kt        # empty stub
        │       │   └── DoseTrackDatabase.kt
        │       ├── di/DatabaseModule.kt
        │       ├── notifications/NotificationScheduler.kt   # stub
        │       ├── billing/SubscriptionManager.kt           # stub
        │       ├── ui/
        │       │   ├── theme/{Color,Theme,Type}.kt
        │       │   ├── navigation/DoseTrackNavHost.kt + Destinations.kt
        │       │   ├── today/TodayScreen.kt
        │       │   ├── medications/MedicationsScreen.kt
        │       │   ├── history/HistoryScreen.kt
        │       │   ├── settings/SettingsScreen.kt
        │       │   ├── onboarding/OnboardingScreen.kt
        │       │   └── paywall/PaywallScreen.kt
        │       └── util/Constants.kt
        └── androidTest/java/com/robbrown/dosetrack/
            └── DoseTrackDatabaseTest.kt          # DB opens, schema valid
```

**Constants.kt** holds the shared IDs so they match iOS exactly: application ID
`com.robbrown.dosetrack`, product IDs `com.robbrown.dosetrack.pro.monthly` /
`com.robbrown.dosetrack.pro.annual`, free-tier med limit `5`.

---

## Chunk 1: Environment + Buildable Empty App

### Task 0: Verify toolchain

**Files:** none

- [ ] **Step 1: Confirm Android Studio + JDK + SDK are installed**

Run: `java -version` and `ls ~/Library/Android/sdk`
Expected: a JDK 17+ version prints, and the SDK directory exists.
If either fails: STOP — install Android Studio (which bundles both) and, in Android
Studio, use **SDK Manager** to install the current SDK Platform + Build-Tools +
Android Emulator, then create one virtual device (Pixel, current API). Do not proceed
until `java -version` succeeds. (As of writing, neither is installed yet.)

### Task 1: Gradle project skeleton that builds

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
  `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/proguard-rules.pro`,
  `app/src/main/AndroidManifest.xml`, `.gitignore` (already present — extend if needed)
- Generate: Gradle wrapper via Android Studio's project creation, OR
  `gradle wrapper --gradle-version 8.9`

- [ ] **Step 1: Create the project via Android Studio** (recommended path)

In Android Studio: New Project → **Empty Activity** (Compose) → Name `DoseTrack`,
Package `com.robbrown.dosetrack`, Language Kotlin, Minimum SDK **API 26**, Build
config language **Kotlin DSL**. Target the empty `dosetrack-android` folder (it already
contains `docs/`, `.git/`, `.gitignore` — let Studio populate around them; if Studio
refuses a non-empty dir, generate in a temp dir and copy the generated files in,
keeping our existing `docs/` and `.gitignore`).

This yields a project that already builds. The remaining tasks adjust it to our
structure and add Room/Hilt.

- [ ] **Step 2: Pin versions in `gradle/libs.versions.toml`**

Ensure the catalog includes: AGP (current stable 8.x), Kotlin 2.x, Compose BOM
(current), androidx.core-ktx, lifecycle-runtime-ktx, activity-compose,
navigation-compose, room (runtime/ktx/compiler), hilt (android/compiler),
hilt-navigation-compose, kotlinx-serialization-json, and test libs (junit,
androidx.test ext, espresso). Use KSP (not kapt) for Room and Hilt annotation
processing.

- [ ] **Step 3: Set applicationId and SDK levels in `app/build.gradle.kts`**

`applicationId = "com.robbrown.dosetrack"`, `minSdk = 26`,
`targetSdk`/`compileSdk = 35` (current), `versionCode = 1`, `versionName = "1.0.0"`.
Enable `buildFeatures { compose = true }` and `buildConfig = true`. Apply KSP,
Hilt, and kotlin-serialization plugins.

- [ ] **Step 4: Build the empty app**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: scaffold Gradle/Compose Android project (empty app builds)"
```

### Task 2: Secrets plumbing

**Files:**
- Create: `secrets.example.properties`
- Modify: `app/build.gradle.kts` (read `secrets.properties` → `BuildConfig` fields),
  `.gitignore` (already ignores `secrets.properties` — verify)

- [ ] **Step 1: Write `secrets.example.properties`**

```properties
# Copy to secrets.properties (gitignored) and fill in.
# Reuses the existing DoseTrack Supabase project (same one iOS uses).
SUPABASE_URL=https://YOUR-PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR-ANON-KEY
```

- [ ] **Step 2: Wire into `app/build.gradle.kts`**

Load `secrets.properties` if present (else fall back to the example), and expose
`BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_ANON_KEY` as `buildConfigField`
strings. Build must NOT fail when `secrets.properties` is absent (use example values
as defaults) so CI/fresh clones still compile.

- [ ] **Step 3: Verify build still succeeds without a real secrets file**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL` (no `secrets.properties` needed yet).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: add gitignored secrets.properties plumbing (Supabase URL/anon key)"
```

---

## Chunk 2: Data Layer (schema stub + verification)

### Task 3: Room entities + converters

**Files:**
- Create: `data/entity/Medication.kt`, `data/entity/Schedule.kt`,
  `data/entity/DoseLog.kt`, `data/converters/ListConverters.kt`,
  `util/Constants.kt`

- [ ] **Step 1: Write `ListConverters.kt`**

Two `@TypeConverter`s using kotlinx.serialization: `List<Int>` ⇄ JSON `String`
(for `daysOfWeek`) and `List<String>` ⇄ JSON `String` (for `notificationIds`).

- [ ] **Step 2: Write the three entities**

Translate 1:1 from the current iOS Core Data model (see spec §7). All `Date` fields →
`Long` (epoch millis, nullable where iOS marks optional). `UUID` → `String` primary
keys. Add `medicationId: String` FK on Schedule and DoseLog with an `@ForeignKey`
(onDelete = CASCADE) and an index on `medicationId`.

- Medication: `id` (PK), name, dosage, unit, colorHex?, photoData: ByteArray?,
  escriptData: ByteArray?, notes?, isActive, currentCount: Int, refillThreshold: Int,
  totalDosesPerDay: Int, sortOrder: Int, createdAt: Long?, updatedAt: Long?
- Schedule: `id` (PK), hour: Int, minute: Int, daysOfWeek: List<Int>,
  frequency: String, intervalDays: Int, isEnabled: Boolean,
  notificationIds: List<String>, updatedAt: Long?, medicationId (FK)
- DoseLog: `id` (PK), scheduledAt: Long?, loggedAt: Long?, status: String, notes?,
  updatedAt: Long?, medicationId (FK)

- [ ] **Step 3: Write `Constants.kt`**

`APPLICATION_ID`, `PRODUCT_MONTHLY = "com.robbrown.dosetrack.pro.monthly"`,
`PRODUCT_ANNUAL = "com.robbrown.dosetrack.pro.annual"`, `FREE_TIER_MED_LIMIT = 5`.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(data): add Room entities and type converters"
```

### Task 4: DAOs + database + Hilt module

**Files:**
- Create: `data/dao/MedicationDao.kt`, `data/dao/ScheduleDao.kt`,
  `data/dao/DoseLogDao.kt`, `data/DoseTrackDatabase.kt`, `di/DatabaseModule.kt`,
  `DoseTrackApplication.kt`
- Modify: `app/src/main/AndroidManifest.xml` (register application, name it)

- [ ] **Step 1: Write empty DAO interfaces**

Each `@Dao interface` with no methods yet (CRUD added in the data-layer feature
phase). Present so `@Database` can reference them.

- [ ] **Step 2: Write `DoseTrackDatabase.kt`**

`@Database(entities = [Medication, Schedule, DoseLog], version = 1)` with
`@TypeConverters(ListConverters::class)`; abstract accessors for the three DAOs.

- [ ] **Step 3: Write `DatabaseModule.kt` + `DoseTrackApplication.kt`**

Hilt `@Module @InstallIn(SingletonComponent)` providing a singleton
`DoseTrackDatabase` (`Room.databaseBuilder(... "dosetrack.db")`) and the three DAOs.
`DoseTrackApplication` annotated `@HiltAndroidApp`; register it as `android:name` in
the manifest.

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL` (KSP generates Room + Hilt code).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(data): add DAOs, Room database, and Hilt DI module"
```

### Task 5: Instrumented test — DB opens and schema is valid

**Files:**
- Create: `app/src/androidTest/java/com/robbrown/dosetrack/DoseTrackDatabaseTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@RunWith(AndroidJUnit4::class)
class DoseTrackDatabaseTest {
    @Test fun database_opens_and_exposes_daos() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(ctx, DoseTrackDatabase::class.java).build()
        assertNotNull(db.medicationDao())
        assertNotNull(db.scheduleDao())
        assertNotNull(db.doseLogDao())
        db.openHelper.writableDatabase   // forces schema creation
        db.close()
    }
}
```

- [ ] **Step 2: Run test (needs a running emulator or connected device)**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS. If it fails because the entity/converter graph is invalid, Room throws
at build/open time — fix the entity that Room names.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "test(data): verify Room database opens with valid schema"
```

---

## Chunk 3: UI Shell + Brief + Push

### Task 6: Theme + four-tab nav shell + placeholder screens

**Files:**
- Create: `ui/theme/{Color,Theme,Type}.kt` (Studio may have generated these — reuse),
  `ui/navigation/Destinations.kt`, `ui/navigation/DoseTrackNavHost.kt`,
  `ui/today/TodayScreen.kt`, `ui/medications/MedicationsScreen.kt`,
  `ui/history/HistoryScreen.kt`, `ui/settings/SettingsScreen.kt`,
  `ui/onboarding/OnboardingScreen.kt`, `ui/paywall/PaywallScreen.kt`,
  `notifications/NotificationScheduler.kt` (stub), `billing/SubscriptionManager.kt` (stub)
- Modify: `MainActivity.kt`

- [ ] **Step 1: Placeholder screens**

Each screen is a Composable showing its title centered (e.g. `Text("Today")`). No
logic. Onboarding and Paywall exist but are not wired into nav yet (reached in later
phases).

- [ ] **Step 2: Nav shell**

`Destinations.kt` = enum/sealed of Today/Medications/History/Settings with label +
icon. `DoseTrackNavHost.kt` = `Scaffold` with a `NavigationBar` (bottom tabs) hosting
a `NavHost` over the four destinations. Matches the iOS `TabView` (house/pill/calendar/
gear icons — use Material equivalents).

- [ ] **Step 3: Stubs for scheduler + subscription manager**

Empty classes with a doc comment `// Implemented in the notifications / billing phase`
so the package structure matches the spec and later phases have a home. No logic.

- [ ] **Step 4: Wire `MainActivity` to show the nav shell in the app theme**

- [ ] **Step 5: Build + launch on emulator**

Run: `./gradlew :app:assembleDebug` then install/run on the emulator (Studio Run, or
`./gradlew :app:installDebug` + launch).
Expected: app opens showing a four-tab bottom bar; tapping tabs switches the centered
placeholder titles.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(ui): add Material3 theme, bottom-nav shell, placeholder screens"
```

### Task 7: Android CLAUDE.md brief

**Files:**
- Create: `CLAUDE.md`

- [ ] **Step 1: Write the Android brief**

Translate the iOS `CLAUDE.md` to Android per spec §9: tech-stack mapping table, the
three-entity schema, subscription tiers/prices (product IDs from `Constants.kt`), the
notification-reliability rules restated for AlarmManager ("always exact alarms
rescheduled on `BOOT_COMPLETED`; never `setInexactRepeating` for medication
reminders"), the critical product rules (5-med free limit, local-first, export always
free, refill countdown always free, soft-delete first, disclaimer text), and the
phased build order re-sequenced for Android with Wear OS + Glance as their own late
phases. Note the source of truth is the *current* iOS app, and that auth/sync/caregiver/
push/meal-times exist on iOS and are future Android phases.

- [ ] **Step 2: Commit + push**

```bash
git add CLAUDE.md
git commit -m "docs: add Android project brief (CLAUDE.md)"
git push origin main
```

---

## Verification (whole plan)

- `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL`.
- `./gradlew :app:connectedDebugAndroidTest` → the Room test passes.
- App launches on an emulator showing the four-tab shell; tabs switch screens.
- `CLAUDE.md` present and committed; `main` pushed to GitHub.

## Explicitly NOT in this plan (future specs/plans)

Data-layer CRUD + repositories; notification engine; Play Billing; screen behaviour;
add/edit form; onboarding flow; paywall logic; CSV export; PDF reports; Supabase auth +
sync; caregiver sharing; push notifications; Wear OS; Glance widgets; Play Store assets.
