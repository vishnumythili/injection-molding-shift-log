# Shift Log — shift-wise production & rejection tracker (Android)

Offline-first app for injection moulding shops. Log each shift's output, rejections by reason,
downtime and notes; see daily/weekly/monthly reports; export to CSV (opens in Excel).

**Stack:** Kotlin · Jetpack Compose (Material 3) · Room (local SQLite) · no network, no ads, no permissions.

## Build & run
1. Install **Android Studio** (latest stable). Open this folder (`ShiftLog`).
2. Let Gradle sync (it will offer to update plugin versions; accept).
3. Run on a phone (USB debugging) or an emulator.

## Screens
- **Home** – today's totals, entries grouped by day, big "New entry" button.
- **Entry form** – date, shift (A/B/C, auto-picked by time), machine, product, target, produced,
  +/- counters for each rejection reason, live Good / Rejected / Rej % card, downtime, operator, remarks.
  Machine / product / operator are remembered as one-tap chips.
- **Reports** – Today / 7 days / 30 days / All: KPIs, rejections by reason, by machine, downtime by reason, CSV export.

## Customise
- Rejection reasons and downtime reasons: `Data.kt` → `object Rej`.
- Shift timings: `Util.kt` → `currentShift()` and `shiftHours`.
- Rejection colour thresholds: `Util.kt` → `rejColor()`.
- Colours: `Theme.kt`. App name: `res/values/strings.xml`.
- **Package name:** change `com.shiftlog.app` (namespace + applicationId in `app/build.gradle.kts`,
  and the `package` lines in the Kotlin files) to your own, e.g. `com.yourname.shiftlog`.
  It is permanent once published.

## Data model
`production = total pieces made`, `rejected = sum of reasons`, `good = produced − rejected`,
`rej % = rejected / produced`.

## Publish
See `PUBLISHING.md`.
