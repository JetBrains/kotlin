# KTIJ-39840 reproducer — verification report

Date: **2026-08-11** (UTC)  
Host: Linux x86_64 (`6.17.0-1020-azure`), no Android SDK  
`JAVA_HOME=/usr/lib/jvm/amazon-corretto-17.0.9.8.1-linux-x64` (Corretto 17.0.9)  
`ANDROID_HOME=unset`

This report records **real** CLI outcomes for the standalone tree under
`ktij-39840-reproducer/`. It does **not** claim an IDE repro on this headless
runner.

---

## Summary

| Path | Status | Notes |
|------|--------|-------|
| Producer `publishToMavenLocal` | **VERIFIED** | Full root + JVM + iosArm64 + iosSimulatorArm64 |
| mavenLocal GAV / `.module` variants | **VERIFIED** | Dumped from `~/.m2` (see below + `logs/`) |
| Consumer `:kmp-control:compileKotlinJvm` | **VERIFIED** | Resolves `kkm-contract` → `kkm-contract-jvm` from mavenLocal |
| Consumer `:android-lib:dependencies` (debugCompileClasspath) | **VERIFIED** | Same JVM variant selection without SDK |
| Consumer `:android-lib:compileDebugKotlin` | **BLOCKED** | SDK location not found (expected on this runner) |
| IDE indexing bug (IU-262.8665.337) | **NOT RUN HERE** | Steps in README; requires desktop IDE |

---

## Environment / pins

| Item | Value |
|------|--------|
| Gradle wrapper | **9.6.1** (`gradle-9.6.1-bin.zip`, sha256 `9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14`) |
| Gradle 9.6 exact | **Unavailable** — `gradle-9.6-bin.zip` returned 404; plan alt used |
| Kotlin | 2.3.21 |
| AGP | 9.1.1 |
| Producer group/artifact/version | `ru.quickresto` / `kkm-contract` / `3.0.0` |
| Producer targets | `jvm` (jvmTarget 1.8), `iosArm64`, `iosSimulatorArm64` |
| Cross-compile flag | `kotlin.native.enableKlibsCrossCompilation=true` |
| Consumer modules | `:android-lib`, `:kmp-control` |
| Consumer dependency | `implementation("ru.quickresto:kkm-contract:3.0.0")` via `mavenLocal()` only |

---

## Commands run

All Gradle invocations used:

```bash
export JAVA_HOME=/usr/lib/jvm/amazon-corretto-17.0.9.8.1-linux-x64
```

### 1. Producer publish — VERIFIED

```bash
cd ktij-39840-reproducer/producer
./gradlew publishToMavenLocal --no-daemon
```

**Result:** `BUILD SUCCESSFUL in 47s` (cold; includes K/N distribution download + iOS klib cross-compile).

Published tasks included:

- `publishKotlinMultiplatformPublicationToMavenLocal`
- `publishJvmPublicationToMavenLocal`
- `publishIosArm64PublicationToMavenLocal`
- `publishIosSimulatorArm64PublicationToMavenLocal`

Log copy: `logs/producer-publishToMavenLocal.log` (re-run / UP-TO-DATE capture).

### 2. Inspect mavenLocal — VERIFIED

```bash
find ~/.m2/repository/ru/quickresto -type f | sort
# + parse each *.module for variant names / available-at
```

#### Published coordinates (artifact ids)

| Maven module (directory) | Role | Key files (version 3.0.0) |
|--------------------------|------|---------------------------|
| `ru.quickresto:kkm-contract` | Root KMP component | `kkm-contract-3.0.0.jar` (metadata), `.module`, `.pom`, `-sources.jar`, `-kotlin-tooling-metadata.json` |
| `ru.quickresto:kkm-contract-jvm` | **JVM variant artifact id** | `kkm-contract-jvm-3.0.0.jar`, `.module`, `.pom`, `-sources.jar` |
| `ru.quickresto:kkm-contract-iosarm64` | iosArm64 | `.klib`, `-metadata.jar`, `.module`, `.pom`, `-sources.jar` |
| `ru.quickresto:kkm-contract-iossimulatorarm64` | iosSimulatorArm64 | `.klib`, `-metadata.jar`, `.module`, `.pom`, `-sources.jar` |

On-disk tree (files only, checksums omitted):

