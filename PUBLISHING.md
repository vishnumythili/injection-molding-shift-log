# Publishing Shift Log on Google Play (free, public)

## 0. Before you start
- **Developer account:** one-time US$25 fee at play.google.com/console. Identity verification is required
  (and a D-U-N-S number if you register as an organisation).
- **Personal accounts created after Nov 2023** must run a **closed test with at least 12 testers opted-in
  for 14 continuous days** before applying for production access. Plan for ~2–3 weeks.
  (Play rules change; confirm the current numbers in Play Console → Dashboard.)
- Pick your final **package name** now (see README) — it can never change.

## 1. Build a signed release bundle
1. Android Studio → *Build → Generate Signed App Bundle / APK → Android App Bundle*.
2. Create a new keystore (this is your **upload key**). Back it up somewhere safe; never commit it.
3. Choose `release`, finish. You get `app/release/app-release.aab`.
4. Keep `versionCode` increasing for every upload (`app/build.gradle.kts`).
5. Enrol in **Play App Signing** when Play Console asks (default for new apps).

## 2. Create the app in Play Console
*All apps → Create app* → name **Shift Log**, language, **App**, **Free**, accept declarations.

## 3. Required forms (Policy → App content)
| Item | What to answer |
|---|---|
| Privacy policy | Host `PRIVACY_POLICY.md` as a web page (GitHub Pages or a Google Sites page works) and paste the URL |
| Ads | No ads |
| App access | All functionality available without login |
| Content rating | Complete the questionnaire (utility app → Everyone) |
| Target audience | 18+ (workplace tool) |
| Data safety | Collects **no data**, shares **no data** (everything stays on the device) |
| Government / financial / health / news app | No |

## 4. Store listing
Use `STORE_LISTING.md`. Required assets:
- App icon **512×512 PNG** (export from Android Studio: right-click `res` → New → Image Asset)
- Feature graphic **1024×500**
- At least **2 phone screenshots** (run the app, take real screenshots; 16:9 or 9:16, min 320 px)

## 5. Test → production
1. *Testing → Closed testing* → create track, upload the `.aab`, add tester emails (Google Group is easiest),
   share the opt-in link, keep ≥12 testers for 14 days.
2. *Dashboard → Apply for production* once eligible.
3. *Production → Create release* → upload the same (or newer) `.aab`, release notes, roll out.
4. Review typically takes from a few hours to a few days.

## 6. After launch
- Raise `versionCode` + `versionName` for each update.
- Watch *Android vitals* and reviews.
- Target SDK must be raised yearly to meet Play's requirement (change `targetSdk`/`compileSdk`, test).

---

## Alternative: build in the cloud with GitHub Actions (no Android Studio)
1. Create a new **private** GitHub repo and push this folder to it (branch `main`).
2. Repo → *Actions* runs **Build Android** on every push (or run it manually). Download the
   `shiftlog-builds` artifact: it holds a debug **APK** (install on your phone to test).
3. For a Play-ready signed **AAB**, create an upload keystore once:
   `keytool -genkey -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
   then add these repo secrets (*Settings → Secrets → Actions*):
   `KEYSTORE_BASE64` (output of `base64 -w0 upload.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
   Re-run the workflow; the artifact now contains a signed `app-release.aab` to upload in Play Console.
4. Keep `upload.jks` backed up and out of git (`.gitignore` already excludes it).
