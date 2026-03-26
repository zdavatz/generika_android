# Generika Android

## Build

```bash
./gradlew compileDebugSources   # compile
./gradlew assembleDebug         # build debug APK
./gradlew assembleRelease       # build release APK
./gradlew bundleRelease         # build release AAB
./gradlew test                  # run tests
./apkup_bundle                  # clean + build + upload AAB to Google Play (production)
./apkup_bundle beta             # upload to beta track
```

## Architecture

- **Language**: Java (no Kotlin)
- **Min SDK**: 23, **Target SDK**: 31
- **Local data**: Realm (products, receipts) + SQLite (pharmaceutical DBs)
- **Activities** extend `BaseActivity` which handles locale management

## Key Databases

| Database | Manager | Source |
|----------|---------|--------|
| `amiko_db_full_idx_pinfo_de.db` | `AmikoDBManager` | `http://pillbox.oddb.org/amiko_db_full_idx_pinfo_de.db.zip` (downloaded as zip, extracted) |
| `interactions.db` | `InteractionsDBManager` | `http://pillbox.oddb.org/interactions.db` |

Both are downloaded to `{dataDir}/databases/` and opened read-only.

### Download Flow

On first launch, `MainActivity.checkAndDownloadDatabase()` checks if each DB exists:
- If either is missing, a progress dialog chains both downloads sequentially (pharmaceutical first, then interactions)
- If both exist, update checks run silently in background
- The "Update All Databases" button in Settings also downloads both sequentially
- After download, Settings shows stats (row counts, file sizes) for each database

## Key Directories

- `app/src/main/java/org/oddb/generika/` — Activities
- `app/src/main/java/org/oddb/generika/data/` — DB managers (`AmikoDBManager`, `InteractionsDBManager`, `DataManager`)
- `app/src/main/java/org/oddb/generika/model/` — Data models (`Product`, `Receipt`, `AmikoDBRow`, etc.)
- `app/src/main/java/org/oddb/generika/util/` — Constants, locale helpers
- `app/src/main/res/layout/` — XML layouts
- `app/src/main/res/xml/user_settings.xml` — Settings preferences
- `app/src/main/assets/` — JSON mapping files (`bag-to-insurance-gln.json`, `bag-to-insurance-name.json`)

## Interactions Lookup (InteractionsDBManager)

3-tier strategy matching oddb.org logic:
1. **EPha curated** — `epha_interactions` table, direct ATC-to-ATC lookup
2. **Substance-level** — `interactions` table via `drugs.active_substances`
3. **Class-level** — `class_keywords` + `drugs.interactions_text` FachInfo search

EAN → ATC resolution goes through `AmikoDBManager.findWithGtin()`.

**Important:** The amikodb `atc` field format is `"N06AB06;Sertralin"` (ATC code + semicolon + substance name). Always parse the code before the semicolon when extracting ATC codes.

For unmatched pairs (no EPha hit), both substance-level and class-level tiers run in both directions, matching the oddb.org Ruby behavior.

**Gegenrichtung severity hints:** For EPha results and class-level results, the code checks FachInfo text severity (`classSeverityForDirection`) in both directions. If one direction has higher severity than the other, a yellow hint is shown (e.g. "Swissmedic FI: Gegenrichtung hat höhere Einstufung (Kontraindiziert vs Keine Einstufung)"). This matches the oddb.org Ruby `build_epha_result` and `find_class_interactions` logic.

## Kostengutsprache (KVV 71)

`KostengutspracheActivity` — IBD Gastroenterology cost approval form, accessible from ReceiptActivity toolbar. Features:
- Patient fields (name, birth date, gender, address, AHV number)
- Insurance fields (insurer name, card number) with insurance card scanner integration
- Diagnosis (M. Crohn / Colitis ulcerosa)
- Medication list (editable text, pre-filled from receipt)
- Physician fields (name, ZSR, hospital, department) with prescription scanner integration
- PDF generation (A4) and share via Android share sheet
- Data persisted to Realm via new model fields: `Patient.ahvNumber`, `Patient.insurerName`, `Patient.healthCardNumber`, `Operator.zsrNumber`, `Receipt.diagnosis`

## Insurance Card Scanner

`InsuranceCardScannerActivity` — Camera-based OCR scanner for Swiss health insurance cards (Versichertenkarte). Uses ML Kit Text Recognition + CameraX.
- Extracts: family name, given name, card number (20 digits), BAG number (5 digits), AHV number (NNN.NNNN.NNNN.NN), birth date, gender (M/F)
- Maps BAG number to insurer GLN and name via JSON assets
- Returns results via `RESULT_OK` Intent extras

## Prescription Scanner

`PrescriptionScannerActivity` — Two-stage prescription scanner using ML Kit Barcode Scanning + Text Recognition + CameraX.
- Stage 1: Live QR detection for CHMED16A / eprescription.hin.ch payloads
- Stage 2: Photo capture + full-page OCR
- Extracts: medications (dosage form keywords), dosages, AHV, ZSR, physician name/title, hospital, department, patient address, prescription date
- Returns results via `RESULT_OK` Intent extras

## Realm Schema

Current version: **3** (defined in `GenerikaApplication.SCHEME_VERSION`). Migrations in `Migration.java`.

## Conventions

- Commit messages: short imperative sentence describing the change
- No Kotlin — the project is pure Java
- Activities registered in `AndroidManifest.xml`
- Strings in `res/values/strings.xml` (+ `values-de/`, `values-fr/`)