```
~/.m2/repository/ru/quickresto/kkm-contract/3.0.0/
  kkm-contract-3.0.0.jar
  kkm-contract-3.0.0-kotlin-tooling-metadata.json
  kkm-contract-3.0.0.module
  kkm-contract-3.0.0.pom
  kkm-contract-3.0.0-sources.jar
~/.m2/repository/ru/quickresto/kkm-contract-jvm/3.0.0/
  kkm-contract-jvm-3.0.0.jar
  kkm-contract-jvm-3.0.0.module
  kkm-contract-jvm-3.0.0.pom
  kkm-contract-jvm-3.0.0-sources.jar
~/.m2/repository/ru/quickresto/kkm-contract-iosarm64/3.0.0/
  kkm-contract-iosarm64-3.0.0.klib
  kkm-contract-iosarm64-3.0.0-metadata.jar
  kkm-contract-iosarm64-3.0.0.module
  kkm-contract-iosarm64-3.0.0.pom
  kkm-contract-iosarm64-3.0.0-sources.jar
~/.m2/repository/ru/quickresto/kkm-contract-iossimulatorarm64/3.0.0/
  kkm-contract-iossimulatorarm64-3.0.0.klib
  kkm-contract-iossimulatorarm64-3.0.0-metadata.jar
  kkm-contract-iossimulatorarm64-3.0.0.module
  kkm-contract-iossimulatorarm64-3.0.0.pom
  kkm-contract-iossimulatorarm64-3.0.0-sources.jar
```

Copies of root + JVM module metadata:  
`logs/kkm-contract-3.0.0.module`, `logs/kkm-contract-jvm-3.0.0.module`,  
full variant text dump: `logs/mavenLocal-variants.txt`.

#### Root module `kkm-contract-3.0.0.module` variants

Component: `ru.quickresto:kkm-contract:3.0.0`  
Created by Gradle **9.6.1**.

| Variant name | platform.type | usage | Payload |
|--------------|---------------|-------|---------|
| `metadataApiElements` | common | kotlin-metadata | file `kkm-contract-metadata-3.0.0.jar` (inside root jar packaging) |
| `metadataSourcesElements` | common | kotlin-runtime | sources |
| `jvmApiElements-published` | jvm | java-api | **available-at** `ru.quickresto:kkm-contract-jvm:3.0.0` |
| `jvmRuntimeElements-published` | jvm | java-runtime | **available-at** `ru.quickresto:kkm-contract-jvm:3.0.0` |
| `jvmSourcesElements-published` | jvm | java-runtime (sources) | **available-at** `ru.quickresto:kkm-contract-jvm:3.0.0` |
| `iosArm64ApiElements-published` | native / ios_arm64 | kotlin-api | **available-at** `…:kkm-contract-iosarm64:3.0.0` |
| `iosArm64SourcesElements-published` | native / ios_arm64 | kotlin-runtime | **available-at** iosarm64 |
| `iosArm64MetadataElements-published` | native / ios_arm64 | kotlin-metadata | **available-at** iosarm64 |
| `iosSimulatorArm64ApiElements-published` | native / ios_simulator_arm64 | kotlin-api | **available-at** iossimulatorarm64 |
| `iosSimulatorArm64SourcesElements-published` | native / ios_simulator_arm64 | kotlin-runtime | **available-at** iossimulatorarm64 |
| `iosSimulatorArm64MetadataElements-published` | native / ios_simulator_arm64 | kotlin-metadata | **available-at** iossimulatorarm64 |

`available-at` example for JVM (from real `.module`):

```json
"available-at": {
  "url": "../../kkm-contract-jvm/3.0.0/kkm-contract-jvm-3.0.0.module",
  "group": "ru.quickresto",
  "module": "kkm-contract-jvm",
  "version": "3.0.0"
}
```

#### JVM module `kkm-contract-jvm-3.0.0.module` variants

| Variant | Files |
|---------|--------|
| `jvmApiElements-published` | `kkm-contract-jvm-3.0.0.jar` |
| `jvmRuntimeElements-published` | `kkm-contract-jvm-3.0.0.jar` |
| `jvmSourcesElements-published` | `kkm-contract-jvm-3.0.0-sources.jar` |

Attributes include `org.jetbrains.kotlin.platform.type=jvm` and
`org.gradle.jvm.environment=standard-jvm`.

#### iOS platform modules (files present on Linux)

| Module | API file | Metadata |
|--------|----------|----------|
| `kkm-contract-iosarm64` | `kkm-contract-iosArm64Main-3.0.0.klib` (as listed in `.module`) | `-metadata.jar` |
| `kkm-contract-iossimulatorarm64` | `kkm-contract-iosSimulatorArm64Main-3.0.0.klib` | `-metadata.jar` |

