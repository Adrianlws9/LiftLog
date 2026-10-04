# LiftLog

A native Android workout tracker built from scratch for personal daily use — not a tutorial project. LiftLog opens to today's scheduled workout, logs sets/reps/weight quickly enough to use mid-set at the gym, and has been refined through months of real training rather than built once and abandoned.

## Why I built this

Every workout app I tried was either bloated with subscriptions and features I didn't need, or too rigid to match my actual routine. I wanted something that opens straight to today's plan, logs a set in two taps, and remembers exactly what I did last time — nothing else. So I built it myself, and kept using it, which meant kept fixing it.

## Features

**Core workout logging**
- Auto-detects the day and shows that day's scheduled workout
- Logs sets with weight, reps, and unit (kg / lb / **bar-only** as a real distinct state, not just "0 kg")
- Smart autofill — new sets default to your last entry, or your last session's numbers
- Live workout timer and a break timer (30s / 1min / 1:30) with a circular countdown, backed by a foreground service, system notification, and `AlarmManager` for reliable delivery
- Per-workout notes, scoped to workout type, carried forward automatically as a starting point each time
- Logging a custom/ad-hoc workout outside the normal plan

**Progress & motivation**
- "Last time" comparison inline on every set, showing both weight and rep deltas
- Personal record badges when a set beats your all-time best
- Per-exercise progress charts and a body-weight trend chart, drawn with a custom lightweight `Canvas`-based chart view (no external charting library)
- Monthly summary: workouts, time trained, total volume lifted, most-trained exercise, and comparison to the previous month

**History & plan management**
- Full workout history: view, edit (including fixing a set logged under the wrong exercise), delete, or copy as a plain-text summary
- Fully editable weekly workout plan — add, remove, rename, and reorder exercises, or move an exercise to a different day
- JSON export/import for backup and safe migration between devices or reinstalls

**Polish**
- Custom app icon and branded splash screen
- Consistent dark theme (deep navy / burnt orange / ivory) with custom typography
- Navigation drawer keeping the home screen to a single primary action

## Tech stack

- **Kotlin**, XML Views (no Jetpack Compose)
- **Room** for local persistence — 5 schema versions, all upgraded through proper migrations with zero data loss across real-world use
- **MVVM**: Activities → ViewModel (`viewModelScope`, `LiveData`) → Repository → Room
- **Foreground Service + BroadcastReceiver + AlarmManager** for a reliable break timer independent of app process state
- **AndroidX Splash Screen**, `DrawerLayout` + `NavigationView`
- A hand-rolled `Canvas`-based line chart `View` for progress visualization
- Min SDK 26 (Android 8.0), built with AGP's built-in Kotlin support + KSP

## Architecture

```
XML UI (Activities)
    ↓
ViewModel (viewModelScope, LiveData)
    ↓
Repository  (single source of truth; translates between UI models and DB entities)
    ↓
Room Database (Entities + DAOs)
```

Package layout:
- `model/` — UI-facing data classes and formatting helpers
- `data/` — Room entities, DAOs, database class, repository
- `ui/` — ViewModels
- `adapter/` — RecyclerView adapters
- `service/` — break timer foreground service + receiver
- `widget/` — custom chart view

## Database notes

The database has gone through 5 real schema versions on a live, in-use install — every migration was written to be non-destructive, and the upgrade path has been explicitly tested (including a caught-and-fixed bug in one migration's raw SQL, and a separate caught-and-fixed bug where fresh installs weren't seeding default data the same way upgrades were). Full export/import was built specifically to guarantee data survives a signing-key change or reinstall, and was verified end-to-end: export → wipe → reinstall-equivalent → import → confirmed byte-for-byte correct history.

## Setup

1. Clone the repo
2. Open in Android Studio (current stable channel recommended)
3. Sync Gradle
4. Run on a device or emulator running Android 8.0+

No API keys or backend setup required — everything is local.

## What I'd do differently / known limitations

- Break-timer vibration can be suppressed in the background on some OEM Android skins (observed on Transsion/HiOS devices) due to aggressive battery management outside the app's control — mitigated with `AlarmManager` + a manifest-registered receiver, but not 100% guaranteed on every device without the user whitelisting the app.
- No cloud sync yet — export/import is manual, by design, to avoid needing a backend for a personal-use app.
- Charts are intentionally simple (line charts, no zoom/pan) — built this way deliberately rather than pulling in a heavy charting library for a personal project.

## License

Personal project, built for my own use. Feel free to look through the code or fork it for your own training log.