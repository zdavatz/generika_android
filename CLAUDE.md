# Generika Android

## Build

```bash
./gradlew compileDebugSources   # compile
./gradlew assembleDebug         # build debug APK
./gradlew assembleRelease       # build release APK
./gradlew test                  # run tests
```

## Architecture

- **Language**: Java (no Kotlin)
- **Min SDK**: 23, **Target SDK**: 31
- **Local data**: Realm (products, receipts) + SQLite (pharmaceutical DBs)
- **Activities** extend `BaseActivity` which handles locale management

## Key Databases

| Database | Manager | Source |
|----------|---------|--------|
| `amiko_db_full_idx_pinfo_de.db` | `AmikoDBManager` | `http://pillbox.oddb.org/amiko_db_full_idx_pinfo_de.db` |
| `interactions.db` | `InteractionsDBManager` | `http://pillbox.oddb.org/interactions.db` |

Both are downloaded to `{dataDir}/databases/` and opened read-only.

## Key Directories

- `app/src/main/java/org/oddb/generika/` — Activities
- `app/src/main/java/org/oddb/generika/data/` — DB managers (`AmikoDBManager`, `InteractionsDBManager`, `DataManager`)
- `app/src/main/java/org/oddb/generika/model/` — Data models (`Product`, `Receipt`, `AmikoDBRow`, etc.)
- `app/src/main/java/org/oddb/generika/util/` — Constants, locale helpers
- `app/src/main/res/layout/` — XML layouts
- `app/src/main/res/xml/user_settings.xml` — Settings preferences

## Interactions Lookup (InteractionsDBManager)

3-tier strategy matching oddb.org logic:
1. **EPha curated** — `epha_interactions` table, direct ATC-to-ATC lookup
2. **Substance-level** — `interactions` table via `drugs.active_substances`
3. **Class-level** — `class_keywords` + `drugs.interactions_text` FachInfo search

EAN → ATC resolution goes through `AmikoDBManager.findWithGtin()`.

## Conventions

- Commit messages: short imperative sentence describing the change
- No Kotlin — the project is pure Java
- Activities registered in `AndroidManifest.xml`
- Strings in `res/values/strings.xml` (+ `values-de/`, `values-fr/`)