Cross-compilation on Linux **succeeded** for klib publish; no iOS variants were dropped.

### 3. Consumer KMP control compile — VERIFIED

```bash
cd ktij-39840-reproducer/consumer
./gradlew :kmp-control:compileKotlinJvm --no-daemon
```

**Result:** `BUILD SUCCESSFUL in 12s`.

```bash
./gradlew :kmp-control:dependencyInsight \
  --dependency kkm-contract --configuration jvmCompileClasspath --no-daemon
```

**Result:** `BUILD SUCCESSFUL`. Selected variant:

```
ru.quickresto:kkm-contract:3.0.0
  Variant jvmApiElements-published:
    org.gradle.jvm.environment         standard-jvm
    org.gradle.usage                   java-api
    org.jetbrains.kotlin.platform.type jvm

ru.quickresto:kkm-contract:3.0.0
\--- jvmCompileClasspath

ru.quickresto:kkm-contract-jvm:3.0.0
  Variant jvmApiElements-published
  ...
\--- ru.quickresto:kkm-contract:3.0.0
     \--- jvmCompileClasspath
```

Log: `logs/kmp-control-dependencyInsight.log`.

### 4. Consumer Android path

#### 4a. Dependency resolution — VERIFIED (no SDK required)

```bash
./gradlew :android-lib:dependencies --configuration debugCompileClasspath --no-daemon
```

**Result:** `BUILD SUCCESSFUL in 7s`.

```
debugCompileClasspath - Compile classpath for '/debug'.
+--- org.jetbrains.kotlin:kotlin-stdlib:2.3.21
|    \--- org.jetbrains:annotations:13.0
\--- ru.quickresto:kkm-contract:3.0.0
     \--- ru.quickresto:kkm-contract-jvm:3.0.0
          \--- org.jetbrains.kotlin:kotlin-stdlib:2.3.21 (*)
```

Log: `logs/android-lib-dependencies.log`.

#### 4b. Compile — BLOCKED (no Android SDK)

```bash
./gradlew :android-lib:compileDebugKotlin --no-daemon
```

**Result:** `BUILD FAILED` (configure/task dependency determination).

Exact failure (verbatim):

```
FAILURE: Build failed with an exception.

* What went wrong:
Could not determine the dependencies of task ':android-lib:compileDebugKotlin'.
> SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/workspace/kotlin/ktij-39840-reproducer/consumer/local.properties'.
```

Log: `logs/android-compileDebugKotlin.log`.

**No green Android compile is claimed on this runner.** With a real SDK, the same
module structure + mavenLocal graph is expected to compile; the KTIJ-39840
symptom is IDE indexing, not Gradle resolution.

---

## IDE-only (not executed on this host)

Target IDE build from the plan: **IU-262.8665.337**.

Follow README § “IDE repro steps”:

1. Publish producer to the IDE machine’s mavenLocal.
2. Open `consumer/` in IU-262.x, sync Gradle.
3. Compare Project Structure / External Libraries / indexable roots for
   `android-lib` vs `kmp-control` on `ru.quickresto:kkm-contract` /
   `kkm-contract-jvm`.
4. Bug shape: dependency listed, JVM class/source roots missing for Android
   module only; Gradle compile OK when SDK present.

---

## Acceptance checklist (Step 2)

- [x] Producer `publishToMavenLocal` with `JAVA_HOME` set
- [x] Published GAV + `.module` variants listed from **actual** files; `-jvm` artifact id called out
- [x] Consumer `:kmp-control:compileKotlinJvm` succeeds against mavenLocal
- [x] Consumer Android compile attempted once; blocker output captured; no fake success
- [x] README: verified vs blocked + IDE steps for IU-262.8665.337
- [x] REPORT + `logs/` non-empty with verified/blocked sections

---

## Discoveries (runner-specific)

- Exact Gradle **9.6** distribution is unpublished (404); wrappers use **9.6.1**.
- AGP **9.1.1** must use built-in Kotlin only (`org.jetbrains.kotlin.android` rejected).
- `:android-lib:dependencies` works **without** SDK; only compile/AGP tasks that need the platform SDK fail.
- iOS klib publish on Linux works with `kotlin.native.enableKlibsCrossCompilation=true` (Kotlin 2.3.21).
- Cold producer publish ~47s; warm publish ~9s; kmp compile ~12s; android deps ~7s; android compile fail ~6s.
