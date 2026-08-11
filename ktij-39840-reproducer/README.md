# KTIJ-39840 reproducer

Standalone Gradle repro for **[KTIJ-39840](https://youtrack.jetbrains.com/issue/KTIJ-39840)**:
a published Kotlin Multiplatform library (JVM + iOS) is consumed by a plain
`com.android.library` module and by a KMP control module. In IntelliJ IDEA /
Android Studio the dependency appears under External Libraries, but **JVM class
and source roots are missing from indexable roots** for the Android module while
the KMP control module indexes correctly. Gradle compile of the library is fine
where the Android SDK is available; Android Studio may also behave correctly
depending on version.

## Layout

```
ktij-39840-reproducer/
  producer/                 # KMP lib: jvm + iosArm64 + iosSimulatorArm64
  consumer/
    android-lib/            # com.android.library (AGP 9.1.1) — bug surface
    kmp-control/            # KMP jvm-only — healthy control path
  logs/                     # captured CLI output + .module dumps
  README.md
  REPORT.md                 # verified vs blocked on this runner
```

There is **no** `project()` dependency from consumer → producer. Both consumer
modules declare:

```kotlin
implementation("ru.quickresto:kkm-contract:3.0.0")
```

and resolve it only via `mavenLocal()` (then `google()` / `mavenCentral()`).

## Versions (pinned)

| Piece | Version |
|-------|---------|
| Gradle wrapper | **9.6.1** (exact `9.6` distribution URL 404s; see REPORT) |
| Kotlin | **2.3.21** |
| AGP | **9.1.1** (built-in Kotlin; do **not** apply `org.jetbrains.kotlin.android`) |
| Published GAV | `ru.quickresto:kkm-contract:3.0.0` |
| JVM artifact id | `ru.quickresto:kkm-contract-jvm:3.0.0` |
| Producer jvmTarget | 1.8 |
| Android compileSdk / minSdk | 35 / 24 |

## Prerequisites

- JDK 17+ (`JAVA_HOME` must point at a real JDK; this tree does not assume `java` on `PATH`)
- For Android **compile** only: Android SDK + `ANDROID_HOME` or `consumer/local.properties` with `sdk.dir=...`
- Network on first run (plugin/dependency download)

Example on this runner:

```bash
export JAVA_HOME=/usr/lib/jvm/amazon-corretto-17.0.9.8.1-linux-x64
```

## CLI setup (publish + control path)

```bash
export JAVA_HOME=...   # JDK 17+

# 1) Publish producer to mavenLocal (JVM + iOS klibs; Linux uses K/N cross-compilation)
cd producer
./gradlew publishToMavenLocal --no-daemon

# 2) KMP control module — should resolve mavenLocal and compile
cd ../consumer
./gradlew :kmp-control:compileKotlinJvm --no-daemon

# Optional: confirm variant selection
./gradlew :kmp-control:dependencyInsight \
  --dependency kkm-contract --configuration jvmCompileClasspath --no-daemon

# 3) Android library — dependency graph can resolve without SDK:
./gradlew :android-lib:dependencies --configuration debugCompileClasspath --no-daemon
# Compile needs Android SDK:
./gradlew :android-lib:compileDebugKotlin --no-daemon
```

Expected resolution for both modules:

```
ru.quickresto:kkm-contract:3.0.0
\--- ru.quickresto:kkm-contract-jvm:3.0.0
```

Optional experiment (reporter tried both coordinates): change the dependency to
`ru.quickresto:kkm-contract-jvm:3.0.0` and re-sync. The bug is about how the IDE
indexes the multiplatform root coordinate / JVM variant for a plain Android
library module.

## What was verified on CI/Linux (no Android SDK)

See [REPORT.md](./REPORT.md) for command transcripts and full variant lists.

| Check | Result |
|-------|--------|
| `producer` `publishToMavenLocal` | **OK** — root + `-jvm` + `iosarm64` + `iossimulatorarm64` |
| Root `.module` variants (metadata, jvm*, ios*) | **OK** — recorded in REPORT / `logs/` |
| `consumer` `:kmp-control:compileKotlinJvm` | **OK** against mavenLocal |
| `consumer` `:android-lib:dependencies` (debugCompileClasspath) | **OK** — selects `-jvm` |
| `consumer` `:android-lib:compileDebugKotlin` | **BLOCKED** — no `ANDROID_HOME` / SDK on runner |

## IDE repro steps (IU-262.x)

Affected build called out in the ticket workflow: **IU-262.8665.337**
(IntelliJ IDEA 2026.2 EAP / matching platform). Re-check on the same baseline
or a nearby IU-262 build.

1. Publish the producer once (CLI section above) so
   `~/.m2/repository/ru/quickresto/kkm-contract/**` exists on the machine that
   runs the IDE.
2. Open **`ktij-39840-reproducer/consumer`** as a Gradle project (not the monorepo root).
3. Trust the project and wait for Gradle sync to finish.
4. Confirm both modules depend on `ru.quickresto:kkm-contract:3.0.0` via mavenLocal
   (Project tool window → Dependencies, or Gradle tool window).
5. **Compare indexing for the same library in the two modules:**
   - Open **Project Structure → Modules → `android-lib` → Dependencies**
     (or External Libraries) and inspect `kkm-contract` / `kkm-contract-jvm`.
   - Check whether **classes** and **sources** roots are present / indexable.
   - Repeat for **`kmp-control`**.
6. **Expected bug shape (android-lib):**
   - Library is listed under Dependencies / External Libraries.
   - JVM class roots and/or source roots are **missing** from indexable roots
     (navigation to `KkmContract` / `KkmContractFactory` from
     `AndroidLibApi.kt` fails or is incomplete).
   - Gradle compile of `android-lib` still succeeds when an Android SDK is configured.
7. **Control (kmp-control):**
   - Same GAV indexes correctly; navigation from `KmpControlApi.kt` to producer
     API works.
8. Optional: File → Invalidate Caches / re-import Gradle; bug should still show
   on `android-lib` only if it is the KTIJ-39840 regression.

### What “good” vs “bad” looks like

| Observation | `android-lib` (bug) | `kmp-control` (control) |
|-------------|---------------------|-------------------------|
| Dependency listed | yes | yes |
| Resolved artifact | `kkm-contract-jvm` jar | `kkm-contract-jvm` jar |
| Indexable class roots for JVM jar | **missing / incomplete** | present |
| Go-to-declaration on `KkmContractFactory` | broken / red code in editor | works |
| CLI compile with SDK | OK | OK (`compileKotlinJvm`) |

## Notes

- Producer sets `kotlin.native.enableKlibsCrossCompilation=true` so iOS klibs can
  be published from Linux. Full native **link** remains Mac-only if needed later.
- AGP 9.x rejects applying `org.jetbrains.kotlin.android` when built-in Kotlin is
  on; `android-lib` uses only `com.android.library` + the built-in `kotlin { }`
  extension.
- Do not open this repro as part of the Kotlin monorepo composite unless you
  intentionally want monorepo classpaths; the bug is about a **published** Maven
  KMP artifact.

## Related files

- [REPORT.md](./REPORT.md) — host environment, exact commands, published coordinates, variant dump, blocked Android compile output
- [logs/](./logs/) — raw Gradle logs and copied `.module` files from mavenLocal
